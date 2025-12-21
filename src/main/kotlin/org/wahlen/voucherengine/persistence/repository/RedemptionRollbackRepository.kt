package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.redemption.RedemptionRollback
import java.util.UUID

interface RedemptionRollbackRepository : JpaRepository<RedemptionRollback, UUID>
