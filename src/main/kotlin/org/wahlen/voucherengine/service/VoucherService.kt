package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.RedemptionDto
import org.wahlen.voucherengine.api.dto.request.*
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionStatus
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.repository.RedemptionRepository
import org.wahlen.voucherengine.persistence.repository.VoucherRepository
import org.wahlen.voucherengine.service.dto.ErrorResponse
import org.wahlen.voucherengine.service.dto.RedemptionResponse
import org.wahlen.voucherengine.service.dto.ValidationResponse
import java.time.Instant

@Service
class VoucherService(
    private val voucherRepository: VoucherRepository,
    private val redemptionRepository: RedemptionRepository,
    private val customerService: CustomerService,
) {

    @Transactional
    fun createVoucher(request: VoucherCreateRequest): Voucher {
        val holder = customerService.ensureCustomer(request.customer)
        val voucher = Voucher(
            code = request.code,
            type = request.type?.let { VoucherType.valueOf(it) },
            discountJson = request.discount,
            giftJson = request.gift,
            loyaltyCardJson = request.loyalty_card,
            metadata = request.metadata,
            active = request.active ?: true,
            holder = holder,
        )
        voucher.redemptionJson = RedemptionDto(
            quantity = request.redemption?.quantity,
            redeemed_quantity = 0,
            per_customer = request.redemption?.per_customer
        )
        return voucherRepository.save(voucher)
    }

    @Transactional(readOnly = true)
    fun validateVoucher(code: String, request: VoucherValidationRequest): ValidationResponse {
        val voucher = voucherRepository.findByCode(code)
            ?: return ValidationResponse(false, error = ErrorResponse("voucher_not_found", "Voucher does not exist."))

        if (voucher.active == false) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is inactive."))
        }
        val now = Instant.now()
        if (voucher.startDate != null && now.isBefore(voucher.startDate)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_inactive", "Voucher is not yet active."))
        }
        if (voucher.expirationDate != null && now.isAfter(voucher.expirationDate)) {
            return ValidationResponse(false, error = ErrorResponse("voucher_expired", "Voucher has expired."))
        }

        val customer = customerService.ensureCustomer(request.customer)
        if (voucher.holder != null && voucher.holder?.id != customer?.id) {
            return ValidationResponse(false, error = ErrorResponse("voucher_not_assigned", "Voucher assigned to another customer."))
        }

        val totalRedemptions = voucher.id?.let { redemptionRepository.countByVoucherId(it) } ?: 0
        val perCustomerLimit = voucher.redemptionJson?.per_customer
        val quantityLimit = voucher.redemptionJson?.quantity

        if (quantityLimit != null && totalRedemptions >= quantityLimit) {
            return ValidationResponse(false, error = ErrorResponse("redemption_limit_exceeded", "This voucher reached its total redemption limit."))
        }

        if (perCustomerLimit != null && customer?.id != null) {
            val customerRedemptions = redemptionRepository.countByVoucherIdAndCustomerId(voucher.id!!, customer.id!!)
            if (customerRedemptions >= perCustomerLimit) {
                return ValidationResponse(false, error = ErrorResponse("redemption_limit_per_customer_exceeded", "This voucher can be redeemed only $perCustomerLimit time(s) per customer."))
            }
        }

        return ValidationResponse(true, voucherCode = voucher.code)
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
}
