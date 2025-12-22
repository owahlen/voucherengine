package org.wahlen.voucherengine.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.ProductCreateRequest
import org.wahlen.voucherengine.api.dto.response.ProductResponse
import org.wahlen.voucherengine.api.dto.response.SkusListResponse
import org.wahlen.voucherengine.api.dto.response.SkuResponse
import org.wahlen.voucherengine.persistence.model.product.Product
import org.wahlen.voucherengine.persistence.repository.ProductRepository
import org.wahlen.voucherengine.persistence.repository.SkuRepository
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val skuRepository: SkuRepository,
    private val tenantService: TenantService
) {

    @Transactional
    fun create(tenantName: String, request: ProductCreateRequest): ProductResponse {
        val tenant = tenantService.requireTenant(tenantName)
        val existing = request.source_id?.let { productRepository.findBySourceIdAndTenantName(it, tenantName) }
        val product = existing ?: Product(sourceId = request.source_id)
        product.name = request.name ?: product.name
        product.price = request.price ?: product.price
        product.attributes = request.attributes ?: product.attributes
        product.metadata = request.metadata ?: product.metadata
        product.imageUrl = request.image_url ?: product.imageUrl
        product.tenant = tenant
        return toResponse(productRepository.save(product), tenantName, includeSkus = true)
    }

    @Transactional(readOnly = true)
    fun list(tenantName: String): List<ProductResponse> =
        productRepository.findAllByTenantName(tenantName).map { toResponse(it, tenantName, includeSkus = false) }

    @Transactional(readOnly = true)
    fun getByIdOrSource(tenantName: String, idOrSource: String): ProductResponse? =
        findEntity(tenantName, idOrSource)?.let { toResponse(it, tenantName, includeSkus = true) }

    @Transactional
    fun update(tenantName: String, idOrSource: String, request: ProductCreateRequest): ProductResponse? {
        val existing = findEntity(tenantName, idOrSource) ?: return null
        existing.name = request.name ?: existing.name
        existing.sourceId = request.source_id ?: existing.sourceId
        existing.price = request.price ?: existing.price
        existing.attributes = request.attributes ?: existing.attributes
        existing.metadata = request.metadata ?: existing.metadata
        existing.imageUrl = request.image_url ?: existing.imageUrl
        return toResponse(productRepository.save(existing), tenantName, includeSkus = true)
    }

    @Transactional
    fun delete(tenantName: String, idOrSource: String): Boolean {
        val existing = findEntity(tenantName, idOrSource) ?: return false
        productRepository.delete(existing)
        return true
    }

    @Transactional(readOnly = true)
    fun findEntity(tenantName: String, idOrSource: String): Product? {
        val uuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byId = productRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return productRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun toResponse(product: Product, tenantName: String, includeSkus: Boolean): ProductResponse {
        val skus = if (includeSkus) {
            val skuResponses = skuRepository.findAllByProductIdAndTenantName(product.id!!, tenantName).map(::toSkuResponse)
            SkusListResponse(
                data = skuResponses,
                total = skuResponses.size
            )
        } else null
        return ProductResponse(
            id = product.id,
            source_id = product.sourceId,
            name = product.name,
            price = product.price,
            attributes = product.attributes,
            metadata = product.metadata,
            image_url = product.imageUrl,
            created_at = product.createdAt,
            updated_at = product.updatedAt,
            skus = skus
        )
    }

    private fun toSkuResponse(sku: org.wahlen.voucherengine.persistence.model.product.Sku): SkuResponse =
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
