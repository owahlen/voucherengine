package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.SkuCreateRequest
import org.wahlen.voucherengine.api.dto.response.SkuResponse
import org.wahlen.voucherengine.api.dto.response.SkusListResponse
import org.wahlen.voucherengine.persistence.model.product.Sku
import org.wahlen.voucherengine.persistence.repository.ProductRepository
import org.wahlen.voucherengine.persistence.repository.SkuRepository
import java.util.UUID

@Service
class SkuService(
    private val skuRepository: SkuRepository,
    private val productRepository: ProductRepository,
    private val tenantService: TenantService
) {

    @Transactional
    fun createForProduct(tenantName: String, productIdOrSource: String, request: SkuCreateRequest): SkuResponse? {
        val product = findProduct(tenantName, productIdOrSource) ?: return null
        val tenant = tenantService.requireTenant(tenantName)
        val existing = request.source_id?.let { skuRepository.findBySourceIdAndTenantName(it, tenantName) }
        val sku = existing ?: Sku(sourceId = request.source_id)
        assignProduct(sku, product)
        sku.sku = request.sku ?: sku.sku
        sku.price = request.price ?: sku.price
        sku.currency = request.currency ?: sku.currency
        sku.attributes = request.attributes ?: sku.attributes
        sku.metadata = request.metadata ?: sku.metadata
        sku.imageUrl = request.image_url ?: sku.imageUrl
        sku.tenant = tenant
        return toResponse(skuRepository.save(sku))
    }

    @Transactional(readOnly = true)
    fun listByProduct(tenantName: String, productIdOrSource: String): SkusListResponse? {
        val product = findProduct(tenantName, productIdOrSource) ?: return null
        val skus = skuRepository.findAllByProductIdAndTenantName(product.id!!, tenantName).map(::toResponse)
        return SkusListResponse(data = skus, total = skus.size)
    }

    @Transactional(readOnly = true)
    fun getByIdOrSource(tenantName: String, idOrSource: String): SkuResponse? =
        findSku(tenantName, idOrSource)?.let(::toResponse)

    @Transactional
    fun updateForProduct(tenantName: String, productIdOrSource: String, skuIdOrSource: String, request: SkuCreateRequest): SkuResponse? {
        val product = findProduct(tenantName, productIdOrSource) ?: return null
        val sku = findSku(tenantName, skuIdOrSource) ?: return null
        assignProduct(sku, product)
        sku.sourceId = request.source_id ?: sku.sourceId
        sku.sku = request.sku ?: sku.sku
        sku.price = request.price ?: sku.price
        sku.currency = request.currency ?: sku.currency
        sku.attributes = request.attributes ?: sku.attributes
        sku.metadata = request.metadata ?: sku.metadata
        sku.imageUrl = request.image_url ?: sku.imageUrl
        return toResponse(skuRepository.save(sku))
    }

    @Transactional
    fun delete(tenantName: String, idOrSource: String): Boolean {
        val sku = findSku(tenantName, idOrSource) ?: return false
        sku.product?.skus?.remove(sku)
        skuRepository.delete(sku)
        return true
    }

    private fun findSku(tenantName: String, idOrSource: String): Sku? {
        val uuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byId = skuRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return skuRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun findProduct(tenantName: String, idOrSource: String): org.wahlen.voucherengine.persistence.model.product.Product? {
        val uuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byId = productRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return productRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun assignProduct(sku: Sku, product: org.wahlen.voucherengine.persistence.model.product.Product) {
        val current = sku.product
        if (current?.id != null && current.id != product.id) {
            current.skus.remove(sku)
        }
        sku.product = product
        if (!product.skus.contains(sku)) {
            product.skus.add(sku)
        }
    }

    private fun toResponse(sku: Sku): SkuResponse =
        SkuResponse(
            id = sku.id,
            source_id = sku.sourceId,
            product_id = sku.product?.id,
            sku = sku.sku,
            price = sku.price,
            currency = sku.currency,
            attributes = sku.attributes,
            metadata = sku.metadata,
            image_url = sku.imageUrl,
            created_at = sku.createdAt,
            updated_at = sku.updatedAt
        )
}
