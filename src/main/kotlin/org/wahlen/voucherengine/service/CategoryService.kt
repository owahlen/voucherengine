package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.CategoryCreateRequest
import org.wahlen.voucherengine.persistence.model.voucher.Category
import org.wahlen.voucherengine.persistence.repository.CategoryRepository
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val tenantService: TenantService
) {

    @Transactional
    fun create(tenantName: String, request: CategoryCreateRequest): Category {
        val tenant = tenantService.requireTenant(tenantName)
        val category = Category(name = request.name)
        category.tenant = tenant
        return categoryRepository.save(category)
    }

    @Transactional(readOnly = true)
    fun list(tenantName: String): List<Category> = categoryRepository.findAllByTenantName(tenantName)

    @Transactional(readOnly = true)
    fun get(tenantName: String, id: UUID): Category? = categoryRepository.findByIdAndTenantName(id, tenantName)

    @Transactional
    fun update(tenantName: String, id: UUID, request: CategoryCreateRequest): Category? {
        val existing = categoryRepository.findByIdAndTenantName(id, tenantName) ?: return null
        existing.name = request.name
        return categoryRepository.save(existing)
    }

    @Transactional
    fun delete(tenantName: String, id: UUID): Boolean {
        val existing = categoryRepository.findByIdAndTenantName(id, tenantName) ?: return false
        categoryRepository.delete(existing)
        return true
    }
}
