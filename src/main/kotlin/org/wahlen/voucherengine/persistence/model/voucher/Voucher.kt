package org.wahlen.voucherengine.persistence.model.voucher

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.GiftDto
import org.wahlen.voucherengine.api.dto.common.LoyaltyCardDto
import org.wahlen.voucherengine.api.dto.common.RedemptionDto
import org.wahlen.voucherengine.api.dto.common.ValidityHoursDto
import org.wahlen.voucherengine.api.dto.common.ValidityTimeframeDto
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.model.common.AuditablePersistable
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.redemption.Redemption
import java.time.Instant

/**
 * Represents a Voucherengine voucher code or loyalty card issued from a campaign.
 *
 * Vouchers carry the redeemable value (discount, gift, or loyalty balance), optional assets
 * like QR/barcodes, active dates, and metadata. They inherit constraints from their campaign
 * and are used during validation and redemption to calculate discounts and track usage.
 *
 * Voucherengine API Docs: Vouchers.
 */
@Entity
@Table
class Voucher(

    /**
     * A code that identifies a voucher.
     * Pattern can use all letters of the English alphabet, Arabic numerals, and special characters.
     */
    @Column(unique = true)
    var code: String? = null,

    /**
     * Identifies the voucher's parent campaign.
     * */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    var campaign: Campaign? = null,

    /**
     * Defines the type of voucher - discount, gift, or loyalty card.
     */
    @Enumerated(EnumType.STRING)
    @Column
    var type: VoucherType? = null,

    /**
     * Contains information about discount.
     * One of: Amount, Unit, Unit Multiple, Percent, Fixed
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var discountJson: DiscountDto? = null,

    /**
     * Contains information about gift.
     * Attributes amount, subtracted_amount, balance, effect
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var giftJson: GiftDto? = null,

    /**
     * Contains information about loyalty card.
     * Attributes: points, balance, next_expiration_date, next_expiration_points,
     * pending_points, expired_points, sutracted_points
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var loyaltyCardJson: LoyaltyCardDto? = null,

    /**
     * Activation timestamp defines when the code starts to be active in ISO 8601 format.
     * Voucher is inactive before this date.
     * Example: 2021-12-01T00:00:00.000Z
     */
    @Column
    var startDate: Instant? = null,

    /**
     * Expiration timestamp defines when the code expires in ISO 8601 format.
     * Voucher is inactive after this date.
     * Example: 2021-12-31T00:00:00.000Z
     */
    @Column
    var expirationDate: Instant? = null,

    /**
     * A flag to toggle the voucher on or off.
     * You can disable a voucher even though it’s within the active period
     * defined by the start_date and expiration_date.
     *   true indicates an active voucher
     *   false indicates an inactive voucher
     */
    @Column
    var active: Boolean? = null,

    /**
     * An optional field to keep any extra textual information about the code such as a code description and details.
     */
    @Column(columnDefinition = "TEXT")
    var additionalInfo: String? = null,

    /**
     * The metadata object stores all custom attributes assigned to the code.
     * A set of key/value pairs that you can attach to a voucher object.
     * It can be useful for storing additional information about the voucher in a structured format.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var metadata: Map<String, Any?>? = null,

    /**
     * Additional assets related to the voucher such as barcodes or QR codes.
     */
    @Embedded
    var assets: VoucherAssetsEmbeddable? = null,

    /**
     * Unique customer identifier of the redeemable holder. It equals to the customer assigned by Voucherify.
     * Example: cust_eWgXlBBiY6THFRJwX45Iakv4
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "holder_id")
    var holder: Customer? = null,

) : AuditablePersistable() {
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "voucher_categories",
        joinColumns = [JoinColumn(name = "voucher_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")]
    )
    var categories: MutableSet<Category> = mutableSetOf()

    @OneToMany(mappedBy = "voucher", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    var redemptions: MutableList<Redemption> = mutableListOf()

    /**
     * Redemption settings for the voucher (quantity and per-customer limits).
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var redemptionJson: RedemptionDto? = null

    /**
     * Recurring validity timeframe (interval/duration).
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var validityTimeframe: ValidityTimeframeDto? = null

    /**
     * Days of week when voucher is valid (0=Sun..6=Sat).
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var validityDayOfWeek: List<Int>? = null

    /**
     * Daily hours when voucher is valid.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var validityHours: ValidityHoursDto? = null
}
