package org.wahlen.voucherengine.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
import org.wahlen.voucherengine.persistence.repository.RedemptionRollbackRepository
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
import org.springframework.web.server.ResponseStatusException
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
    private val redemptionRollbackRepository: RedemptionRollbackRepository,
    private val publicationRepository: org.wahlen.voucherengine.persistence.repository.PublicationRepository,
    private val productRepository: org.wahlen.voucherengine.persistence.repository.ProductRepository,
    private val skuRepository: org.wahlen.voucherengine.persistence.repository.SkuRepository,
    private val tenantService: TenantService,
    private val voucherTransactionRepository: org.wahlen.voucherengine.persistence.repository.VoucherTransactionRepository,
    private val clock: Clock,
) {

    @Transactional
    fun createVoucher(tenantName: String, request: VoucherCreateRequest): Voucher {
        val tenant = tenantService.requireTenant(tenantName)
        val holder = customerService.ensureCustomer(tenantName, request.customer)
        val campaign = request.campaign_id?.let { campaignRepository.findByIdAndTenantName(it, tenantName) }
        validateValidity(request)
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
        voucher.tenant = tenant
        voucher.validityTimeframe = request.validity_timeframe
        voucher.validityDayOfWeek = request.validity_day_of_week
        voucher.validityHours = request.validity_hours
        voucher.redemptionJson = RedemptionDto(
            quantity = request.redemption?.quantity,
            redeemed_quantity = 0,
            per_customer = request.redemption?.per_customer
        )
        generateAssetsIfMissing(voucher)
        attachCategories(voucher, request.category_ids, tenantName)
        return voucherRepository.save(voucher)
    }

    @Transactional
    fun updateVoucher(tenantName: String, code: String, request: VoucherCreateRequest): Voucher? {
        val existing = voucherRepository.findByCodeAndTenantName(code, tenantName) ?: return null
        validateValidity(request)
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
        val holder = customerService.ensureCustomer(tenantName, request.customer)
        if (holder != null) {
            existing.holder = holder
        }
        existing.campaign = request.campaign_id?.let { campaignRepository.findByIdAndTenantName(it, tenantName) } ?: existing.campaign
        attachCategories(existing, request.category_ids, tenantName)
        return voucherRepository.save(existing)
    }

    @Transactional
    fun setVoucherActive(tenantName: String, code: String, active: Boolean): Voucher? {
        val existing = voucherRepository.findByCodeAndTenantName(code, tenantName) ?: return null
        existing.active = active
        return voucherRepository.save(existing)
    }

    @Transactional(readOnly = true)
    fun getByCode(tenantName: String, code: String): Voucher? = voucherRepository.findByCodeAndTenantName(code, tenantName)

    @Transactional(readOnly = true)
    fun validateVoucher(tenantName: String, code: String, request: VoucherValidationRequest): ValidationResponse =
        validateVoucherInternal(tenantName, code, request, skipPerCustomerLimit = false, allowedRulePrefixes = null)

    @Transactional(readOnly = true)
    fun validateVoucherForQualification(
        tenantName: String,
        code: String,
        request: VoucherValidationRequest,
        allowedRulePrefixes: Set<String>? = null
    ): ValidationResponse =
        validateVoucherInternal(tenantName, code, request, skipPerCustomerLimit = true, allowedRulePrefixes = allowedRulePrefixes)

    private fun validateVoucherInternal(
        tenantName: String,
        code: String,
        request: VoucherValidationRequest,
        skipPerCustomerLimit: Boolean,
        allowedRulePrefixes: Set<String>?
    ): ValidationResponse {
        val voucher = voucherRepository.findByCodeAndTenantName(code, tenantName)
            ?: return ValidationResponse(false, error = ErrorResponse("voucher_not_found", "Voucher does not exist."))

        val now = Instant.now(clock)
        ensureActiveWindow(voucher, now)?.let { return it }

        val customer = customerService.ensureCustomer(tenantName, request.customer)
        ensureOwnership(voucher, customer)?.let { return it }

        val totalRedemptions = voucher.id?.let { redemptionRepository.countByVoucherIdAndTenantName(it, tenantName).toInt() } ?: 0
        val perCustomerLimit = if (skipPerCustomerLimit) null else voucher.redemptionJson?.per_customer
        val quantityLimit = voucher.redemptionJson?.quantity

        ensureLimits(tenantName, perCustomerLimit, quantityLimit, totalRedemptions, voucher, customer)?.let { return it }

        val customerRedemptions = if (customer?.id != null && voucher.id != null) {
            redemptionRepository.countByVoucherIdAndCustomerIdAndTenantName(voucher.id!!, customer.id!!, tenantName).toInt()
        } else 0

        ensureOrderItemsExist(tenantName, request.order)?.let { return it }
        applyValidationRules(
            tenantName,
            voucher,
            customer,
            customerRedemptions,
            totalRedemptions,
            request,
            allowedRulePrefixes
        )?.let { return it }

        ensureCategories(voucher, request.categories)?.let { return it }

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
    fun redeem(tenantName: String, request: RedemptionRequest): RedemptionResponse {
        if (request.redeemables.isEmpty()) {
            return RedemptionResponse("failure", error = ErrorResponse("invalid_request", "No redeemables provided"))
        }
        val redeemable = request.redeemables.first()
        if (redeemable.`object` != "voucher") {
            return RedemptionResponse("failure", error = ErrorResponse("unsupported_redeemable", "Only vouchers are supported"))
        }
        val result = redeemSingle(
            tenantName,
            redeemable.id,
            request.customer,
            request.order,
            request.tracking_id,
            request.metadata
        )
        return if (result.error != null) {
            RedemptionResponse("failure", error = result.error)
        } else {
            RedemptionResponse("success", redemptionId = result.redemption?.id)
        }
    }

    @Transactional
    fun redeemSingle(
        tenantName: String,
        code: String,
        customerRef: CustomerReferenceDto?,
        order: OrderRequest?,
        trackingId: String?,
        metadata: Map<String, Any?>?
    ): RedeemSingleResult {
        val voucher = voucherRepository.findByCodeAndTenantName(code, tenantName)
            ?: return RedeemSingleResult(error = ErrorResponse("voucher_not_found", "Voucher does not exist."))

        val validation = validateVoucher(
            tenantName,
            voucher.code ?: code,
            VoucherValidationRequest(customer = customerRef, order = order, metadata = metadata)
        )
        if (!validation.valid) {
            return RedeemSingleResult(error = validation.error ?: ErrorResponse("invalid_request", "Validation failed."))
        }

        val tenant = tenantService.requireTenant(tenantName)
        val customer = customerService.ensureCustomer(tenantName, customerRef)
        val hashedTrackingId = trackingId?.let(::hashTrackingId) ?: customerRef?.source_id?.let(::hashTrackingId)

        val redemption = Redemption(
            trackingId = hashedTrackingId,
            metadata = metadata,
            voucher = voucher,
            customer = customer,
            amount = order?.amount,
            result = RedemptionResult.SUCCESS,
            status = RedemptionStatus.SUCCEEDED
        )
        redemption.tenant = tenant
        val saved = redemptionRepository.save(redemption)

        val redemptionJson = voucher.redemptionJson
        if (redemptionJson != null) {
            val current = redemptionJson.redeemed_quantity ?: 0
            voucher.redemptionJson = redemptionJson.copy(redeemed_quantity = current + 1)
            voucherRepository.save(voucher)
        }
        return RedeemSingleResult(redemption = saved)
    }

    data class RedeemSingleResult(
        val redemption: Redemption? = null,
        val error: ErrorResponse? = null
    )

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

    @Transactional(readOnly = true)
    fun listVouchers(tenantName: String, pageable: Pageable): Page<Voucher> =
        voucherRepository.findAllByTenantName(tenantName, pageable)

    @Transactional(readOnly = true)
    fun listVouchersByCampaign(tenantName: String, campaignId: UUID): List<Voucher> =
        voucherRepository.findAllByCampaignIdAndTenantName(campaignId, tenantName)

    @Transactional
    fun deleteVoucher(tenantName: String, code: String): Boolean {
        val existing = voucherRepository.findByCodeAndTenantName(code, tenantName) ?: return false
        val voucherId = existing.id
        val voucherCode = existing.code

        if (voucherId != null) {
            val publications = publicationRepository.findAllByTenantNameAndVoucherId(tenantName, voucherId)
            if (publications.isNotEmpty()) {
                publicationRepository.deleteAll(publications)
            }
            val redemptions = redemptionRepository.findAllByTenantNameAndVoucherId(tenantName, voucherId)
            if (redemptions.isNotEmpty()) {
                redemptionRepository.deleteAll(redemptions)
            }
            val idAssignments = validationRulesAssignmentRepository
                .findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(
                    tenantName,
                    "voucher",
                    listOf(voucherId.toString())
                )
            if (idAssignments.isNotEmpty()) {
                validationRulesAssignmentRepository.deleteAll(idAssignments)
            }
        }
        if (!voucherCode.isNullOrBlank()) {
            val codeAssignments = validationRulesAssignmentRepository
                .findAllByTenantNameAndRelatedObjectTypeAndRelatedObjectIdIn(
                    tenantName,
                    "voucher",
                    listOf(voucherCode)
                )
            if (codeAssignments.isNotEmpty()) {
                validationRulesAssignmentRepository.deleteAll(codeAssignments)
            }
        }
        voucherRepository.delete(existing)
        return true
    }

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

    private fun attachCategories(voucher: Voucher, categoryIds: List<UUID>?, tenantName: String) {
        if (categoryIds == null) return
        val categories = categoryRepository.findAllByIdInAndTenantName(categoryIds, tenantName)
        if (categories.size != categoryIds.size) {
            throw org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "One or more categories not found"
            )
        }
        voucher.categories.clear()
        voucher.categories.addAll(categories)
    }

    private fun applyValidationRules(
        tenantName: String,
        voucher: Voucher,
        customer: Customer?,
        customerRedemptions: Int,
        totalRedemptions: Int,
        request: VoucherValidationRequest,
        allowedRulePrefixes: Set<String>?
    ): ValidationResponse? {
        val assignments = mutableListOf<org.wahlen.voucherengine.persistence.model.validation.ValidationRulesAssignment>()
        voucher.id?.let { id ->
            assignments += validationRulesAssignmentRepository.findByRelatedObjectIdAndRelatedObjectTypeAndTenantName(id.toString(), "voucher", tenantName)
        }
        voucher.code?.let { code ->
            assignments += validationRulesAssignmentRepository.findByRelatedObjectIdAndRelatedObjectTypeAndTenantName(code, "voucher", tenantName)
        }
        if (assignments.isEmpty()) return null

        assignments.forEach { assignment ->
            val rule = assignment.rule ?: assignment.ruleId?.let { validationRuleRepository.findByIdAndTenantName(it, tenantName) }
                ?: return@forEach
            val rulesMap = rule.rules ?: return@forEach
            val ok = RuleEvaluator.evaluate(
                rulesMap,
                RuleEvaluator.Context(
                    voucher = voucher,
                    customer = customer ?: customerService.ensureCustomer(tenantName, request.customer),
                    request = request,
                    totalRedemptions = totalRedemptions,
                    perCustomerRedemptions = customerRedemptions
                ),
                allowedRulePrefixes
            )
            if (!ok) {
                return ValidationResponse(
                    valid = false,
                    error = buildRuleError(rule, "rule_failed", "Validation rule not satisfied."),
                    validationRuleId = rule.id?.toString()
                )
            }
        }
        return null
    }

    private fun validateValidity(request: VoucherCreateRequest) {
        request.validity_timeframe?.let { tf ->
            val duration = tf.duration?.let { Duration.parse(it) } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_timeframe.duration is required and must be ISO-8601 duration")
            val interval = tf.interval?.let { Duration.parse(it) } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_timeframe.interval is required and must be ISO-8601 duration")
            if (duration.isZero || interval.isZero || duration.isNegative || interval.isNegative) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_timeframe duration and interval must be positive")
            }
            if (duration > interval) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_timeframe duration must be <= interval")
            }
            if (request.start_date == null) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "start_date is required when using validity_timeframe")
            }
        }

        request.validity_day_of_week?.let { days ->
            if (days.any { it !in 0..6 }) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_day_of_week must contain values 0-6")
            }
        }

        request.validity_hours?.daily?.let { slots ->
            slots.forEach { slot ->
                val start = slot.start_time?.let { LocalTime.parse(it) }
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_hours.daily.start_time must be HH:mm")
                val end = slot.expiration_time?.let { LocalTime.parse(it) }
                    ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_hours.daily.expiration_time must be HH:mm")
                if (!start.isBefore(end)) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_hours daily start_time must be before expiration_time")
                }
                slot.days_of_week?.let { days ->
                    if (days.any { it !in 0..6 }) {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_hours days_of_week must be 0-6")
                    }
                }
            }
            // overlap check per day
            val byDay = mutableMapOf<Int, MutableList<Pair<LocalTime, LocalTime>>>()
            slots.forEach { slot ->
                val start = LocalTime.parse(slot.start_time)
                val end = LocalTime.parse(slot.expiration_time)
                val days = slot.days_of_week ?: listOf(0,1,2,3,4,5,6)
                days.forEach { day ->
                    val list = byDay.computeIfAbsent(day) { mutableListOf() }
                    list.add(start to end)
                }
            }
            byDay.values.forEach { intervals ->
                val sorted = intervals.sortedBy { it.first }
                for (i in 1 until sorted.size) {
                    val prev = sorted[i-1]
                    val curr = sorted[i]
                    if (!curr.first.isAfter(prev.second)) {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "validity_hours intervals overlap for a day")
                    }
                }
            }
        }
    }

    private fun buildRuleError(rule: org.wahlen.voucherengine.persistence.model.validation.ValidationRule, defaultCode: String, defaultMessage: String): ErrorResponse {
        val errorMap = rule.error
        val code = (errorMap?.get("code") as? String) ?: defaultCode
        val message = (errorMap?.get("message") as? String) ?: defaultMessage
        return ErrorResponse(code, message)
    }

    private fun hashTrackingId(source: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(source.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun ensureActiveWindow(voucher: Voucher, now: Instant): ValidationResponse? {
        if (voucher.active == false) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is inactive."))
        }
        if (voucher.startDate != null && now.isBefore(voucher.startDate)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is not yet active."))
        }
        if (voucher.expirationDate != null && now.isAfter(voucher.expirationDate)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_expired", "This voucher has expired."))
        }
        if (!isWithinValidityWindow(voucher, now)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is not active right now."))
        }
        return null
    }

    private fun ensureOwnership(voucher: Voucher, customer: Customer?): ValidationResponse? {
        if (voucher.holder != null && voucher.holder?.id != customer?.id) {
            return ValidationResponse(false, error = ErrorResponse("voucher_not_assigned", "Voucher assigned to another customer."))
        }
        return null
    }

    private fun ensureOrderItemsExist(tenantName: String, order: org.wahlen.voucherengine.api.dto.request.OrderRequest?): ValidationResponse? {
        val items = order?.items ?: return null
        items.forEach { item ->
            val productId = item.product_id?.trim()
            if (!productId.isNullOrBlank()) {
                val product = findProductByIdOrSource(tenantName, productId)
                if (product == null) {
                    return ValidationResponse(false, error = ErrorResponse("product_not_found", "Product does not exist."))
                }
            }
            val skuId = item.sku_id?.trim()
            if (!skuId.isNullOrBlank()) {
                val sku = findSkuByIdOrSource(tenantName, skuId)
                if (sku == null) {
                    return ValidationResponse(false, error = ErrorResponse("sku_not_found", "SKU does not exist."))
                }
            }
        }
        return null
    }

    private fun findProductByIdOrSource(tenantName: String, idOrSource: String): org.wahlen.voucherengine.persistence.model.product.Product? {
        val uuid = runCatching { java.util.UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byId = productRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return productRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun findSkuByIdOrSource(tenantName: String, idOrSource: String): org.wahlen.voucherengine.persistence.model.product.Sku? {
        val uuid = runCatching { java.util.UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byId = skuRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return skuRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun ensureLimits(
        tenantName: String,
        perCustomerLimit: Int?,
        quantityLimit: Int?,
        totalRedemptions: Int,
        voucher: Voucher,
        customer: Customer?
    ): ValidationResponse? {
        if (quantityLimit != null && totalRedemptions >= quantityLimit) {
            return ValidationResponse(false, error = ErrorResponse("redemption_limit_exceeded", "This voucher reached its total redemption limit."))
        }
        if (perCustomerLimit != null && customer?.id != null && voucher.id != null) {
            val customerRedemptions = redemptionRepository.countByVoucherIdAndCustomerIdAndTenantName(voucher.id!!, customer.id!!, tenantName).toInt()
            if (customerRedemptions >= perCustomerLimit) {
                val message = if (perCustomerLimit == 1) {
                    "This voucher can be redeemed only once per customer."
                } else {
                    "This voucher can be redeemed only $perCustomerLimit time(s) per customer."
                }
                return ValidationResponse(false, error = ErrorResponse("redemption_limit_per_customer_exceeded", message))
            }
        }
        return null
    }

    @Transactional
    fun rollbackRedemption(
        tenantName: String,
        redemptionId: UUID,
        request: org.wahlen.voucherengine.api.dto.request.RollbackRequest
    ): org.wahlen.voucherengine.persistence.model.redemption.RedemptionRollback? {
        val tenant = tenantService.requireTenant(tenantName)
        val redemption = redemptionRepository.findByIdAndTenantName(redemptionId, tenantName) ?: return null
        val rollback = org.wahlen.voucherengine.persistence.model.redemption.RedemptionRollback(
            date = Instant.now(clock),
            trackingId = request.reason,
            metadata = request.metadata,
            amount = request.amount,
            result = RedemptionResult.SUCCESS,
            reason = request.reason,
            redemption = redemption,
            customer = redemption.customer
        )
        rollback.tenant = tenant
        return redemptionRollbackRepository.save(rollback)
    }

    private fun ensureCategories(voucher: Voucher, categoryIds: List<UUID>?): ValidationResponse? {
        if (voucher.categories.isNotEmpty()) {
            if (categoryIds.isNullOrEmpty()) {
                return ValidationResponse(false, error = ErrorResponse("voucher_category_mismatch", "Voucher is not applicable for provided categories."))
            }
            val overlap = voucher.categories.any { it.id != null && categoryIds.contains(it.id) }
            if (!overlap) {
                return ValidationResponse(false, error = ErrorResponse("voucher_category_mismatch", "Voucher is not applicable for provided categories."))
            }
        }
        return null
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

    @Transactional
    fun adjustVoucherBalance(
        tenantName: String,
        code: String,
        request: VoucherBalanceUpdateRequest
    ): org.wahlen.voucherengine.api.dto.response.VoucherBalanceUpdateResponse? {
        val tenant = tenantService.requireTenant(tenantName)
        val voucher = getByCode(tenantName, code) ?: return null
        val amount = request.amount ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "amount is required")

        when (voucher.type) {
            VoucherType.GIFT_VOUCHER -> {
                val gift = voucher.giftJson ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher has no gift configuration")
                val currentBalance = gift.balance ?: gift.amount ?: 0L
                val newBalance = currentBalance + amount
                if (newBalance < 0) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance")
                }
                gift.balance = newBalance
                voucher.giftJson = gift
                voucherRepository.save(voucher)

                val transaction = org.wahlen.voucherengine.persistence.model.voucher.VoucherTransaction(
                    sourceId = request.source_id,
                    voucher = voucher,
                    campaignId = voucher.campaign?.id,
                    type = if (amount > 0) org.wahlen.voucherengine.persistence.model.voucher.VoucherTransactionType.CREDITS_ADDITION
                    else org.wahlen.voucherengine.persistence.model.voucher.VoucherTransactionType.CREDITS_REMOVAL,
                    source = "API",
                    reason = request.reason,
                    amount = amount,
                    balanceAfter = newBalance
                )
                transaction.tenant = tenant
                voucherTransactionRepository.save(transaction)

                return org.wahlen.voucherengine.api.dto.response.VoucherBalanceUpdateResponse(
                    amount = amount,
                    total = newBalance,
                    balance = newBalance,
                    type = "gift_voucher",
                    related_object = org.wahlen.voucherengine.api.dto.response.RelatedObjectDto(
                        type = "voucher",
                        id = voucher.id
                    )
                )
            }
            VoucherType.LOYALTY_CARD -> {
                val loyalty = voucher.loyaltyCardJson ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Voucher has no loyalty card configuration")
                val currentBalance = loyalty.balance ?: loyalty.points ?: 0L
                val newBalance = currentBalance + amount
                if (newBalance < 0) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance")
                }
                loyalty.balance = newBalance
                voucher.loyaltyCardJson = loyalty
                voucherRepository.save(voucher)

                val transaction = org.wahlen.voucherengine.persistence.model.voucher.VoucherTransaction(
                    sourceId = request.source_id,
                    voucher = voucher,
                    campaignId = voucher.campaign?.id,
                    type = if (amount > 0) org.wahlen.voucherengine.persistence.model.voucher.VoucherTransactionType.POINTS_ADDITION
                    else org.wahlen.voucherengine.persistence.model.voucher.VoucherTransactionType.POINTS_REMOVAL,
                    source = "API",
                    reason = request.reason,
                    amount = amount,
                    balanceAfter = newBalance
                )
                transaction.tenant = tenant
                voucherTransactionRepository.save(transaction)

                return org.wahlen.voucherengine.api.dto.response.VoucherBalanceUpdateResponse(
                    amount = amount,
                    total = newBalance,
                    balance = newBalance,
                    type = "loyalty_card",
                    related_object = org.wahlen.voucherengine.api.dto.response.RelatedObjectDto(
                        type = "voucher",
                        id = voucher.id
                    )
                )
            }
            else -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance adjustments only supported for gift vouchers and loyalty cards")
        }
    }

    @Transactional(readOnly = true)
    fun listVoucherTransactions(
        tenantName: String,
        code: String,
        pageable: Pageable
    ): Page<org.wahlen.voucherengine.persistence.model.voucher.VoucherTransaction>? {
        val voucher = getByCode(tenantName, code) ?: return null
        return voucherTransactionRepository.findAllByVoucher_IdAndTenant_Name(voucher.id!!, tenantName, pageable)
    }

    @Transactional(readOnly = true)
    fun listVoucherRedemptions(
        tenantName: String,
        code: String
    ): org.wahlen.voucherengine.api.dto.response.VoucherRedemptionsResponse? {
        val voucher = getByCode(tenantName, code) ?: return null
        val redemptions = redemptionRepository.findAllByTenantNameAndVoucherId(tenantName, voucher.id!!)
        
        val redemptionEntries = redemptions.map { redemption ->
            org.wahlen.voucherengine.api.dto.response.RedemptionEntryResponse(
                id = redemption.id,
                date = redemption.createdAt,
                customer_id = redemption.customer?.id,
                tracking_id = redemption.trackingId,
                metadata = redemption.metadata,
                result = redemption.result?.name,
                status = redemption.status?.name,
                amount = redemption.amount,
                voucher = org.wahlen.voucherengine.api.dto.response.VoucherReferenceDto(
                    id = voucher.id,
                    code = voucher.code,
                    campaign_id = voucher.campaign?.id
                )
            )
        }

        return org.wahlen.voucherengine.api.dto.response.VoucherRedemptionsResponse(
            quantity = voucher.redemptionJson?.quantity,
            redeemed_quantity = redemptions.size,
            url = "/v1/vouchers/$code/redemption",
            total = redemptions.size,
            redemption_entries = redemptionEntries
        )
    }

    @Transactional
    fun bulkUpdateVoucherMetadata(
        tenantName: String,
        updates: List<org.wahlen.voucherengine.api.dto.request.VoucherBulkUpdateRequest>
    ): org.wahlen.voucherengine.api.dto.response.BulkOperationResponse {
        var successCount = 0
        val failedCodes = mutableListOf<String>()

        updates.forEach { update ->
            val voucher = getByCode(tenantName, update.code)
            if (voucher != null) {
                voucher.metadata = update.metadata
                voucherRepository.save(voucher)
                successCount++
            } else {
                failedCodes.add(update.code)
            }
        }

        return org.wahlen.voucherengine.api.dto.response.BulkOperationResponse(
            success_count = successCount,
            failure_count = failedCodes.size,
            failed_codes = failedCodes
        )
    }

    @Transactional
    fun updateMetadataAsync(
        tenantName: String,
        request: org.wahlen.voucherengine.api.dto.request.VoucherMetadataUpdateRequest
    ): org.wahlen.voucherengine.api.dto.response.BulkOperationResponse {
        var successCount = 0
        val failedCodes = mutableListOf<String>()

        request.codes.forEach { code ->
            val voucher = getByCode(tenantName, code)
            if (voucher != null) {
                val currentMetadata = voucher.metadata?.toMutableMap() ?: mutableMapOf()
                currentMetadata.putAll(request.metadata)
                voucher.metadata = currentMetadata
                voucherRepository.save(voucher)
                successCount++
            } else {
                failedCodes.add(code)
            }
        }

        return org.wahlen.voucherengine.api.dto.response.BulkOperationResponse(
            success_count = successCount,
            failure_count = failedCodes.size,
            failed_codes = failedCodes
        )
    }

    @Transactional
    fun importVouchers(
        tenantName: String,
        request: org.wahlen.voucherengine.api.dto.request.VoucherImportRequest
    ): org.wahlen.voucherengine.api.dto.response.BulkOperationResponse {
        var successCount = 0
        val failedCodes = mutableListOf<String>()

        request.vouchers.forEach { voucherRequest ->
            try {
                createVoucher(tenantName, voucherRequest)
                successCount++
            } catch (e: Exception) {
                failedCodes.add(voucherRequest.code ?: "unknown")
            }
        }

        return org.wahlen.voucherengine.api.dto.response.BulkOperationResponse(
            success_count = successCount,
            failure_count = failedCodes.size,
            failed_codes = failedCodes
        )
    }

    fun toTransactionResponse(tx: org.wahlen.voucherengine.persistence.model.voucher.VoucherTransaction): org.wahlen.voucherengine.api.dto.response.VoucherTransactionResponse {
        val voucher = tx.voucher
        val balanceType = when (voucher?.type) {
            VoucherType.GIFT_VOUCHER -> "gift_voucher"
            VoucherType.LOYALTY_CARD -> "loyalty_card"
            else -> null
        }

        val details = org.wahlen.voucherengine.api.dto.response.VoucherTransactionDetailsDto(
            balance = if (tx.balanceAfter != null && balanceType != null) {
                org.wahlen.voucherengine.api.dto.response.VoucherBalanceDto(
                    type = balanceType,
                    total = tx.balanceAfter,
                    balance = tx.balanceAfter
                )
            } else null,
            order = tx.redemption?.order?.let {
                org.wahlen.voucherengine.api.dto.response.OrderReferenceDto(
                    id = it.id,
                    source_id = it.sourceId
                )
            },
            redemption = tx.redemption?.id?.let {
                org.wahlen.voucherengine.api.dto.response.RedemptionReferenceDto(id = it)
            },
            rollback = tx.rollback?.id?.let {
                org.wahlen.voucherengine.api.dto.response.RollbackReferenceDto(id = it)
            }
        )

        return org.wahlen.voucherengine.api.dto.response.VoucherTransactionResponse(
            id = tx.id,
            source_id = tx.sourceId,
            voucher_id = tx.voucher?.id,
            campaign_id = tx.campaignId,
            type = tx.type?.name,
            source = tx.source,
            reason = tx.reason,
            amount = tx.amount,
            created_at = tx.createdAt,
            details = details
        )
    }
}
