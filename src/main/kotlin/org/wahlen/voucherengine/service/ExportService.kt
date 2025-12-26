package org.wahlen.voucherengine.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.request.ExportCreateRequest
import org.wahlen.voucherengine.api.dto.request.ExportParameters
import org.wahlen.voucherengine.api.dto.response.ExportResponse
import org.wahlen.voucherengine.api.dto.response.ExportResultResponse
import org.wahlen.voucherengine.api.dto.response.ExportsListResponse
import org.wahlen.voucherengine.persistence.model.export.Export
import org.wahlen.voucherengine.persistence.repository.ExportRepository
import org.wahlen.voucherengine.service.async.AsyncJobPublisher
import org.wahlen.voucherengine.service.async.command.CustomerExportCommand
import org.wahlen.voucherengine.service.async.command.OrderExportCommand
import org.wahlen.voucherengine.service.async.command.PlaceholderExportCommand
import org.wahlen.voucherengine.service.async.command.PublicationExportCommand
import org.wahlen.voucherengine.service.async.command.RedemptionExportCommand
import org.wahlen.voucherengine.service.async.command.VoucherExportCommand
import tools.jackson.databind.ObjectMapper
import java.util.UUID

/**
 * Service for managing async exports.
 * 
 * Creates Export entities with SCHEDULED status and publishes async jobs to SQS.
 * The actual export work is done by dedicated export services (VoucherExportService, OrderExportService)
 * which are invoked by async commands (VoucherExportCommand, OrderExportCommand).
 * 
 * Export results are stored in S3 and accessible via presigned URLs in the Export.resultUrl field.
 */
@Service
class ExportService(
    private val exportRepository: ExportRepository,
    private val tenantService: TenantService,
    private val asyncJobPublisher: AsyncJobPublisher
) {
    private val objectMapper = ObjectMapper()

    @Transactional
    fun createExport(tenantName: String, request: ExportCreateRequest): ExportResponse {
        val exportedObject = request.exported_object?.lowercase()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "exported_object is required")
        if (exportedObject !in supportedObjects) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "exported_object is not supported")
        }
        val tenant = tenantService.requireTenant(tenantName)
        
        // Create export entity with SCHEDULED status
        val export = Export(
            exportedObject = exportedObject,
            status = "SCHEDULED",
            channel = "API",
            resultUrl = null,
            resultToken = null,
            parameters = request.parameters?.let { toParametersMap(it) },
            tenant = tenant
        )
        val saved = exportRepository.save(export)
        
        // Publish async job based on exported object type
        val command = when (exportedObject) {
            "voucher" -> VoucherExportCommand(
                tenantName = tenantName,
                parameters = mapOf(
                    "exportId" to saved.id.toString(),
                    "format" to "CSV",
                    "fields" to (request.parameters?.fields ?: emptyList()),
                    "order" to request.parameters?.order,
                    "filters" to (request.parameters?.filters ?: emptyMap<String, Any?>())
                )
            )
            "redemption" -> RedemptionExportCommand(
                tenantName = tenantName,
                parameters = mapOf(
                    "exportId" to saved.id.toString(),
                    "format" to "CSV",
                    "fields" to (request.parameters?.fields ?: emptyList()),
                    "order" to request.parameters?.order,
                    "filters" to (request.parameters?.filters ?: emptyMap<String, Any?>())
                )
            )
            "publication" -> PublicationExportCommand(
                tenantName = tenantName,
                parameters = mapOf(
                    "exportId" to saved.id.toString(),
                    "format" to "CSV",
                    "fields" to (request.parameters?.fields ?: emptyList()),
                    "order" to request.parameters?.order,
                    "filters" to (request.parameters?.filters ?: emptyMap<String, Any?>())
                )
            )
            "customer" -> CustomerExportCommand(
                tenantName = tenantName,
                parameters = mapOf(
                    "exportId" to saved.id.toString(),
                    "format" to "CSV",
                    "fields" to (request.parameters?.fields ?: emptyList()),
                    "order" to request.parameters?.order,
                    "filters" to (request.parameters?.filters ?: emptyMap<String, Any?>())
                )
            )
            "order" -> OrderExportCommand(
                tenantName = tenantName,
                parameters = mapOf(
                    "exportId" to saved.id.toString(),
                    "format" to "CSV",
                    "fields" to (request.parameters?.fields ?: emptyList()),
                    "order" to request.parameters?.order,
                    "filters" to (request.parameters?.filters ?: emptyMap<String, Any?>())
                )
            )
            else -> PlaceholderExportCommand(
                tenantName = tenantName,
                exportId = saved.id!!,
                exportedObject = exportedObject,
                parameters = request.parameters?.let { toParametersMap(it) } ?: emptyMap()
            )
        }
        
        asyncJobPublisher.publish(command, tenant)
        
        return toResponse(saved)
    }

    fun listExports(tenantName: String, page: Int, limit: Int, order: String?): ExportsListResponse {
        val normalizedLimit = limit.coerceIn(1, 100)
        val normalizedPage = if (page < 1) 1 else page
        val sort = when (order) {
            "created_at" -> Sort.by("createdAt").ascending()
            "-created_at" -> Sort.by("createdAt").descending()
            "status" -> Sort.by("status").ascending()
            "-status" -> Sort.by("status").descending()
            else -> Sort.by("createdAt").descending()
        }
        val pageRequest = PageRequest.of(normalizedPage - 1, normalizedLimit, sort)
        val exports = exportRepository.findAllByTenantName(tenantName, pageRequest)
        return ExportsListResponse(
            exports = exports.content.map(::toResponse),
            total = exports.totalElements.toInt()
        )
    }

    fun getExport(tenantName: String, id: UUID): ExportResponse? =
        exportRepository.findByIdAndTenantName(id, tenantName)?.let(::toResponse)

    fun deleteExport(tenantName: String, id: UUID): Boolean {
        val export = exportRepository.findByIdAndTenantName(id, tenantName) ?: return false
        exportRepository.delete(export)
        return true
    }

    fun downloadExport(tenantName: String, id: UUID, token: String): String {
        val export = exportRepository.findByIdAndTenantName(id, tenantName)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Export not found")
        if (export.resultToken != token) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid token")
        }
        if (export.status != "DONE") {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Export not ready. Status: ${export.status}")
        }
        if (export.resultUrl == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Export file not available")
        }

        // For async exports, the resultUrl is a presigned S3 URL
        // This endpoint is deprecated - clients should use the S3 URL from result.url directly
        // Return a message telling client to use the S3 URL
        throw ResponseStatusException(
            HttpStatus.SEE_OTHER,
            "Download from S3 URL in result.url: ${export.resultUrl}"
        )
    }

    private fun toResponse(export: Export): ExportResponse =
        ExportResponse(
            id = export.id,
            created_at = export.createdAt,
            status = export.status,
            channel = export.channel,
            result = ExportResultResponse(url = export.resultUrl),
            user_id = export.userId,
            exported_object = export.exportedObject,
            parameters = export.parameters?.let { objectMapper.convertValue(it, ExportParameters::class.java) }
        )

    private fun toParametersMap(parameters: ExportParameters): Map<String, Any?> =
        mapOf(
            "order" to parameters.order,
            "fields" to parameters.fields,
            "filters" to parameters.filters
        )

    private val supportedObjects = setOf(
        "voucher",
        "redemption",
        "publication",
        "customer",
        "order",
        "product",
        "sku",
        "points_expiration",
        "voucher_transactions"
    )
}
