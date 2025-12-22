package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.DiscountType
import org.wahlen.voucherengine.api.dto.common.RedemptionDto
import org.wahlen.voucherengine.api.dto.response.AssetDto
import org.wahlen.voucherengine.api.dto.response.VoucherAssetsDto
import org.wahlen.voucherengine.api.dto.response.VoucherResponse
import org.wahlen.voucherengine.api.dto.request.*
import org.wahlen.voucherengine.api.dto.response.CategoryResponse
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionStatus
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.repository.CampaignRepository
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import org.wahlen.voucherengine.persistence.repository.CategoryRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import org.wahlen.voucherengine.persistence.repository.ValidationRulesAssignmentRepository
import org.wahlen.voucherengine.persistence.repository.ValidationRuleRepository
import org.wahlen.voucherengine.service.dto.ErrorResponse
import org.wahlen.voucherengine.service.dto.RedemptionResponse
import org.wahlen.voucherengine.service.dto.ValidationOrderSummary
import org.wahlen.voucherengine.service.dto.ValidationResponse
import org.wahlen.voucherengine.service.rules.RuleEvaluator
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.Instant
import java.util.UUID

@Service
class VoucherService(
    private val voucherRepository: VoucherRepository,
    private val redemptionRepository: RedemptionRepository,
    private val customerService: CustomerService,
    private val categoryRepository: CategoryRepository,
    private val campaignRepository: CampaignRepository,
    private val validationRulesAssignmentRepository: ValidationRulesAssignmentRepository,
    private val validationRuleRepository: ValidationRuleRepository,
    private val clock: Clock,
) {

    @Transactional
    fun createVoucher(request: VoucherCreateRequest): Voucher {
        val holder = customerService.ensureCustomer(request.customer)
        val campaign = request.campaign_id?.let { campaignRepository.findById(it).orElse(null) }
        val voucher = Voucher(
            code = request.code,
            type = request.type?.let { VoucherType.valueOf(it) },
            discountJson = request.discount,
            giftJson = request.gift,
            loyaltyCardJson = request.loyalty_card,
            metadata = request.metadata,
            active = request.active ?: true,
            holder = holder,
            additionalInfo = request.additional_info,
            campaign = campaign,
            startDate = request.start_date,
            expirationDate = request.expiration_date
        )
        voucher.validityTimeframe = request.validity_timeframe
        voucher.validityDayOfWeek = request.validity_day_of_week
        voucher.validityHours = request.validity_hours
        voucher.redemptionJson = RedemptionDto(
            quantity = request.redemption?.quantity,
            redeemed_quantity = 0,
            per_customer = request.redemption?.per_customer
        )
        generateAssetsIfMissing(voucher)
        attachCategories(voucher, request.category_ids)
        return voucherRepository.save(voucher)
    }

    @Transactional
    fun updateVoucher(code: String, request: VoucherCreateRequest): Voucher? {
        val existing = voucherRepository.findByCode(code) ?: return null
        existing.type = request.type?.let { VoucherType.valueOf(it) } ?: existing.type
        existing.discountJson = request.discount ?: existing.discountJson
        existing.giftJson = request.gift ?: existing.giftJson
        existing.loyaltyCardJson = request.loyalty_card ?: existing.loyaltyCardJson
        existing.metadata = request.metadata ?: existing.metadata
        existing.active = request.active ?: existing.active
        existing.additionalInfo = request.additional_info ?: existing.additionalInfo
        existing.validityTimeframe = request.validity_timeframe ?: existing.validityTimeframe
        existing.validityDayOfWeek = request.validity_day_of_week ?: existing.validityDayOfWeek
        existing.validityHours = request.validity_hours ?: existing.validityHours
        existing.startDate = request.start_date ?: existing.startDate
        existing.expirationDate = request.expiration_date ?: existing.expirationDate
        existing.redemptionJson = existing.redemptionJson?.copy(
            quantity = request.redemption?.quantity ?: existing.redemptionJson?.quantity,
            per_customer = request.redemption?.per_customer ?: existing.redemptionJson?.per_customer
        ) ?: RedemptionDto(
            quantity = request.redemption?.quantity,
            per_customer = request.redemption?.per_customer,
            redeemed_quantity = 0
        )
        val holder = customerService.ensureCustomer(request.customer)
        if (holder != null) {
            existing.holder = holder
        }
        existing.campaign = request.campaign_id?.let { campaignRepository.findById(it).orElse(null) } ?: existing.campaign
        attachCategories(existing, request.category_ids)
        return voucherRepository.save(existing)
    }

    @Transactional(readOnly = true)
    fun getByCode(code: String): Voucher? = voucherRepository.findByCode(code)

    @Transactional(readOnly = true)
    fun validateVoucher(code: String, request: VoucherValidationRequest): ValidationResponse {
        val voucher = voucherRepository.findByCode(code)
            ?: return ValidationResponse(false, error = ErrorResponse("voucher_not_found", "Voucher does not exist."))

        if (voucher.active == false) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is inactive."))
        }
        val now = Instant.now(clock)
        if (voucher.startDate != null && now.isBefore(voucher.startDate)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is not yet active."))
        }
        if (voucher.expirationDate != null && now.isAfter(voucher.expirationDate)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_expired", "This voucher has expired."))
        }
        if (!isWithinValidityWindow(voucher, now)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is not active right now."))
        }

        val customer = customerService.ensureCustomer(request.customer)
        if (voucher.holder != null && voucher.holder?.id != customer?.id) {
            return ValidationResponse(false, error = ErrorResponse("voucher_not_assigned", "Voucher assigned to another customer."))
        }

        val totalRedemptions = voucher.id?.let { redemptionRepository.countByVoucherId(it).toInt() } ?: 0
        val perCustomerLimit = voucher.redemptionJson?.per_customer
        val quantityLimit = voucher.redemptionJson?.quantity

        if (quantityLimit != null && totalRedemptions >= quantityLimit) {
            return ValidationResponse(false, error = ErrorResponse("redemption_limit_exceeded", "This voucher reached its total redemption limit."))
        }

        val customerRedemptions = if (customer?.id != null && voucher.id != null) {
            redemptionRepository.countByVoucherIdAndCustomerId(voucher.id!!, customer.id!!).toInt()
        } else 0
        if (perCustomerLimit != null && customer?.id != null) {
            if (customerRedemptions >= perCustomerLimit) {
                val message = if (perCustomerLimit == 1) {
                    "This voucher can be redeemed only once per customer."
                } else {
                    "This voucher can be redeemed only $perCustomerLimit time(s) per customer."
                }
                return ValidationResponse(false, error = ErrorResponse("redemption_limit_per_customer_exceeded", message))
            }
        }

        applyValidationRules(voucher, customer, customerRedemptions, totalRedemptions, request)?.let { return it }

        if (voucher.categories.isNotEmpty()) {
            val categoryIds = request.categories
            if (categoryIds.isNullOrEmpty()) {
                return ValidationResponse(false, error = ErrorResponse("voucher_category_mismatch", "Voucher is not applicable for provided categories."))
            }
            val overlap = voucher.categories.any { it.id != null && categoryIds.contains(it.id) }
            if (!overlap) {
                return ValidationResponse(false, error = ErrorResponse("voucher_category_mismatch", "Voucher is not applicable for provided categories."))
            }
        }

        val orderAmount = request.order?.amount
        val orderUnits = request.order?.items?.sumOf { it.quantity ?: 0 }
        val discountAmount = calculateDiscountAmount(voucher, orderAmount, orderUnits)
        val voucherDto = toVoucherResponse(voucher)
        val discountDto = voucher.discountJson?.let { base ->
            if (discountAmount != null && base.type == DiscountType.PERCENT) {
                base.copy(amount_off = discountAmount)
            } else base
        }
        val orderSummary = if (orderAmount != null) {
            ValidationOrderSummary(
                amount = orderAmount,
                discount_amount = discountAmount,
                total_amount = discountAmount?.let { orderAmount - it } ?: orderAmount
            )
        } else null

        return ValidationResponse(
            valid = true,
            voucher = voucherDto,
            discount = discountDto,
            order = orderSummary
        )
    }

    @Transactional
    fun redeem(request: RedemptionRequest): RedemptionResponse {
        if (request.redeemables.isEmpty()) {
            return RedemptionResponse("failure", error = ErrorResponse("invalid_request", "No redeemables provided"))
        }
        val redeemable = request.redeemables.first()
        if (redeemable.`object` != "voucher") {
            return RedemptionResponse("failure", error = ErrorResponse("unsupported_redeemable", "Only vouchers are supported"))
        }
        val voucher = voucherRepository.findByCode(redeemable.id)
            ?: return RedemptionResponse("failure", error = ErrorResponse("voucher_not_found", "Voucher does not exist."))

        val validation = validateVoucher(voucher.code ?: redeemable.id, VoucherValidationRequest(request.customer, request.order))
        if (!validation.valid) {
            return RedemptionResponse("failure", error = validation.error)
        }

        val customer = customerService.ensureCustomer(request.customer)

        val redemption = Redemption(
            voucher = voucher,
            customer = customer,
            amount = request.order?.amount,
            result = RedemptionResult.SUCCESS,
            status = RedemptionStatus.SUCCEEDED
        )
        val saved = redemptionRepository.save(redemption)

        val redemptionJson = voucher.redemptionJson
        if (redemptionJson != null) {
            val current = redemptionJson.redeemed_quantity ?: 0
            voucher.redemptionJson = redemptionJson.copy(redeemed_quantity = current + 1)
            voucherRepository.save(voucher)
        }

        return RedemptionResponse("success", redemptionId = saved.id)
    }

    private fun calculateDiscountAmount(voucher: Voucher, orderAmount: Long?, orderUnits: Int?): Long? {
        val discount = voucher.discountJson ?: return null
        if (orderAmount == null) return null
        return when (discount.type) {
            DiscountType.PERCENT -> discount.percent_off?.let { (orderAmount * it) / 100 }
            DiscountType.AMOUNT, DiscountType.FIXED -> discount.amount_off
            DiscountType.UNIT -> {
                val perUnit = discount.amount_off ?: return null
                val units = orderUnits ?: voucher.redemptionJson?.quantity ?: 0
                perUnit * units
            }
        }
    }

    fun toVoucherResponse(voucher: Voucher): VoucherResponse =
        VoucherResponse(
            id = voucher.id,
            objectType = "voucher",
            code = voucher.code,
            type = voucher.type,
            status = if (voucher.active == true) "ACTIVE" else "INACTIVE",
            discount = voucher.discountJson,
            gift = voucher.giftJson,
            loyalty_card = voucher.loyaltyCardJson,
            redemption = voucher.redemptionJson,
            additional_info = voucher.additionalInfo,
            start_date = voucher.startDate,
            expiration_date = voucher.expirationDate,
            validity_timeframe = voucher.validityTimeframe,
            validity_day_of_week = voucher.validityDayOfWeek,
            validity_hours = voucher.validityHours,
            metadata = voucher.metadata,
            assets = VoucherAssetsDto(
                qr = AssetDto(id = voucher.assets?.qrId, url = voucher.assets?.qrUrl),
                barcode = AssetDto(id = voucher.assets?.barcodeId, url = voucher.assets?.barcodeUrl)
            ),
            categories = voucher.categories.map { CategoryResponse(it.id, it.name, it.createdAt, it.updatedAt) },
            campaign_id = voucher.campaign?.id,
            created_at = voucher.createdAt,
            updated_at = voucher.updatedAt
        )

    private fun generateAssetsIfMissing(voucher: Voucher) {
        val code = voucher.code ?: return
        val assets = voucher.assets ?: org.wahlen.voucherengine.persistence.model.voucher.VoucherAssetsEmbeddable()
        if (assets.qrId == null) {
            assets.qrId = "qr_$code"
            assets.qrUrl = "/v1/vouchers/$code/qr"
        }
        if (assets.barcodeId == null) {
            assets.barcodeId = "bc_$code"
            assets.barcodeUrl = "/v1/vouchers/$code/barcode"
        }
        voucher.assets = assets
    }

    private fun attachCategories(voucher: Voucher, categoryIds: List<UUID>?) {
        if (categoryIds == null) return
        val categories = categoryRepository.findAllById(categoryIds)
        if (categories.size != categoryIds.size) {
            throw org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "One or more categories not found"
            )
        }
        voucher.categories.clear()
        voucher.categories.addAll(categories)
    }

    private fun applyValidationRules(voucher: Voucher, customer: Customer?, customerRedemptions: Int, totalRedemptions: Int, request: VoucherValidationRequest): ValidationResponse? {
        val assignments = mutableListOf<org.wahlen.voucherengine.persistence.model.validation.ValidationRulesAssignment>()
        voucher.id?.let { id -> assignments += validationRulesAssignmentRepository.findByRelatedObjectIdAndRelatedObjectType(id.toString(), "voucher") }
        voucher.code?.let { code -> assignments += validationRulesAssignmentRepository.findByRelatedObjectIdAndRelatedObjectType(code, "voucher") }
        if (assignments.isEmpty()) return null

        assignments.forEach { assignment ->
            val rule = assignment.rule ?: assignment.ruleId?.let { validationRuleRepository.findById(it).orElse(null) }
                ?: return@forEach
            val rulesMap = rule.rules ?: return@forEach
            val ok = RuleEvaluator.evaluate(
                rulesMap,
                RuleEvaluator.Context(
                    voucher = voucher,
                    customer = customer ?: customerService.ensureCustomer(request.customer),
                    request = request,
                    totalRedemptions = totalRedemptions,
                    perCustomerRedemptions = customerRedemptions
                )
            )
            if (!ok) {
                return ValidationResponse(false, error = buildRuleError(rule, "rule_failed", "Validation rule not satisfied."))
            }
        }
        return null
    }

    private fun buildRuleError(rule: org.wahlen.voucherengine.persistence.model.validation.ValidationRule, defaultCode: String, defaultMessage: String): ErrorResponse {
        val errorMap = rule.error
        val code = (errorMap?.get("code") as? String) ?: defaultCode
        val message = (errorMap?.get("message") as? String) ?: defaultMessage
        return ErrorResponse(code, message)
    }

    private fun isWithinValidityWindow(voucher: Voucher, now: Instant): Boolean {
        val zoned = now.atZone(ZoneOffset.UTC)

        voucher.validityDayOfWeek?.let { days ->
            if (days.isNotEmpty()) {
                val dow = zoned.dayOfWeek.value % 7 // Sunday -> 0
                if (!days.contains(dow)) return false
            }
        }

        voucher.validityHours?.daily?.let { slots ->
            if (slots.isNotEmpty()) {
                val currentTime = zoned.toLocalTime()
                val matches = slots.any { slot ->
                    val start = slot.start_time?.let { LocalTime.parse(it) } ?: return@any false
                    val end = slot.expiration_time?.let { LocalTime.parse(it) } ?: return@any false
                    val dayAllowed = slot.days_of_week?.let { it.contains(zoned.dayOfWeek.value % 7) } ?: true
                    dayAllowed && !currentTime.isBefore(start) && currentTime.isBefore(end)
                }
                if (!matches) return false
            }
        }

        voucher.validityTimeframe?.let { timeframe ->
            val start = voucher.startDate ?: return false
            val duration = timeframe.duration?.let { Duration.parse(it) } ?: return false
            val interval = timeframe.interval?.let { Duration.parse(it) } ?: return false
            val elapsed = Duration.between(start, now)
            if (elapsed.isNegative) return false
            val cycles = elapsed.dividedBy(interval)
            val cycleRemainder = elapsed.minus(interval.multipliedBy(cycles))
            if (cycleRemainder >= duration) return false
        }
        return true
    }
}
