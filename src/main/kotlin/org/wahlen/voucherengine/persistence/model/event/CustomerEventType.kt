package org.wahlen.voucherengine.persistence.model.event

/**
 * Standard customer event types following Voucherify naming conventions.
 * These constants ensure consistency across the system.
 */
object CustomerEventType {
    
    // Customer lifecycle events
    const val CUSTOMER_CREATED = "customer.created"
    const val CUSTOMER_UPDATED = "customer.updated"
    const val CUSTOMER_DELETED = "customer.deleted"
    
    // Validation events
    const val VALIDATION_SUCCEEDED = "customer.validation.succeeded"
    const val VALIDATION_FAILED = "customer.validation.failed"
    
    // Redemption events
    const val REDEMPTION_SUCCEEDED = "customer.redemption.succeeded"
    const val REDEMPTION_FAILED = "customer.redemption.failed"
    const val REDEMPTION_ROLLBACK_SUCCEEDED = "customer.redemption.rollback.succeeded"
    const val REDEMPTION_ROLLBACK_FAILED = "customer.redemption.rollback.failed"
    
    // Publication events
    const val PUBLICATION_SUCCEEDED = "customer.publication.succeeded"
    const val PUBLICATION_FAILED = "customer.publication.failed"
    
    // Order events
    const val ORDER_CREATED = "customer.order.created"
    const val ORDER_UPDATED = "customer.order.updated"
    const val ORDER_CANCELED = "customer.order.canceled"
    const val ORDER_FULFILLED = "customer.order.fulfilled"
    const val ORDER_PAID = "customer.order.paid"
    
    // Reward events (future)
    const val REWARDED = "customer.rewarded"
    const val REWARDED_LOYALTY_POINTS = "customer.rewarded.loyalty_points"
    
    // Segment events (future)
    const val SEGMENT_ENTERED = "customer.segment.entered"
    const val SEGMENT_LEFT = "customer.segment.left"
    
    // Gift voucher events (future)
    const val VOUCHER_GIFT_BALANCE_ADDED = "customer.voucher.gift.balance_added"
    
    // Loyalty card events (future)
    const val LOYALTY_POINTS_ADDED = "customer.voucher.loyalty_card.points_added"
    const val LOYALTY_POINTS_EXPIRED = "customer.voucher.loyalty_card.points_expired"
    const val LOYALTY_TIER_UPGRADED = "customer.loyalty.tier.upgraded"
    const val LOYALTY_TIER_DOWNGRADED = "customer.loyalty.tier.downgraded"
}
