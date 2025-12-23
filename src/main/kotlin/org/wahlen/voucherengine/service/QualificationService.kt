package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.QualificationFilters
import org.wahlen.voucherengine.api.dto.request.QualificationRequest
import org.wahlen.voucherengine.api.dto.request.VoucherValidationRequest
import org.wahlen.voucherengine.api.dto.response.CategoryResponse
import org.wahlen.voucherengine.api.dto.response.QualificationItemList
import org.wahlen.voucherengine.api.dto.response.QualificationOrderSummary
import org.wahlen.voucherengine.api.dto.response.QualificationRedeemable
import org.wahlen.voucherengine.api.dto.response.QualificationRedeemableResult
import org.wahlen.voucherengine.api.dto.response.QualificationRedeemables
import org.wahlen.voucherengine.api.dto.response.QualificationResponse
import org.wahlen.voucherengine.api.dto.response.StackingRulesResponse
import org.wahlen.voucherengine.api.dto.response.QualificationGiftResult
import org.wahlen.voucherengine.api.dto.response.QualificationLoyaltyCardResult
import org.wahlen.voucherengine.api.dto.response.ValidationRuleAssignmentResponse
import org.wahlen.voucherengine.api.dto.response.ValidationRulesAssignmentsListResponse
import org.wahlen.voucherengine.api.dto.response.ValidationResponse
import org.wahlen.voucherengine.api.dto.response.ValidationOrderSummary
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import java.time.Instant

