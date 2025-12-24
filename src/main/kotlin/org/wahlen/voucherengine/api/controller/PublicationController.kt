package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.wahlen.voucherengine.api.dto.request.PublicationCreateRequest
import org.wahlen.voucherengine.api.dto.request.PublicationCampaignRequest
import org.wahlen.voucherengine.api.dto.request.CustomerReferenceDto
import org.wahlen.voucherengine.api.dto.response.PublicationResponse
import org.wahlen.voucherengine.api.dto.response.PublicationsListResponse
import org.wahlen.voucherengine.service.PublicationService
import kotlin.math.max
import kotlin.math.min
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class PublicationController(
    private val publicationService: PublicationService
) {

    @Operation(
        summary = "Create publication",
        operationId = "createPublication",
        responses = [
            ApiResponse(responseCode = "200", description = "Publication created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PostMapping("/publications")
    fun createPublication(
        @RequestHeader("tenant") tenant: String,
        @RequestParam(name = "join_once", required = false) joinOnce: Boolean?,
        @Valid @RequestBody body: PublicationCreateRequest
    ): ResponseEntity<PublicationResponse> =
        ResponseEntity.ok(publicationService.createPublication(tenant, body, joinOnce))

    @Operation(
        summary = "Create publication (GET)",
        operationId = "createPublicationGet",
        responses = [
            ApiResponse(responseCode = "200", description = "Publication created"),
            ApiResponse(responseCode = "400", description = "Validation error"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/publications/create")
    fun createPublicationGet(
        @RequestHeader("tenant") tenant: String,
        @RequestParam(name = "join_once", required = false) joinOnce: Boolean?,
        @RequestParam(name = "voucher", required = false) voucher: String?,
        @RequestParam(name = "campaign[name]", required = false) campaignName: String?,
        @RequestParam(name = "campaign[count]", required = false) campaignCount: Int?,
        @RequestParam(name = "source_id", required = false) sourceId: String?,
        @RequestParam(name = "customer[source_id]") customerSourceId: String,
        @RequestParam(name = "customer[name]", required = false) customerName: String?,
        @RequestParam(name = "customer[email]", required = false) customerEmail: String?,
        @RequestParam(name = "customer[phone]", required = false) customerPhone: String?,
        @RequestParam(name = "channel", required = false) channel: String?
    ): ResponseEntity<PublicationResponse> {
        if (campaignCount != null && campaignCount != 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "campaign.count must be 1 for GET")
        }
        val customer = CustomerReferenceDto(
            source_id = customerSourceId,
            email = customerEmail,
            name = customerName,
            phone = customerPhone
        )
        val campaign = campaignName?.let { PublicationCampaignRequest(name = it, count = campaignCount) }
        val request = PublicationCreateRequest(
            source_id = sourceId,
            customer = customer,
            channel = channel,
            voucher = voucher,
            campaign = campaign
        )
        return ResponseEntity.ok(publicationService.createPublication(tenant, request, joinOnce))
    }

    @Operation(
        summary = "List publications",
        operationId = "listPublications",
        responses = [
            ApiResponse(responseCode = "200", description = "List of publications")
        ]
    )
    @GetMapping("/publications")
    fun listPublications(
        @RequestHeader("tenant") tenant: String,
        @RequestParam(name = "campaign", required = false) campaign: String?,
        @RequestParam(name = "customer", required = false) customer: String?,
        @RequestParam(name = "voucher", required = false) voucher: String?,
        @RequestParam(name = "result", required = false) result: String?,
        @RequestParam(name = "source_id", required = false) sourceId: String?,
        @RequestParam(name = "voucher_type", required = false) voucherType: String?,
        @RequestParam(name = "is_referral_code", required = false) isReferralCode: Boolean?,
        @RequestParam(name = "order", required = false) order: String?,
        @RequestParam(name = "page", required = false, defaultValue = "1") page: Int,
        @RequestParam(name = "limit", required = false, defaultValue = "10") limit: Int,
        request: HttpServletRequest
    ): ResponseEntity<PublicationsListResponse> {
        if (page > 1000) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page_over_limit")
        }
        val entries = publicationService.listPublications(tenant, campaign, customer, voucher, result, sourceId)
        val filtered = applyFilters(entries, voucherType, isReferralCode, request)
        val sorted = applyOrder(filtered, order)
        val normalizedLimit = max(1, min(limit, 100))
        val start = max(0, (page - 1) * normalizedLimit)
        val paged = if (start >= sorted.size) emptyList() else sorted.drop(start).take(normalizedLimit)
        return ResponseEntity.ok(
            PublicationsListResponse(
                publications = paged.map(publicationService::toResponse),
                total = sorted.size
            )
        )
    }

    @Operation(
        summary = "List publications for voucher",
        operationId = "listVoucherPublications",
        responses = [
            ApiResponse(responseCode = "200", description = "List of publications")
        ]
    )
    @GetMapping("/vouchers/{code}/publications")
    fun listVoucherPublications(
        @RequestHeader("tenant") tenant: String,
        @RequestParam(name = "page", required = false, defaultValue = "1") page: Int,
        @RequestParam(name = "limit", required = false, defaultValue = "10") limit: Int,
        @PathVariable code: String
    ): ResponseEntity<PublicationsListResponse> {
        if (page > 1000) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page_over_limit")
        }
        val entries = publicationService.listPublications(tenant, null, null, code, null, null)
        val normalizedLimit = max(1, min(limit, 100))
        val start = max(0, (page - 1) * normalizedLimit)
        val paged = if (start >= entries.size) emptyList() else entries.drop(start).take(normalizedLimit)
        return ResponseEntity.ok(
            PublicationsListResponse(
                publications = paged.map(publicationService::toResponse),
                total = entries.size
            )
        )
    }

    @Operation(
        summary = "Delete publication",
        operationId = "deletePublication",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/publications/{id}")
    fun deletePublication(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: java.util.UUID
    ): ResponseEntity<Void> {
        return if (publicationService.deletePublication(tenant, id)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(
        summary = "Get publication",
        operationId = "getPublication",
        responses = [
            ApiResponse(responseCode = "200", description = "Publication found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/publications/{id}")
    fun getPublication(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: java.util.UUID
    ): ResponseEntity<PublicationResponse> {
        val publication = publicationService.getPublication(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(publicationService.toResponse(publication))
    }

    private fun applyOrder(
        entries: List<org.wahlen.voucherengine.persistence.model.publication.Publication>,
        order: String?
    ): List<org.wahlen.voucherengine.persistence.model.publication.Publication> {
        val normalized = order?.trim().orEmpty()
        return when (normalized) {
            "id" -> entries.sortedBy { it.id }
            "-id" -> entries.sortedByDescending { it.id }
            "voucher_code" -> entries.sortedBy { it.voucher?.code ?: it.vouchers.firstOrNull()?.code }
            "-voucher_code" -> entries.sortedByDescending { it.voucher?.code ?: it.vouchers.firstOrNull()?.code }
            "tracking_id" -> entries.sortedBy { it.customer?.sourceId }
            "-tracking_id" -> entries.sortedByDescending { it.customer?.sourceId }
            "customer_id" -> entries.sortedBy { it.customer?.id }
            "-customer_id" -> entries.sortedByDescending { it.customer?.id }
            "created_at" -> entries.sortedBy { it.createdAt }
            "-created_at" -> entries.sortedByDescending { it.createdAt }
            "channel" -> entries.sortedBy { it.channel }
            "-channel" -> entries.sortedByDescending { it.channel }
            else -> entries.sortedByDescending { it.createdAt }
        }
    }

    private fun applyFilters(
        entries: List<org.wahlen.voucherengine.persistence.model.publication.Publication>,
        voucherType: String?,
        isReferralCode: Boolean?,
        request: HttpServletRequest
    ): List<org.wahlen.voucherengine.persistence.model.publication.Publication> {
        val filterParams = request.parameterMap
        val junction = filterParams["filters[junction]"]?.firstOrNull()?.uppercase()
        val customerFilter = extractStringFilter(filterParams, "customer_id")
        val voucherFilter = extractStringFilter(filterParams, "voucher_code")
        val campaignFilter = extractStringFilter(filterParams, "campaign_name")
        val failureFilter = extractStringFilter(filterParams, "failure_code")
        val resultFilter = extractStringFilter(filterParams, "result")
        val sourceFilter = extractStringFilter(filterParams, "source_id")
        val voucherTypeFilter = extractStringFilter(filterParams, "voucher_type")
        val referralFilter = extractStringFilter(filterParams, "is_referral_code")
        val parentFilter = extractStringFilter(filterParams, "parent_object_id")
        val relatedFilter = extractStringFilter(filterParams, "related_object_id")

        return entries.filter { publication ->
            val vouchers = if (publication.vouchers.isNotEmpty()) publication.vouchers else listOfNotNull(publication.voucher)
            val voucherCodes = vouchers.mapNotNull { it.code }
            val voucherTypes = vouchers.mapNotNull { it.type?.name }
            val campaignNames = vouchers.mapNotNull { it.campaign?.name }.ifEmpty { publication.campaign?.name?.let { listOf(it) } ?: emptyList() }
            val isReferral = vouchers.any {
                it.campaign?.type == org.wahlen.voucherengine.persistence.model.campaign.CampaignType.REFERRAL_PROGRAM
            } || publication.campaign?.type == org.wahlen.voucherengine.persistence.model.campaign.CampaignType.REFERRAL_PROGRAM
            val checks = mutableListOf<Boolean>()

            customerFilter?.let { checks.add(matchesStringFilter(publication.customer?.id?.toString(), it)) }
            voucherFilter?.let { checks.add(matchesStringFilter(voucherCodes, it)) }
            campaignFilter?.let { checks.add(matchesStringFilter(campaignNames, it)) }
            failureFilter?.let { checks.add(matchesStringFilter(publication.failureCode, it)) }
            resultFilter?.let { checks.add(matchesStringFilter(publication.result?.name, it)) }
            sourceFilter?.let { checks.add(matchesStringFilter(publication.sourceId, it)) }
            voucherTypeFilter?.let { checks.add(matchesStringFilter(voucherTypes, it)) }
            referralFilter?.let { checks.add(matchesStringFilter(isReferral.toString(), it)) }
            parentFilter?.let { checks.add(matchesStringFilter(publication.campaign?.id?.toString(), it)) }
            relatedFilter?.let { checks.add(matchesStringFilter(vouchers.mapNotNull { it.id?.toString() }, it)) }

            if (isReferralCode != null) {
                checks.add(isReferralCode == isReferral)
            }
            voucherType?.let { type ->
                checks.add(voucherTypes.any { it.equals(type, ignoreCase = true) })
            }

            if (checks.isEmpty()) {
                true
            } else if (junction == "OR") {
                checks.any { it }
            } else {
                checks.all { it }
            }
        }
    }

    private data class StringFilter(
        val isValues: Set<String>,
        val inValues: Set<String>,
        val isNotValues: Set<String>,
        val notInValues: Set<String>,
        val containsValues: List<String>,
        val startsWithValues: List<String>,
        val endsWithValues: List<String>,
        val moreThanValues: List<String>,
        val lessThanValues: List<String>,
        val moreThanEqualValues: List<String>,
        val lessThanEqualValues: List<String>,
        val hasValue: Boolean,
        val isUnknown: Boolean
    ) {
        fun hasIncludeConditions(): Boolean =
            isValues.isNotEmpty() ||
                inValues.isNotEmpty() ||
                containsValues.isNotEmpty() ||
                startsWithValues.isNotEmpty() ||
                endsWithValues.isNotEmpty() ||
                moreThanValues.isNotEmpty() ||
                lessThanValues.isNotEmpty() ||
                moreThanEqualValues.isNotEmpty() ||
                lessThanEqualValues.isNotEmpty()
    }

    private fun extractStringFilter(
        params: Map<String, Array<String>>,
        field: String
    ): StringFilter? {
        val isValues = extractFilterValues(params, field, "\$is").toSet()
        val inValues = extractFilterValues(params, field, "\$in").toSet()
        val isNotValues = extractFilterValues(params, field, "\$is_not").toSet()
        val notInValues = extractFilterValues(params, field, "\$not_in").toSet()
        val containsValues = extractFilterValues(params, field, "\$contains")
        val startsWithValues = extractFilterValues(params, field, "\$starts_with")
        val endsWithValues = extractFilterValues(params, field, "\$ends_with")
        val moreThanValues = extractFilterValues(params, field, "\$more_than")
        val lessThanValues = extractFilterValues(params, field, "\$less_than")
        val moreThanEqualValues = extractFilterValues(params, field, "\$more_than_equal")
        val lessThanEqualValues = extractFilterValues(params, field, "\$less_than_equal")
        val hasValueValues = extractFilterValues(params, field, "\$has_value")
        val isUnknownValues = extractFilterValues(params, field, "\$is_unknown")
        val hasValue = hasValueValues.any { it.equals("true", ignoreCase = true) } || hasValueValues.any { it.isBlank() }
        val isUnknown = isUnknownValues.any { it.equals("true", ignoreCase = true) } || isUnknownValues.any { it.isBlank() }

        return if (
            isValues.isEmpty() &&
            inValues.isEmpty() &&
            isNotValues.isEmpty() &&
            notInValues.isEmpty() &&
            containsValues.isEmpty() &&
            startsWithValues.isEmpty() &&
            endsWithValues.isEmpty() &&
            moreThanValues.isEmpty() &&
            lessThanValues.isEmpty() &&
            moreThanEqualValues.isEmpty() &&
            lessThanEqualValues.isEmpty() &&
            !hasValue &&
            !isUnknown
        ) {
            null
        } else {
            StringFilter(
                isValues = isValues,
                inValues = inValues,
                isNotValues = isNotValues,
                notInValues = notInValues,
                containsValues = containsValues,
                startsWithValues = startsWithValues,
                endsWithValues = endsWithValues,
                moreThanValues = moreThanValues,
                lessThanValues = lessThanValues,
                moreThanEqualValues = moreThanEqualValues,
                lessThanEqualValues = lessThanEqualValues,
                hasValue = hasValue,
                isUnknown = isUnknown
            )
        }
    }

    private fun extractFilterValues(
        params: Map<String, Array<String>>,
        field: String,
        operator: String
    ): List<String> {
        val prefix = "filters[$field][conditions][$operator]"
        return params.entries
            .filter { it.key.startsWith(prefix) }
            .flatMap { it.value.toList() }
    }

    private fun matchesStringFilter(value: String?, filter: StringFilter): Boolean {
        val normalized = value?.trim()
        if (normalized.isNullOrBlank()) {
            return when {
                filter.isUnknown -> true
                filter.hasValue -> false
                filter.hasIncludeConditions() -> false
                else -> true
            }
        }

        if (filter.isUnknown) {
            return false
        }
        if (filter.hasValue && normalized.isBlank()) {
            return false
        }
        if (isExcluded(normalized, filter)) {
            return false
        }
        if (!filter.hasIncludeConditions()) {
            return true
        }
        return matchesInclude(normalized, filter)
    }

    private fun matchesStringFilter(values: List<String>, filter: StringFilter): Boolean {
        if (values.isEmpty()) {
            return matchesStringFilter(null, filter)
        }
        if (filter.isUnknown) {
            return false
        }
        val trimmed = values.map { it.trim() }
        if (filter.hasValue && trimmed.all { it.isBlank() }) {
            return false
        }
        if (trimmed.any { isExcluded(it, filter) }) {
            return false
        }
        if (!filter.hasIncludeConditions()) {
            return true
        }
        return trimmed.any { matchesInclude(it, filter) }
    }

    private fun matchesInclude(value: String, filter: StringFilter): Boolean {
        if ((filter.isValues + filter.inValues).contains(value)) return true
        if (filter.containsValues.any { value.contains(it, ignoreCase = true) }) return true
        if (filter.startsWithValues.any { value.startsWith(it, ignoreCase = true) }) return true
        if (filter.endsWithValues.any { value.endsWith(it, ignoreCase = true) }) return true
        if (filter.moreThanValues.any { compareValues(value, it) > 0 }) return true
        if (filter.lessThanValues.any { compareValues(value, it) < 0 }) return true
        if (filter.moreThanEqualValues.any { compareValues(value, it) >= 0 }) return true
        if (filter.lessThanEqualValues.any { compareValues(value, it) <= 0 }) return true
        return false
    }

    private fun isExcluded(value: String, filter: StringFilter): Boolean =
        filter.isNotValues.contains(value) || filter.notInValues.contains(value)

    private fun compareValues(left: String, right: String): Int =
        left.compareTo(right, ignoreCase = true)
}
