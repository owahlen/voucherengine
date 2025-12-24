package org.wahlen.voucherengine.persistence.model.customer

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.tenant.Tenant
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import java.time.LocalDate

/**
 * Represents a Voucherengine customer profile used for personalization and eligibility checks.
 *
 * Customers can hold vouchers, be linked to orders and redemptions, and provide metadata
 * for validation rules such as segments, limits per customer, or audience targeting.
 *
 * Voucherengine API Docs: Customers.
 */
@Entity
class Customer(

    /**
     * A unique identifier of the customer who validates a voucher.
     * It can be a customer ID or email from a CRM system, database,
     * or a third-party service.
     * If you also pass a customer ID (unique ID assigned by Voucherengine), the source ID will be ignored.
     */
    @Column
    var sourceId: String? = null,

    /**
     * Customer's first and last name.
     */
    @Column
    var name: String? = null,

    /**
     * An arbitrary string that you can attach to a customer object.
     */
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    /**
     * Customer's email address.
     */
    @Column
    var email: String? = null,

    /**
     * Customer's phone number.
     * This parameter is mandatory when you try to send out codes to customers via an SMS channel.
     */
    @Column
    var phone: String? = null,

    /**
     * Customer's birthdate
     */
    @Column
    var birthdate: LocalDate? = null,

    /**
     * Customer's address.
     */
    @Column
    var address: AddressEmbeddable? = null,

    /**
     * A set of custom key/value pairs that you can attach to a customer.
     * The metadata object stores all custom attributes assigned to the customer.
     * It can be useful for storing additional information about the customer
     * in a structured format such as customer segmentation or preferences.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * The tenant this customer belongs to.
     * Used for multi-tenancy isolation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    var tenant: Tenant? = null,

    /**
     * Collection of vouchers held by this customer.
     * These are vouchers where this customer is set as the holder.
     */
    @OneToMany(mappedBy = "holder", fetch = FetchType.LAZY)
    var heldVouchers: MutableList<Voucher> = mutableListOf()

) : AuditablePersistable()
