package org.wahlen.voucherengine.persistence.model.customer

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * Embeddable address information for customer profiles.
 *
 * Used to store structured address data that can be used for
 * validation rules, customer segmentation, or shipping information.
 *
 * Voucherengine API Docs: Customers.
 */
@Embeddable
class AddressEmbeddable(

    /**
     * City name.
     */
    @Column
    var city: String? = null,

    /**
     * State or province.
     */
    @Column(columnDefinition = "TEXT")
    var state: String? = null,

    /**
     * First line of the street address.
     */
    @Column
    var line_1: String? = null,

    /**
     * Second line of the street address (optional, for apartment numbers, etc.).
     */
    @Column
    var line_2: String? = null,

    /**
     * Country name or code.
     */
    @Column
    var country: String? = null,

    /**
     * Postal or ZIP code.
     */
    @Column(columnDefinition = "TEXT")
    var postalCode: String? = null
)
