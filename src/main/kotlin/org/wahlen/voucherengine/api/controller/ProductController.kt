package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.ProductCreateRequest
import org.wahlen.voucherengine.api.dto.request.SkuCreateRequest
import org.wahlen.voucherengine.api.dto.response.ProductResponse
import org.wahlen.voucherengine.api.dto.response.ProductsListResponse
import org.wahlen.voucherengine.api.dto.response.SkuResponse
import org.wahlen.voucherengine.api.dto.response.SkusListResponse
import org.wahlen.voucherengine.service.ProductService
import org.wahlen.voucherengine.service.SkuService

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class ProductController(
    private val productService: ProductService,
    private val skuService: SkuService
) {

    @Operation(
        summary = "Create product",
        operationId = "createProduct",
        responses = [
            ApiResponse(responseCode = "201", description = "Product created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/products")
    fun createProduct(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: ProductCreateRequest
    ): ResponseEntity<ProductResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(productService.create(tenant, body))

    @Operation(
        summary = "List products",
        operationId = "listProducts",
        responses = [
            ApiResponse(responseCode = "200", description = "List of products")
        ]
    )
    @GetMapping("/products")
    fun listProducts(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @Parameter(description = "Sort field, prefix with '-' for descending", example = "-created_at")
        @RequestParam(required = false, defaultValue = "created_at") order: String
    ): ResponseEntity<ProductsListResponse> {
        val sort = parseSort(order, mapOf("created_at" to "createdAt", "updated_at" to "updatedAt", "name" to "name"), "created_at")
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = org.springframework.data.domain.PageRequest.of((page - 1).coerceAtLeast(0), cappedLimit, sort)
        val productsPage = productService.list(tenant, pageable)
        return ResponseEntity.ok(
            ProductsListResponse(
                products = productsPage.content,
                total = productsPage.totalElements.toInt()
            )
        )
    }

    @Operation(
        summary = "Get product",
        operationId = "getProduct",
        responses = [
            ApiResponse(responseCode = "200", description = "Product found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/products/{productId}")
    fun getProduct(
        @RequestHeader("tenant") tenant: String,
        @PathVariable productId: String
    ): ResponseEntity<ProductResponse> {
        val product = productService.getByIdOrSource(tenant, productId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(product)
    }

    @Operation(
        summary = "Update product",
        operationId = "updateProduct",
        responses = [
            ApiResponse(responseCode = "200", description = "Product updated"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PutMapping("/products/{productId}")
    fun updateProduct(
        @RequestHeader("tenant") tenant: String,
        @PathVariable productId: String,
        @Valid @RequestBody body: ProductCreateRequest
    ): ResponseEntity<ProductResponse> {
        val updated = productService.update(tenant, productId, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @Operation(
        summary = "Delete product",
        operationId = "deleteProduct",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/products/{productId}")
    fun deleteProduct(
        @RequestHeader("tenant") tenant: String,
        @PathVariable productId: String
    ): ResponseEntity<Void> {
        return if (productService.delete(tenant, productId)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "List SKUs for product",
        operationId = "listProductSkus",
        responses = [
            ApiResponse(responseCode = "200", description = "List of SKUs"),
            ApiResponse(responseCode = "404", description = "Product not found")
        ]
    )
    @GetMapping("/products/{productId}/skus")
    fun listProductSkus(
        @RequestHeader("tenant") tenant: String,
        @PathVariable productId: String
    ): ResponseEntity<SkusListResponse> {
        val skus = skuService.listByProduct(tenant, productId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(skus)
    }

    @Operation(
        summary = "Create SKU for product",
        operationId = "createSkuForProduct",
        responses = [
            ApiResponse(responseCode = "201", description = "SKU created"),
            ApiResponse(responseCode = "404", description = "Product not found")
        ]
    )
    @PostMapping("/products/{productId}/skus")
    fun createSkuForProduct(
        @RequestHeader("tenant") tenant: String,
        @PathVariable productId: String,
        @Valid @RequestBody body: SkuCreateRequest
    ): ResponseEntity<SkuResponse> {
        val sku = skuService.createForProduct(tenant, productId, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(HttpStatus.CREATED).body(sku)
    }

    @Operation(
        summary = "Update SKU for product",
        operationId = "updateSkuForProduct",
        responses = [
            ApiResponse(responseCode = "200", description = "SKU updated"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PutMapping("/products/{productId}/skus/{skuId}")
    fun updateSkuForProduct(
        @RequestHeader("tenant") tenant: String,
        @PathVariable productId: String,
        @PathVariable skuId: String,
        @Valid @RequestBody body: SkuCreateRequest
    ): ResponseEntity<SkuResponse> {
        val sku = skuService.updateForProduct(tenant, productId, skuId, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sku)
    }

    @Operation(
        summary = "Delete SKU for product",
        operationId = "deleteSkuForProduct",
        responses = [
            ApiResponse(responseCode = "204", description = "Deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/products/{productId}/skus/{skuId}")
    fun deleteSkuForProduct(
        @RequestHeader("tenant") tenant: String,
        @PathVariable productId: String,
        @PathVariable skuId: String
    ): ResponseEntity<Void> {
        return if (skuService.delete(tenant, skuId)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "Get SKU",
        operationId = "getSku",
        responses = [
            ApiResponse(responseCode = "200", description = "SKU found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/skus/{skuId}")
    fun getSku(
        @RequestHeader("tenant") tenant: String,
        @PathVariable skuId: String
    ): ResponseEntity<SkuResponse> {
        val sku = skuService.getByIdOrSource(tenant, skuId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(sku)
    }

    @Operation(
        summary = "Import SKUs from CSV",
        operationId = "importSkusCSV",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - SKU CSV import not yet supported")
        ]
    )
    @PostMapping("/skus/importCSV", consumes = ["text/csv"])
    fun importSkusCSV(
        @RequestHeader("tenant") tenant: String,
        @RequestBody csvContent: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "SKU CSV import not yet implemented"))
    }

    @Operation(
        summary = "Import products from CSV",
        operationId = "importProductsCSV",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - product CSV import not yet supported")
        ]
    )
    @PostMapping("/products/importCSV", consumes = ["text/csv"])
    fun importProductsCSV(
        @RequestHeader("tenant") tenant: String,
        @RequestBody csvContent: String
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Product CSV import not yet implemented"))
    }

    @Operation(
        summary = "Update products in bulk asynchronously",
        operationId = "updateProductsBulkAsync",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - product bulk update not yet supported")
        ]
    )
    @PostMapping("/products/bulk/async")
    fun updateProductsBulkAsync(
        @RequestHeader("tenant") tenant: String,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Product bulk update not yet implemented"))
    }

    @Operation(
        summary = "Update products metadata in bulk asynchronously",
        operationId = "updateProductsMetadataAsync",
        responses = [
            ApiResponse(responseCode = "501", description = "Not implemented - product metadata bulk update not yet supported")
        ]
    )
    @PostMapping("/products/metadata/async")
    fun updateProductsMetadataAsync(
        @RequestHeader("tenant") tenant: String,
        @RequestBody body: Map<String, Any>
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(mapOf("message" to "Product metadata bulk update not yet implemented"))
    }
}
