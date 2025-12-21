package org.wahlen.voucherengine.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.wahlen.voucherengine.persistence.model.voucher.Category
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID>
