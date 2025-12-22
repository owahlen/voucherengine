package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.QualificationFilters
import org.wahlen.voucherengine.api.dto.request.QualificationRequest
import org.wahlen.voucherengine.api.dto.request.VoucherValidationRequest
import org.wahlen.voucherengine.api.dto.response.CategoryResponse
import org.wahlen.voucherengine.api.dto.response.QualificationOrderSummary
import org.wahlen.voucherengine.api.dto.response.QualificationRedeemable
import org.wahlen.voucherengine.api.dto.response.QualificationRedeemableResult
import org.wahlen.voucherengine.api.dto.response.QualificationRedeemables
import org.wahlen.voucherengine.api.dto.response.QualificationResponse
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import java.time.Instant

@Service
class QualificationService(
    private val voucherRepository: VoucherRepository,
    private val customerService: CustomerService,
    private val voucherService: VoucherService
) {

    @Transactional(readOnly = true)
    fun qualify(tenantName: String, request: QualificationRequest): QualificationResponse {
        val scenario = request.scenario?.uppercase()
        val customer = request.customer?.source_id?.let { customerService.getByIdOrSource(tenantName, it) }
        val vouchers = when (scenario) {
            "CUSTOMER_WALLET", "PRODUCTS_BY_CUSTOMER", "PRODUCTS_DISCOUNT_BY_CUSTOMER" -> {
                customer?.id?.let { voucherRepository.findAllByTenantNameAndHolderId(tenantName, it) } ?: emptyList()
            }
            else -> voucherRepository.findAllByTenantName(tenantName)
        }

        val options = request.options
        val limit = (options?.limit ?: 5).coerceIn(1, 50)
        val sorted = vouchers.sortedByDescending { it.createdAt }
        val filteredByCursor = options?.starting_after?.let { cursor ->
            sorted.filter { it.createdAt != null && it.createdAt!!.isBefore(cursor) }
        } ?: sorted
        val filtered = applyFilters(filteredByCursor, options?.filters)

        val redeemables = mutableListOf<QualificationRedeemable>()
        for (voucher in filtered) {
            val validation = voucherService.validateVoucherForQualification(
                tenantName,
                voucher.code ?: continue,
                VoucherValidationRequest(customer = request.customer, order = request.order)
            )
            if (!validation.valid) {
                continue
            }
            redeemables.add(toRedeemable(voucher, validation, options?.expand))
            if (redeemables.size >= limit) {
                break
            }
        }

        val hasMore = filtered.size > redeemables.size
        val lastCreatedAt = redeemables.lastOrNull()?.created_at
        val orderAmount = request.order?.amount
        val orderSummary = orderAmount?.let {
            QualificationOrderSummary(amount = it, total_amount = it)
        }
        val trackingId = request.tracking_id ?: request.customer?.source_id
        return QualificationResponse(
            redeemables = QualificationRedeemables(
                data = redeemables,
                total = redeemables.size,
                has_more = hasMore,
                more_starting_after = if (hasMore) lastCreatedAt else null
            ),
            tracking_id = trackingId,
            order = orderSummary
        )
    }

    private fun toRedeemable(
        voucher: Voucher,
        validation: org.wahlen.voucherengine.service.dto.ValidationResponse,
        expand: List<String>?
    ): QualificationRedeemable {
        val includeRedeemable = expand?.contains("redeemable") == true
        val includeCategory = expand?.contains("category") == true
        val categories = if (includeCategory) {
            voucher.categories.map {
                CategoryResponse(
                    id = it.id,
                    name = it.name,
                    created_at = it.createdAt
                )
            }
        } else null
        val result = QualificationRedeemableResult(discount = validation.discount)
        val order = validation.order?.let { QualificationOrderSummary(amount = it.amount, total_amount = it.total_amount) }
        return QualificationRedeemable(
            id = voucher.code,
            `object` = "voucher",
            created_at = voucher.createdAt,
            result = result,
            order = order,
            name = if (includeRedeemable) voucher.code else null,
            campaign_name = if (includeRedeemable) voucher.campaign?.name else null,
            campaign_id = if (includeRedeemable) voucher.campaign?.id?.toString() else null,
            metadata = if (includeRedeemable) voucher.metadata else null,
            categories = categories
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
            filters.holder_role?.let { checks.add(true) }
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
        if (value == null) return false
        if (isNotValues.contains(value) || notInValues.contains(value)) return false
        if (isValues.isNotEmpty() && !isValues.contains(value)) return false
        if (inValues.isNotEmpty() && !inValues.contains(value)) return false
        return true
    }

    private fun matches(values: List<String>, condition: org.wahlen.voucherengine.api.dto.request.QualificationFieldConditions): Boolean {
        if (values.isEmpty()) return false
        return values.any { matches(it, condition) }
    }
}
