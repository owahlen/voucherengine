package org.wahlen.voucherengine.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.wahlen.voucherengine.api.dto.request.ProductCollectionCreateRequest
import org.wahlen.voucherengine.api.dto.request.ProductCollectionItemRequest
import org.wahlen.voucherengine.api.dto.response.ProductCollectionItemResponse
import org.wahlen.voucherengine.api.dto.response.ProductCollectionResponse
import org.wahlen.voucherengine.api.dto.response.ProductCollectionsProductsListResponse
import org.wahlen.voucherengine.api.dto.response.ProductCollectionsListResponse
import org.wahlen.voucherengine.api.dto.response.ProductResponse
import org.wahlen.voucherengine.api.dto.response.SkuWithProductResponse
import org.wahlen.voucherengine.persistence.model.product.Product
import org.wahlen.voucherengine.persistence.model.product.Sku
import org.wahlen.voucherengine.persistence.model.productcollection.ProductCollection
import org.wahlen.voucherengine.persistence.model.productcollection.ProductCollectionItem
import org.wahlen.voucherengine.persistence.model.productcollection.ProductCollectionItemType
import org.wahlen.voucherengine.persistence.model.productcollection.ProductCollectionType
import org.wahlen.voucherengine.persistence.repository.ProductCollectionRepository
import org.wahlen.voucherengine.persistence.repository.ProductRepository
import org.wahlen.voucherengine.persistence.repository.SkuRepository
import java.util.UUID

