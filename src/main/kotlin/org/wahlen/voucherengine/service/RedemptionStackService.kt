package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.wahlen.voucherengine.api.dto.request.RedemptionRequest
import org.wahlen.voucherengine.api.dto.response.RedemptionItemResponse
import org.wahlen.voucherengine.api.dto.response.RedemptionsRedeemResponse
import org.wahlen.voucherengine.api.dto.response.ValidationRedeemableResponse
import org.wahlen.voucherengine.persistence.model.redemption.Redemption

@Service
class RedemptionStackService(
    private val validationStackService: ValidationStackService,
    private val voucherService: VoucherService,
    private val sessionLockService: SessionLockService
) {

    fun redeem(tenantName: String, request: RedemptionRequest): RedemptionsRedeemResponse {
        val validationRequest = org.wahlen.voucherengine.api.dto.request.ValidationStackRequest(
            redeemables = request.redeemables,
            customer = request.customer,
            order = request.order,
            tracking_id = request.tracking_id,
            metadata = request.metadata,
            session = request.session,
            options = null
        )
        val validation = validationStackService.validate(tenantName, validationRequest)
        val applicationMode = validation.stacking_rules?.redeemables_application_mode ?: "ALL"

        if (applicationMode == "ALL" &&
            (!validation.inapplicable_redeemables.isNullOrEmpty() || !validation.skipped_redeemables.isNullOrEmpty())
        ) {
            return RedemptionsRedeemResponse(
                redemptions = emptyList(),
                order = validation.order,
                inapplicable_redeemables = validation.inapplicable_redeemables,
                skipped_redeemables = validation.skipped_redeemables
            )
        }

        val redemptions = mutableListOf<RedemptionItemResponse>()
        val sessionKey = request.session?.key
        validation.redeemables.orEmpty().forEach { redeemable ->
            if (redeemable.status != "APPLICABLE") return@forEach
            val redeemed = voucherService.redeemSingle(
                tenantName,
                redeemable.id ?: return@forEach,
                request.customer,
                request.order,
                request.tracking_id,
                request.metadata
            )
            redeemed.redemption?.let { redemptions += toResponse(it, sessionKey) }
        }

        if (sessionKey != null && redemptions.isNotEmpty()) {
            sessionLockService.clearLocks(tenantName, sessionKey)
        }

        return RedemptionsRedeemResponse(
            redemptions = redemptions,
            order = validation.order,
            inapplicable_redeemables = validation.inapplicable_redeemables,
            skipped_redeemables = validation.skipped_redeemables
        )
    }

    private fun toResponse(redemption: Redemption, sessionKey: String?): RedemptionItemResponse =
        RedemptionItemResponse(
            id = redemption.id,
            date = redemption.createdAt,
            customer_id = redemption.customerId,
            tracking_id = redemption.trackingId,
            metadata = redemption.metadata,
            amount = redemption.amount,
            result = redemption.result?.name,
            status = redemption.status?.name,
            failure_code = if (redemption.result == org.wahlen.voucherengine.persistence.model.redemption.RedemptionResult.FAILURE) "redemption_failed" else null,
            failure_message = redemption.reason,
            session = sessionKey?.let { org.wahlen.voucherengine.api.dto.common.SessionDto(key = it) }
        )
}
