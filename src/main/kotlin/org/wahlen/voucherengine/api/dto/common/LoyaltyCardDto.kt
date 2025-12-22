package org.wahlen.voucherengine.api.dto.common

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

/**
 * Loyalty card payload as stored in jsonb.
 */
data class LoyaltyCardDto(
    @field:Schema(description = "Total loyalty points assigned", example = "1200")
    var points: Long? = null,
    @field:Schema(description = "Current balance after redemptions", example = "900")
    var balance: Long? = null,
    @field:Schema(description = "When the next batch of points expires", example = "2025-12-31T23:59:59Z")
    var next_expiration_date: Instant? = null,
    @field:Schema(description = "Points expiring at next_expiration_date", example = "200")
    var next_expiration_points: Long? = null,
    @field:Schema(description = "Points pending confirmation", example = "50")
    var pending_points: Long? = null,
    @field:Schema(description = "Expired points total", example = "30")
    var expired_points: Long? = null,
    @field:Schema(description = "Points already spent/subtracted", example = "300")
    var subtracted_points: Long? = null,
)
