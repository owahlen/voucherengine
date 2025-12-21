package org.wahlen.voucherengine.api.dto

import java.time.Instant

/**
 * Loyalty card payload as stored in jsonb.
 */
data class LoyaltyCardDto(
    var points: Long? = null,
    var balance: Long? = null,
    var next_expiration_date: Instant? = null,
    var next_expiration_points: Long? = null,
    var pending_points: Long? = null,
    var expired_points: Long? = null,
    var subtracted_points: Long? = null,
)
