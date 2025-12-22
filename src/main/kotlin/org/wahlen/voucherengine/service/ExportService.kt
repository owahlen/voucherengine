package org.wahlen.voucherengine.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.request.ExportCreateRequest
import org.wahlen.voucherengine.api.dto.request.ExportParameters
import org.wahlen.voucherengine.api.dto.response.ExportResponse
import org.wahlen.voucherengine.api.dto.response.ExportResultResponse
import org.wahlen.voucherengine.api.dto.response.ExportsListResponse
import org.wahlen.voucherengine.persistence.model.export.Export
import org.wahlen.voucherengine.persistence.model.product.Product
import org.wahlen.voucherengine.persistence.model.product.Sku
import org.wahlen.voucherengine.persistence.model.publication.Publication
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.repository.CustomerRepository
import org.wahlen.voucherengine.persistence.repository.ExportRepository
import org.wahlen.voucherengine.persistence.repository.OrderRepository
import org.wahlen.voucherengine.persistence.repository.ProductRepository
import org.wahlen.voucherengine.persistence.repository.PublicationRepository
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import org.wahlen.voucherengine.persistence.repository.SkuRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import java.time.Instant
import java.util.UUID

@Service
class ExportService(
    private val exportRepository: ExportRepository,
    private val tenantService: TenantService,
    private val voucherRepository: VoucherRepository,
    private val redemptionRepository: RedemptionRepository,
    private val publicationRepository: PublicationRepository,
    private val customerRepository: CustomerRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val skuRepository: SkuRepository
) {
    private val objectMapper = ObjectMapper()

    fun createExport(tenantName: String, request: ExportCreateRequest): ExportResponse {
        val exportedObject = request.exported_object?.lowercase()
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "exported_object is required")
        if (exportedObject !in supportedObjects) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "exported_object is not supported")
        }
        val tenant = tenantService.requireTenant(tenantName)
        val token = UUID.randomUUID().toString().replace("-", "")
        val export = Export(
            exportedObject = exportedObject,
            status = "DONE",
            channel = "API",
            resultUrl = null,
            resultToken = token,
            parameters = request.parameters?.let { toParametersMap(it) }
        )
        export.tenant = tenant
        val saved = exportRepository.save(export)
        saved.resultUrl = buildResultUrl(saved.id, token, null)
        return toResponse(exportRepository.save(saved))
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
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Export not found")
        }
        if (export.status != "DONE") {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Export not ready")
        }
        val parameters = export.parameters?.let { objectMapper.convertValue(it, ExportParameters::class.java) }
        val fields = parameters?.fields?.ifEmpty { null } ?: defaultFields.getValue(export.exportedObject)
        val rows = when (export.exportedObject) {
            "voucher" -> buildVoucherRows(tenantName, fields, parameters)
            "redemption" -> buildRedemptionRows(tenantName, fields, parameters)
            "publication" -> buildPublicationRows(tenantName, fields, parameters)
            "customer" -> buildCustomerRows(tenantName, fields, parameters)
            "order" -> buildOrderRows(tenantName, fields, parameters)
            "product" -> buildProductRows(tenantName, fields, parameters)
            "sku" -> buildSkuRows(tenantName, fields, parameters)
            "points_expiration" -> emptyList()
            "voucher_transactions" -> emptyList()
            else -> emptyList()
        }
        return buildCsv(fields, rows)
    }

    private fun buildResultUrl(exportId: UUID?, token: String, baseUrl: String?): String {
        val id = exportId?.toString() ?: "unknown"
        val prefix = baseUrl?.trimEnd('/') ?: ""
        return "$prefix/v1/exports/$id?token=$token"
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

    private fun buildVoucherRows(tenantName: String, fields: List<String>, parameters: ExportParameters?): List<List<String>> {
        var vouchers = voucherRepository.findAllByTenantName(tenantName)
        vouchers = applyMetadataFilters(vouchers, parameters?.filters) { it.metadata }
        vouchers = applyOrder(vouchers, parameters?.order) { it.createdAt to it.updatedAt }
        return vouchers.map { voucher -> fields.map { field -> renderVoucherField(voucher, field, tenantName) } }
    }

    private fun buildRedemptionRows(tenantName: String, fields: List<String>, parameters: ExportParameters?): List<List<String>> {
        var redemptions = redemptionRepository.findAllByTenantName(tenantName)
        redemptions = applyMetadataFilters(redemptions, parameters?.filters) { it.metadata }
        redemptions = applyOrder(redemptions, parameters?.order) { it.createdAt to it.updatedAt }
        return redemptions.map { redemption -> fields.map { field -> renderRedemptionField(redemption, field) } }
    }

    private fun buildPublicationRows(tenantName: String, fields: List<String>, parameters: ExportParameters?): List<List<String>> {
        var publications = publicationRepository.findAllByTenantName(tenantName)
        publications = applyMetadataFilters(publications, parameters?.filters) { it.metadata }
        publications = applyOrder(publications, parameters?.order) { it.createdAt to it.updatedAt }
        return publications.map { publication -> fields.map { field -> renderPublicationField(publication, field) } }
    }

    private fun buildCustomerRows(tenantName: String, fields: List<String>, parameters: ExportParameters?): List<List<String>> {
        var customers = customerRepository.findAllByTenantName(tenantName)
        customers = applyMetadataFilters(customers, parameters?.filters) { it.metadata }
        customers = applyOrder(customers, parameters?.order) { it.createdAt to it.updatedAt }
        return customers.map { customer -> fields.map { field -> renderCustomerField(customer, field) } }
    }

    private fun buildOrderRows(tenantName: String, fields: List<String>, parameters: ExportParameters?): List<List<String>> {
        var orders = orderRepository.findAllByTenantName(tenantName)
        orders = applyMetadataFilters(orders, parameters?.filters) { it.metadata }
        orders = applyOrder(orders, parameters?.order) { it.createdAt to it.updatedAt }
        return orders.map { order -> fields.map { field -> renderOrderField(order, field) } }
    }

    private fun buildProductRows(tenantName: String, fields: List<String>, parameters: ExportParameters?): List<List<String>> {
        var products = productRepository.findAllByTenantName(tenantName)
        products = applyMetadataFilters(products, parameters?.filters) { it.metadata }
        products = applyOrder(products, parameters?.order) { it.createdAt to it.updatedAt }
        return products.map { product -> fields.map { field -> renderProductField(product, field) } }
    }

    private fun buildSkuRows(tenantName: String, fields: List<String>, parameters: ExportParameters?): List<List<String>> {
        var skus = skuRepository.findAllByTenantName(tenantName)
        skus = applyMetadataFilters(skus, parameters?.filters) { it.metadata }
        skus = applyOrder(skus, parameters?.order) { it.createdAt to it.updatedAt }
        return skus.map { sku -> fields.map { field -> renderSkuField(sku, field) } }
    }

    private fun renderVoucherField(voucher: Voucher, field: String, tenantName: String): String {
        val discount = voucher.discountJson
        val value = when (field) {
            "code" -> voucher.code
            "voucher_type" -> voucher.type?.name
            "value" -> when (voucher.type) {
                org.wahlen.voucherengine.persistence.model.voucher.VoucherType.GIFT_VOUCHER -> voucher.giftJson?.balance
                org.wahlen.voucherengine.persistence.model.voucher.VoucherType.LOYALTY_CARD -> voucher.loyaltyCardJson?.points
                else -> discount?.percent_off ?: discount?.amount_off
            }
            "discount_type" -> discount?.type?.name
            "campaign" -> voucher.campaign?.name
            "campaign_id" -> voucher.campaign?.id
            "category" -> voucher.categories.firstOrNull()?.name
            "start_date" -> voucher.startDate
            "expiration_date" -> voucher.expirationDate
            "gift_balance" -> voucher.giftJson?.balance
            "loyalty_balance" -> voucher.loyaltyCardJson?.balance ?: voucher.loyaltyCardJson?.points
            "redemption_quantity" -> voucher.redemptionJson?.quantity
            "redemption_count" -> voucher.id?.let { redemptionRepository.countByVoucherIdAndTenantName(it, tenantName) }
            "active" -> voucher.active
            "qr_code" -> voucher.assets?.qrUrl
            "bar_code" -> voucher.assets?.barcodeUrl
            "metadata" -> voucher.metadata
            "is_referral_code" -> null
            "created_at" -> voucher.createdAt
            "updated_at" -> voucher.updatedAt
            "validity_timeframe_interval" -> voucher.validityTimeframe?.interval
            "validity_timeframe_duration" -> voucher.validityTimeframe?.duration
            "validity_day_of_week" -> voucher.validityDayOfWeek?.joinToString(",")
            "discount_amount_limit" -> null
            "additional_info" -> voucher.additionalInfo
            "customer_id" -> voucher.holder?.id
            "customer_source_id" -> voucher.holder?.sourceId
            "discount_unit_type" -> null
            "discount_unit_effect" -> null
            else -> if (field.startsWith("metadata.")) voucher.metadata?.get(field.removePrefix("metadata.")) else null
        }
        return formatValue(value)
    }

    private fun renderRedemptionField(redemption: Redemption, field: String): String {
        val value = when (field) {
            "id" -> redemption.id
            "object" -> "redemption"
            "voucher_code" -> redemption.voucher?.code
            "campaign" -> redemption.voucher?.campaign?.name
            "promotion_tier_id" -> null
            "customer_id" -> redemption.customer?.id
            "customer_source_id" -> redemption.customer?.sourceId
            "customer_name" -> redemption.customer?.name
            "tracking_id" -> redemption.trackingId
            "order_amount" -> redemption.order?.amount
            "gift_amount" -> redemption.amount
            "loyalty_points" -> redemption.amount
            "result" -> redemption.result?.name
            "failure_code" -> if (redemption.result == org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult.FAILURE) "redemption_failed" else null
            "failure_message" -> redemption.reason
            "metadata" -> redemption.metadata
            "date" -> redemption.createdAt
            else -> if (field.startsWith("metadata.")) redemption.metadata?.get(field.removePrefix("metadata.")) else null
        }
        return formatValue(value)
    }

    private fun renderPublicationField(publication: Publication, field: String): String {
        val value = when (field) {
            "voucher_code" -> publication.voucher?.code ?: publication.vouchers.firstOrNull()?.code
            "code" -> publication.voucher?.code ?: publication.vouchers.firstOrNull()?.code
            "customer_id" -> publication.customer?.id
            "customer_source_id" -> publication.customer?.sourceId
            "date" -> publication.createdAt
            "channel" -> publication.channel
            "campaign" -> publication.campaign?.name
            "is_winner" -> null
            "metadata" -> publication.metadata
            else -> if (field.startsWith("metadata.")) publication.metadata?.get(field.removePrefix("metadata.")) else null
        }
        return formatValue(value)
    }

    private fun renderCustomerField(customer: org.wahlen.voucherengine.persistence.model.customer.Customer, field: String): String {
        val value = when (field) {
            "name" -> customer.name
            "id" -> customer.id
            "description" -> customer.description
            "email" -> customer.email
            "source_id" -> customer.sourceId
            "created_at" -> customer.createdAt
            "updated_at" -> customer.updatedAt
            "phone" -> customer.phone
            "metadata" -> customer.metadata
            else -> if (field.startsWith("metadata.")) customer.metadata?.get(field.removePrefix("metadata.")) else null
        }
        return formatValue(value)
    }

    private fun renderOrderField(order: org.wahlen.voucherengine.persistence.model.order.Order, field: String): String {
        val value = when (field) {
            "id" -> order.id
            "source_id" -> order.sourceId
            "created_at" -> order.createdAt
            "updated_at" -> order.updatedAt
            "status" -> order.status
            "amount" -> order.amount
            "discount_amount" -> order.discountAmount
            "items_discount_amount" -> null
            "total_discount_amount" -> order.discountAmount
            "total_amount" -> order.amount?.minus(order.discountAmount ?: 0)
            "customer_id" -> order.customer?.id
            "referrer_id" -> null
            "metadata" -> order.metadata
            else -> if (field.startsWith("metadata.")) order.metadata?.get(field.removePrefix("metadata.")) else null
        }
        return formatValue(value)
    }

    private fun renderProductField(product: Product, field: String): String {
        val value = when (field) {
            "id" -> product.id
            "name" -> product.name
            "price" -> product.price
            "image_url" -> product.imageUrl
            "source_id" -> product.sourceId
            "attributes" -> product.attributes
            "created_at" -> product.createdAt
            "updated_at" -> product.updatedAt
            "metadata" -> product.metadata
            else -> if (field.startsWith("metadata.")) product.metadata?.get(field.removePrefix("metadata.")) else null
        }
        return formatValue(value)
    }

    private fun renderSkuField(sku: Sku, field: String): String {
        val value = when (field) {
            "id" -> sku.id
            "sku" -> sku.sku
            "product_id" -> sku.product?.id
            "currency" -> sku.currency
            "price" -> sku.price
            "image_url" -> sku.imageUrl
            "source_id" -> sku.sourceId
            "attributes" -> sku.attributes
            "created_at" -> sku.createdAt
            "updated_at" -> sku.updatedAt
            "metadata" -> sku.metadata
            else -> if (field.startsWith("metadata.")) sku.metadata?.get(field.removePrefix("metadata.")) else null
        }
        return formatValue(value)
    }

    private fun <T> applyMetadataFilters(
        entries: List<T>,
        filters: Map<String, Any?>?,
        metadataSelector: (T) -> Map<String, Any?>?
    ): List<T> {
        if (filters.isNullOrEmpty()) return entries
        val metadataFilters = filters.filterKeys { it.startsWith("metadata.") }
        if (metadataFilters.isEmpty()) return entries
        return entries.filter { entry ->
            val metadata = metadataSelector(entry) ?: emptyMap()
            metadataFilters.all { (key, value) ->
                metadata[key.removePrefix("metadata.")]?.toString() == value?.toString()
            }
        }
    }

    private fun <T> applyOrder(entries: List<T>, order: String?, dates: (T) -> Pair<Instant?, Instant?>): List<T> {
        return when (order) {
            "created_at" -> entries.sortedBy { dates(it).first }
            "-created_at" -> entries.sortedByDescending { dates(it).first }
            "updated_at" -> entries.sortedBy { dates(it).second }
            "-updated_at" -> entries.sortedByDescending { dates(it).second }
            else -> entries
        }
    }

    private fun buildCsv(fields: List<String>, rows: List<List<String>>): String {
        val header = fields.joinToString(",") { escapeCsv(it) }
        val body = rows.joinToString("\n") { row -> row.joinToString(",") { escapeCsv(it) } }
        return if (body.isBlank()) header else "$header\n$body"
    }

    private fun formatValue(value: Any?): String {
        if (value == null) return ""
        return when (value) {
            is Instant -> value.toString()
            is Map<*, *>, is List<*> -> objectMapper.writeValueAsString(value)
            else -> value.toString()
        }
    }

    private fun escapeCsv(value: String): String {
        val needsEscaping = value.contains(",") || value.contains("\"") || value.contains("\n")
        if (!needsEscaping) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

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

    private val defaultFields = mapOf(
        "voucher" to listOf("code", "voucher_type", "value", "discount_type"),
        "redemption" to listOf("id", "object", "voucher_code", "customer_id", "date", "result"),
        "publication" to listOf("code", "customer_id", "date", "channel"),
        "customer" to listOf("name", "source_id"),
        "order" to listOf("id", "source_id", "status"),
        "product" to listOf("id", "name", "price", "image_url", "source_id", "attributes", "created_at"),
        "sku" to listOf("id", "sku", "product_id", "currency", "price", "image_url", "source_id", "attributes", "created_at"),
        "points_expiration" to listOf("id", "campaign_id", "voucher_id", "status", "expires_at", "points"),
        "voucher_transactions" to listOf("id", "type", "source_id", "status", "reason", "source", "balance", "amount", "created_at")
    )
}
