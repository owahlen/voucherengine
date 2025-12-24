package org.wahlen.voucherengine.persistence.model.segment

enum class SegmentType {
    /**
     * Customers automatically enter/leave based on filter.
     * Triggers customer.segment.entered/left events.
     */
    AUTO_UPDATE,

    /**
     * Customers enter/leave based on filter.
     * No events triggered.
     */
    PASSIVE,

    /**
     * Manually assigned customer IDs.
     */
    STATIC
}