@Service
class ProductCollectionService(
    private val productCollectionRepository: ProductCollectionRepository,
    private val productRepository: ProductRepository,
    private val skuRepository: SkuRepository,
    private val tenantService: TenantService
) {

    @Transactional
    fun create(tenantName: String, request: ProductCollectionCreateRequest): ProductCollectionResponse {
        val tenant = tenantService.requireTenant(tenantName)
        val collection = ProductCollection()
        collection.tenant = tenant
        applyRequest(collection, request)
        return toResponse(productCollectionRepository.save(collection))
    }

    @Transactional
    fun update(tenantName: String, idOrName: String, request: ProductCollectionCreateRequest): ProductCollectionResponse? {
        val collection = find(tenantName, idOrName) ?: return null
        applyRequest(collection, request)
        return toResponse(productCollectionRepository.save(collection))
    }

    @Transactional(readOnly = true)
    fun get(tenantName: String, idOrName: String): ProductCollectionResponse? =
        find(tenantName, idOrName)?.let(::toResponse)

    @Transactional(readOnly = true)
    fun list(tenantName: String, pageable: Pageable): ProductCollectionsListResponse {
        val page = productCollectionRepository.findAllByTenantName(tenantName, pageable)
        val data = page.content.map(::toResponse)
        return ProductCollectionsListResponse(data = data, total = page.totalElements.toInt())
    }

    @Transactional
    fun delete(tenantName: String, idOrName: String): Boolean {
        val collection = find(tenantName, idOrName) ?: return false
        productCollectionRepository.delete(collection)
        return true
    }

    @Transactional(readOnly = true)
    fun listProducts(tenantName: String, idOrName: String): ProductCollectionsProductsListResponse? {
        val collection = find(tenantName, idOrName) ?: return null
        if (collection.type == ProductCollectionType.AUTO_UPDATE) {
        val products = productRepository.findAllByTenantName(tenantName)
        val skus = skuRepository.findAllByTenantName(tenantName)
        val filter = collection.filter
        val matchedProducts = products.filter { matchesFilter(filter, it) }
        val matchedSkus = skus.filter { sku ->
            matchesFilter(filter, sku) || sku.product?.let { matchesFilter(filter, it) } == true
        }
            val data = matchedProducts.map { toProductResponse(it, tenantName, includeSkus = false) } +
                matchedSkus.map { toSkuWithProductResponse(it) }
            return ProductCollectionsProductsListResponse(data = data, total = data.size)
        }
        val items = collection.items.mapNotNull { item ->
            when (item.objectType) {
                ProductCollectionItemType.PRODUCT -> {
                    findProduct(tenantName, item.itemId)?.let { toProductResponse(it, tenantName, includeSkus = false) }
                }
                ProductCollectionItemType.SKU -> {
                    findSku(tenantName, item.itemId)?.let { toSkuWithProductResponse(it) }
                }
                else -> null
            }
        }
        return ProductCollectionsProductsListResponse(data = items, total = items.size)
    }

    private fun applyRequest(collection: ProductCollection, request: ProductCollectionCreateRequest) {
        collection.name = request.name ?: collection.name
        collection.type = request.type?.let { ProductCollectionType.valueOf(it.uppercase()) }
            ?: collection.type
            ?: defaultType(request)
        collection.filter = request.filter ?: collection.filter
        val products = request.products
        if (products != null) {
            collection.items.clear()
            products.forEach { itemRequest ->
                val item = toItem(itemRequest, collection)
                if (item != null) {
                    collection.items.add(item)
                }
            }
        }
    }

    private fun defaultType(request: ProductCollectionCreateRequest): ProductCollectionType =
        if (request.products.isNullOrEmpty()) ProductCollectionType.AUTO_UPDATE else ProductCollectionType.STATIC

    private fun toItem(
        request: ProductCollectionItemRequest,
        collection: ProductCollection
    ): ProductCollectionItem? {
        val objectType = request.`object`?.let { ProductCollectionItemType.valueOf(it.uppercase()) } ?: return null
        val item = ProductCollectionItem(
            itemId = request.id,
            productId = request.product_id,
            objectType = objectType,
            collection = collection
        )
        item.tenant = collection.tenant
        return item
    }

    private fun find(tenantName: String, idOrName: String): ProductCollection? {
        val uuid = runCatching { UUID.fromString(idOrName) }.getOrNull()
        if (uuid != null) {
            val byId = productCollectionRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return productCollectionRepository.findByNameAndTenantName(idOrName, tenantName)
    }

    private fun toResponse(collection: ProductCollection): ProductCollectionResponse =
        ProductCollectionResponse(
            id = collection.id,
            name = collection.name,
            type = collection.type?.name,
            filter = collection.filter,
            products = collection.items.map(::toItemResponse),
            created_at = collection.createdAt
        )

    private fun toItemResponse(item: ProductCollectionItem): ProductCollectionItemResponse =
        ProductCollectionItemResponse(
            id = item.itemId,
            product_id = item.productId,
            `object` = item.objectType?.name?.lowercase()
        )

    private fun findProduct(tenantName: String, idOrSource: String?): Product? {
        if (idOrSource.isNullOrBlank()) return null
        val uuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byId = productRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return productRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun findSku(tenantName: String, idOrSource: String?): Sku? {
        if (idOrSource.isNullOrBlank()) return null
        val uuid = runCatching { UUID.fromString(idOrSource) }.getOrNull()
        if (uuid != null) {
            val byId = skuRepository.findByIdAndTenantName(uuid, tenantName)
            if (byId != null) return byId
        }
        return skuRepository.findBySourceIdAndTenantName(idOrSource, tenantName)
    }

    private fun matchesFilter(filter: Map<String, Any?>?, product: Product): Boolean {
        if (filter == null) return true
        val junction = (filter["junction"] as? String)?.lowercase() ?: "and"
        val conditions = filter.filterKeys { it != "junction" }
        if (conditions.isEmpty()) return true
        val results = conditions.map { (field, definition) ->
            val conditionMap = when (definition) {
                is Map<*, *> -> {
                    val nested = definition["conditions"]
                    if (nested is Map<*, *>) nested else definition
                }
                else -> emptyMap<Any?, Any?>()
            }
            matchesConditions(resolveProductField(product, field), conditionMap)
        }
        return if (junction == "or") results.any { it } else results.all { it }
    }

    private fun matchesFilter(filter: Map<String, Any?>?, sku: Sku): Boolean {
        if (filter == null) return true
        val junction = (filter["junction"] as? String)?.lowercase() ?: "and"
        val conditions = filter.filterKeys { it != "junction" }
        if (conditions.isEmpty()) return true
        val results = conditions.map { (field, definition) ->
            val conditionMap = when (definition) {
                is Map<*, *> -> {
                    val nested = definition["conditions"]
                    if (nested is Map<*, *>) nested else definition
                }
                else -> emptyMap<Any?, Any?>()
            }
            matchesConditions(resolveSkuField(sku, field), conditionMap)
        }
        return if (junction == "or") results.any { it } else results.all { it }
    }

    private fun resolveProductField(product: Product, field: String): Any? {
        return when {
            field == "id" -> product.id?.toString()
            field == "name" -> product.name
            field == "source_id" -> product.sourceId
            field == "price" -> product.price
            field == "attributes" -> product.attributes
            field == "image_url" -> product.imageUrl
            field.startsWith("metadata.") -> {
                val key = field.removePrefix("metadata.")
                product.metadata?.get(key)
            }
            else -> null
        }
    }

    private fun resolveSkuField(sku: Sku, field: String): Any? {
        return when {
            field.startsWith("product.") -> sku.product?.let { resolveProductField(it, field.removePrefix("product.")) }
            field == "id" -> sku.id?.toString()
            field == "product_id" -> sku.product?.id?.toString()
            field == "sku" -> sku.sku
            field == "source_id" -> sku.sourceId
            field == "price" -> sku.price
            field == "currency" -> sku.currency
            field == "attributes" -> sku.attributes
            field == "image_url" -> sku.imageUrl
            field.startsWith("metadata.") -> {
                val key = field.removePrefix("metadata.")
                sku.metadata?.get(key)
            }
            else -> null
        }
    }

    private fun matchesConditions(actual: Any?, conditionMap: Map<*, *>): Boolean {
        if (conditionMap.isEmpty()) return false
        return conditionMap.entries.all { (op, value) ->
            when (op as? String) {
                "\$eq" -> actual == value
                "\$ne" -> actual != value
                "\$contains" -> contains(actual, value)
                "\$contains_any" -> containsAny(actual, value)
                "\$contains_all" -> containsAll(actual, value)
                "\$in", "\$is" -> isIn(actual, value)
                "\$is_not" -> !isIn(actual, value)
                "\$gt" -> compareNumber(actual, value) > 0
                "\$gte" -> compareNumber(actual, value) >= 0
                "\$lt" -> compareNumber(actual, value) < 0
                "\$lte" -> compareNumber(actual, value) <= 0
                else -> false
            }
        }
    }

    private fun contains(actual: Any?, expected: Any?): Boolean {
        return when (actual) {
            is String -> expected is String && actual.contains(expected)
            is Iterable<*> -> actual.any { it == expected }
            is Map<*, *> -> actual.values.any { it == expected } || actual.keys.any { it == expected }
            else -> false
        }
    }

    private fun containsAny(actual: Any?, expected: Any?): Boolean {
        val expectedList = expected as? Iterable<*> ?: return false
        return when (actual) {
            is String -> expectedList.filterIsInstance<String>().any { actual.contains(it) }
            is Iterable<*> -> actual.any { item -> expectedList.any { it == item } }
            is Map<*, *> -> actual.values.any { value -> expectedList.any { it == value } } ||
                actual.keys.any { key -> expectedList.any { it == key } }
            else -> false
        }
    }

    private fun containsAll(actual: Any?, expected: Any?): Boolean {
        val expectedList = expected as? Iterable<*> ?: return false
        return when (actual) {
            is Iterable<*> -> expectedList.all { item -> actual.any { it == item } }
            is Map<*, *> -> expectedList.all { item ->
                actual.values.any { it == item } || actual.keys.any { it == item }
            }
            else -> false
        }
    }

    private fun isIn(actual: Any?, expected: Any?): Boolean {
        val expectedList = expected as? Iterable<*> ?: return false
        return expectedList.any { it == actual }
    }

    private fun compareNumber(actual: Any?, expected: Any?): Int {
        val actualNumber = (actual as? Number)?.toDouble() ?: return -1
        val expectedNumber = (expected as? Number)?.toDouble() ?: return -1
        return actualNumber.compareTo(expectedNumber)
    }

    private fun toProductResponse(product: Product, tenantName: String, includeSkus: Boolean): ProductResponse =
        ProductResponse(
            id = product.id,
            source_id = product.sourceId,
            name = product.name,
            price = product.price,
            attributes = product.attributes,
            metadata = product.metadata,
            image_url = product.imageUrl,
            created_at = product.createdAt,
            updated_at = product.updatedAt,
            skus = if (includeSkus) {
                val skuResponses = skuRepository.findAllByProductIdAndTenantName(product.id!!, tenantName)
                    .map { sku -> org.wahlen.voucherengine.api.dto.response.SkuResponse(
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
                    ) }
                org.wahlen.voucherengine.api.dto.response.SkusListResponse(data = skuResponses, total = skuResponses.size)
            } else null
        )

    private fun toSkuWithProductResponse(sku: Sku): SkuWithProductResponse =
        SkuWithProductResponse(
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
            updated_at = sku.updatedAt,
            product = sku.product?.let { toProductResponse(it, tenantName = it.tenant?.name ?: "", includeSkus = false) }
        )
}
