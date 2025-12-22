package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.wahlen.voucherengine.persistence.model.publication.Publication
import org.wahlen.voucherengine.persistence.model.publication.PublicationResult
import java.util.UUID

interface PublicationRepository : JpaRepository<Publication, UUID> {
    fun findAllByTenantName(tenantName: String): List<Publication>
    fun findAllByTenantNameAndCustomerId(tenantName: String, customerId: UUID): List<Publication>
    fun findAllByTenantNameAndCampaignName(tenantName: String, campaignName: String): List<Publication>
    fun findAllByTenantNameAndResult(tenantName: String, result: PublicationResult): List<Publication>
    fun findAllByTenantNameAndSourceId(tenantName: String, sourceId: String): List<Publication>
    fun findAllByTenantNameAndCampaignId(tenantName: String, campaignId: UUID): List<Publication>
    fun findByTenantNameAndCampaignIdAndCustomerId(tenantName: String, campaignId: UUID, customerId: UUID): Publication?
    fun findByTenantNameAndVoucherIdAndCustomerId(tenantName: String, voucherId: UUID, customerId: UUID): Publication?
    fun findByIdAndTenantName(id: UUID, tenantName: String): Publication?

    @Query(
        """
        select distinct p from Publication p
        left join p.voucher v
        left join p.vouchers vv
        where p.tenant.name = :tenantName
          and (v.code = :voucherCode or vv.code = :voucherCode)
        """
    )
    fun findAllByTenantNameAndVoucherCode(
        @Param("tenantName") tenantName: String,
        @Param("voucherCode") voucherCode: String
    ): List<Publication>

    @Query(
        """
        select distinct p from Publication p
        left join p.voucher v
        left join p.vouchers vv
        where p.tenant.name = :tenantName
          and (v.id = :voucherId or vv.id = :voucherId)
        """
    )
    fun findAllByTenantNameAndVoucherId(
        @Param("tenantName") tenantName: String,
        @Param("voucherId") voucherId: UUID
    ): List<Publication>
}
