package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import java.time.Instant
import java.util.UUID

interface VoucherRepository : JpaRepository<Voucher, UUID> {
    fun findByCodeAndTenantName(code: String, tenantName: String): Voucher?
    fun findAllByCampaignIdAndTenantName(campaignId: UUID, tenantName: String): List<Voucher>
    fun findAllByTenantName(tenantName: String, pageable: Pageable): Page<Voucher>
    fun findAllByTenantName(tenantName: String): List<Voucher>
    fun findFirstByCampaignIdAndTenantNameAndHolderIsNull(campaignId: UUID, tenantName: String): Voucher?
    fun findAllByCampaignIdAndTenantNameAndHolderIsNull(campaignId: UUID, tenantName: String, pageable: Pageable): List<Voucher>
    fun findAllByCampaignIdAndTenantNameAndHolderId(campaignId: UUID, tenantName: String, holderId: UUID): List<Voucher>
    fun findAllByTenantNameAndHolderId(tenantName: String, holderId: UUID): List<Voucher>
    fun findAllByCampaignIdInAndTenantName(campaignIds: List<UUID>, tenantName: String, pageable: Pageable): Page<Voucher>
    fun countByTenantName(tenantName: String): Long
    fun countByCampaignIdInAndTenantName(campaignIds: List<UUID>, tenantName: String): Long

    @Query(
        """
        select v from Voucher v
        where v.campaign.id = :campaignId
          and v.tenant.name = :tenantName
          and v.holder is null
          and (v.active is null or v.active = true)
          and (v.startDate is null or v.startDate <= :now)
          and (v.expirationDate is null or v.expirationDate >= :now)
        """
    )
    fun findAllSuitableForPublication(
        @Param("campaignId") campaignId: UUID,
        @Param("tenantName") tenantName: String,
        @Param("now") now: Instant,
        pageable: Pageable
    ): List<Voucher>
}
