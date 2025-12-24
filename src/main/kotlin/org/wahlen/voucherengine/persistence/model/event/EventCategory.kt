package org.wahlen.voucherengine.persistence.model.event

/**
 * Category of customer event - distinguishes customer actions from system effects.
 */
enum class EventCategory {
    /** Customer-initiated action (e.g., redemption, validation) */
    ACTION,
    
    /** System-generated effect (e.g., points added, tier upgraded) */
    EFFECT
}
