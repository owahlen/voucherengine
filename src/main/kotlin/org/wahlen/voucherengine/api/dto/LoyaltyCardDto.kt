package org.wahlen.voucherengine.api.dto

import java.time.Instant

/**
 * Loyalty card payload as stored in jsonb.
 */
data class LoyaltyCardDto(
    val points: Long? = null,
    val balance: Long? = null,
    val next_expiration_date: Instant? = null,
    val next_expiration_points: Long? = null,
    val pending_points: Long? = null,
    val expired_points: Long? = null,
    val subtracted_points: Long? = null,
)