@Service
class QualificationService(
    private val voucherRepository: VoucherRepository,
    private val customerService: CustomerService,
    private val voucherService: VoucherService,
    private val validationRulesAssignmentRepository: org.wahlen.voucherengine.persistence.repository.ValidationRulesAssignmentRepository
) {

    @Transactional(readOnly = true)
    fun qualify(tenantName: String, request: QualificationRequest): QualificationResponse {
        val scenario = request.scenario?.uppercase()
        val audienceOnly = scenario == "AUDIENCE_ONLY"
        val customer = request.customer?.source_id?.let { customerService.getByIdOrSource(tenantName, it) }
        val vouchers = when (scenario) {
            "CUSTOMER_WALLET", "PRODUCTS_BY_CUSTOMER", "PRODUCTS_DISCOUNT_BY_CUSTOMER" -> {
                customer?.id?.let { voucherRepository.findAllByTenantNameAndHolderId(tenantName, it) } ?: emptyList()
            }
            else -> voucherRepository.findAllByTenantName(tenantName)
        }

        val options = request.options
        val limit = (options?.limit ?: 5).coerceIn(1, 50)
        val sortedByCreatedAt = vouchers.sortedByDescending { it.createdAt }
        val filteredByCursor = options?.starting_after?.let { cursor ->
            sortedByCreatedAt.filter { it.createdAt != null && it.createdAt!!.isBefore(cursor) }
        } ?: sortedByCreatedAt
        val filtered = applyFilters(filteredByCursor, options?.filters)

        val scored = filtered.mapNotNull { voucher ->
            val code = voucher.code ?: return@mapNotNull null
            val validationRequest = VoucherValidationRequest(
                customer = request.customer,
                order = if (audienceOnly) null else request.order,
                metadata = request.metadata
            )
            val validation = voucherService.validateVoucherForQualification(
                tenantName,
                code,
                validationRequest,
                allowedRulePrefixes = if (audienceOnly) setOf("customer.") else null
            )
            if (!validation.valid) return@mapNotNull null
            val discountAmount = validation.order?.discount_amount ?: 0L
            QualificationCandidate(voucher, validation, discountAmount)
        }
        val sortedByRule = applySorting(scored, options?.sorting_rule)
        val limited = sortedByRule.take(limit)
        val redeemables = limited.map { toRedeemable(it.voucher, it.validation, options?.expand, tenantName, request) }

        val hasMore = sortedByRule.size > redeemables.size
        val lastCreatedAt = redeemables.lastOrNull()?.created_at
        val trackingSource = request.customer?.source_id ?: request.tracking_id
        val trackingId = trackingSource?.let(::hashTrackingId)
        val orderSummary = buildOrderSummary(request)
        return QualificationResponse(
            redeemables = QualificationRedeemables(
                data = redeemables,
                total = redeemables.size,
                has_more = hasMore,
                more_starting_after = if (hasMore) lastCreatedAt else null
            ),
            tracking_id = trackingId,
            order = orderSummary,
            stacking_rules = StackingRulesResponse()
        )
    }

    private fun toRedeemable(
        voucher: Voucher,
        validation: ValidationResponse,
        expand: List<String>?,
        tenantName: String,
        request: QualificationRequest
    ): QualificationRedeemable {
        val includeRedeemable = expand?.contains("redeemable") == true
        val includeCategory = expand?.contains("category") == true
        val includeValidationRules = expand?.contains("validation_rules") == true
        val lockRequested = request.session?.type?.uppercase() == "LOCK"
        val categories = if (includeCategory) {
            voucher.categories.map {
                CategoryResponse(
                    id = it.id,
                    name = it.name,
                    created_at = it.createdAt
                )
            }
        } else null
        val giftResult = if (voucher.type == VoucherType.GIFT_VOUCHER) {
            QualificationGiftResult(
                balance = voucher.giftJson?.balance,
                credits = null,
                locked_credits = if (lockRequested) 0L else null
            )
        } else null
        val loyaltyResult = if (voucher.type == VoucherType.LOYALTY_CARD) {
            QualificationLoyaltyCardResult(points = voucher.loyaltyCardJson?.points ?: voucher.loyaltyCardJson?.balance)
        } else null
        val result = QualificationRedeemableResult(
            discount = validation.discount,
            gift = giftResult,
            loyalty_card = loyaltyResult
        )
        val order = validation.order?.let {
            QualificationOrderSummary(
                amount = it.amount,
                total_amount = it.total_amount,
                items = buildOrderItems(request)
            )
        }
        val assignments = if (includeValidationRules) {
            val entries = validationRulesAssignmentRepository.findByRelatedObjectIdAndRelatedObjectTypeAndTenantName(
                voucher.code ?: "",
                "voucher",
                tenantName
            )
            val data = entries.map {
                ValidationRuleAssignmentResponse(
                    id = it.id,
                    rule_id = it.ruleId,
                    related_object_id = it.relatedObjectId,
                    related_object_type = it.relatedObjectType,
                    validation_status = it.validationStatus,
                    validation_omitted_rules = it.omittedRules,
                    created_at = it.createdAt,
                    updated_at = it.updatedAt
                )
            }
            ValidationRulesAssignmentsListResponse(data = data, total = data.size)
        } else null
        return QualificationRedeemable(
            id = voucher.code,
            `object` = "voucher",
            created_at = voucher.createdAt,
            result = result,
            order = order,
            applicable_to = QualificationItemList(),
            inapplicable_to = QualificationItemList(),
            name = if (includeRedeemable) voucher.code else null,
            campaign_name = if (includeRedeemable) voucher.campaign?.name else null,
            campaign_id = if (includeRedeemable) voucher.campaign?.id?.toString() else null,
            metadata = if (includeRedeemable) voucher.metadata else null,
            categories = categories,
            validation_rules_assignments = assignments
        )
    }

    private fun applyFilters(vouchers: List<Voucher>, filters: QualificationFilters?): List<Voucher> {
        if (filters == null) return vouchers
        val filtered = vouchers.filter { voucher ->
            val checks = mutableListOf<Boolean>()
            filters.category_id?.let { checks.add(matches(voucher.categories.mapNotNull { it.id?.toString() }, it)) }
            filters.campaign_id?.let { checks.add(matches(voucher.campaign?.id?.toString(), it)) }
            filters.campaign_type?.let { checks.add(matches(voucher.campaign?.type?.name, it)) }
            filters.voucher_type?.let { checks.add(matches(voucher.type?.name, it)) }
            filters.code?.let { checks.add(matches(voucher.code, it)) }
            filters.resource_id?.let { checks.add(matches(voucher.id?.toString(), it)) }
            filters.resource_type?.let { checks.add(matches("voucher", it)) }
            filters.holder_role?.let { checks.add(matchesHolderRole(voucher, it)) }
            if (checks.isEmpty()) true else {
                val junction = filters.junction?.uppercase()
                if (junction == "OR") checks.any { it } else checks.all { it }
            }
        }
        return filtered
    }

    private fun matches(value: String?, condition: org.wahlen.voucherengine.api.dto.request.QualificationFieldConditions): Boolean {
        val conditions = condition.conditions ?: return true
        val isValues = conditions.`is` ?: emptyList()
        val isNotValues = conditions.is_not ?: emptyList()
        val inValues = conditions.`in` ?: emptyList()
        val notInValues = conditions.not_in ?: emptyList()
        if (conditions.is_unknown == true) {
            return value == null || value.isBlank()
        }
        if (value == null) return false
        if (conditions.has_value == true && value.isBlank()) return false
        if (isNotValues.contains(value) || notInValues.contains(value)) return false
        if (isValues.isNotEmpty() && !isValues.contains(value)) return false
        if (inValues.isNotEmpty() && !inValues.contains(value)) return false
        return true
    }

    private fun matches(values: List<String>, condition: org.wahlen.voucherengine.api.dto.request.QualificationFieldConditions): Boolean {
        if (values.isEmpty()) {
            return condition.conditions?.is_unknown == true
        }
        return values.any { matches(it, condition) }
    }

    private fun matchesHolderRole(voucher: Voucher, condition: org.wahlen.voucherengine.api.dto.request.QualificationFieldConditions): Boolean {
        val role = if (voucher.holder != null) "OWNER" else null
        return if (role == null) {
            condition.conditions?.is_unknown == true
        } else {
            matches(role, condition)
        }
    }

    private fun applySorting(
        candidates: List<QualificationCandidate>,
        sortingRule: String?
    ): List<QualificationCandidate> {
        return when (sortingRule?.uppercase()) {
            "BEST_DEAL" -> candidates.sortedByDescending { it.discountAmount }
            "LEAST_DEAL" -> candidates.sortedBy { it.discountAmount }
            else -> candidates.sortedByDescending { it.voucher.createdAt }
        }
    }

    private fun hashTrackingId(source: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(source.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun buildOrderSummary(request: QualificationRequest): QualificationOrderSummary? {
        val amount = request.order?.amount ?: return null
        return QualificationOrderSummary(
            amount = amount,
            total_amount = amount,
            items = buildOrderItems(request)
        )
    }

    private fun buildOrderItems(request: QualificationRequest): List<org.wahlen.voucherengine.api.dto.response.OrderItemResponse>? {
        val items = request.order?.items ?: return null
        return items.map { item ->
            val amount = (item.price ?: 0L) * (item.quantity ?: 0)
            org.wahlen.voucherengine.api.dto.response.OrderItemResponse(
                product_id = item.product_id ?: item.product?.id,
                sku_id = item.sku_id ?: item.sku?.id,
                quantity = item.quantity,
                amount = amount,
                subtotal_amount = amount,
                price = item.price
            )
        }
    }

    private data class QualificationCandidate(
        val voucher: Voucher,
        val validation: ValidationResponse,
        val discountAmount: Long
    )
}
