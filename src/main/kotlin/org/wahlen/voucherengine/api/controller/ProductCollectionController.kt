package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.ProductCollectionCreateRequest
import org.wahlen.voucherengine.api.dto.response.ProductCollectionResponse
import org.wahlen.voucherengine.api.dto.response.ProductCollectionsListResponse
import org.wahlen.voucherengine.api.dto.response.ProductCollectionsProductsListResponse
import org.wahlen.voucherengine.service.ProductCollectionService

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class ProductCollectionController(
    private val productCollectionService: ProductCollectionService
) {

    @Operation(
        summary = "List product collections",
        operationId = "listProductCollections",
        responses = [
            ApiResponse(responseCode = "200", description = "List of collections")
        ]
    )
    @GetMapping("/product-collections")
    fun listCollections(
        @RequestHeader("tenant") tenant: String,
        @Parameter(description = "Max number of items per page", example = "10")
        @RequestParam(required = false, defaultValue = "10") limit: Int,
        @Parameter(description = "1-based page index", example = "1")
        @RequestParam(required = false, defaultValue = "1") page: Int,
        @Parameter(description = "Sort field, prefix with '-' for descending", example = "-created_at")
        @RequestParam(required = false, defaultValue = "created_at") order: String
    ): ResponseEntity<ProductCollectionsListResponse> {
        val sort = parseSort(order, mapOf("created_at" to "createdAt", "updated_at" to "updatedAt", "name" to "name"), "created_at")
        val cappedLimit = limit.coerceIn(1, 100)
        val pageable = PageRequest.of((page - 1).coerceAtLeast(0), cappedLimit, sort)
        return ResponseEntity.ok(productCollectionService.list(tenant, pageable))
    }

    @Operation(
        summary = "Create product collection",
        operationId = "createProductCollection",
        responses = [
            ApiResponse(responseCode = "201", description = "Collection created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/product-collections")
    fun createCollection(
        @RequestHeader("tenant") tenant: String,
        @Valid @org.springframework.web.bind.annotation.RequestBody body: ProductCollectionCreateRequest
    ): ResponseEntity<ProductCollectionResponse> =
        ResponseEntity.status(HttpStatus.OK).body(productCollectionService.create(tenant, body))

    @Operation(
        summary = "Get product collection",
        operationId = "getProductCollection",
        responses = [
            ApiResponse(responseCode = "200", description = "Collection found"),
            ApiResponse(responseCode = "404", description = "Collection not found")
        ]
    )
    @GetMapping("/product-collections/{collectionId}")
    fun getCollection(
        @RequestHeader("tenant") tenant: String,
        @PathVariable collectionId: String
    ): ResponseEntity<ProductCollectionResponse> {
        val collection = productCollectionService.get(tenant, collectionId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(collection)
    }

    @Operation(
        summary = "Update product collection",
        operationId = "updateProductCollection",
        responses = [
            ApiResponse(responseCode = "200", description = "Collection updated"),
            ApiResponse(responseCode = "404", description = "Collection not found")
        ]
    )
    @PutMapping("/product-collections/{collectionId}")
    fun updateCollection(
        @RequestHeader("tenant") tenant: String,
        @PathVariable collectionId: String,
        @Valid @org.springframework.web.bind.annotation.RequestBody body: ProductCollectionCreateRequest
    ): ResponseEntity<ProductCollectionResponse> {
        val updated = productCollectionService.update(tenant, collectionId, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @Operation(
        summary = "Delete product collection",
        operationId = "deleteProductCollection",
        responses = [
            ApiResponse(responseCode = "204", description = "Collection deleted"),
            ApiResponse(responseCode = "404", description = "Collection not found")
        ]
    )
    @DeleteMapping("/product-collections/{collectionId}")
    fun deleteCollection(
        @RequestHeader("tenant") tenant: String,
        @PathVariable collectionId: String
    ): ResponseEntity<Void> {
        return if (productCollectionService.delete(tenant, collectionId)) ResponseEntity.noContent().build()
        else ResponseEntity.notFound().build()
    }

    @Operation(
        summary = "List products in collection",
        operationId = "listProductCollectionProducts",
        responses = [
            ApiResponse(responseCode = "200", description = "Products in collection"),
            ApiResponse(responseCode = "404", description = "Collection not found")
        ]
    )
    @GetMapping("/product-collections/{collectionId}/products")
    fun listCollectionProducts(
        @RequestHeader("tenant") tenant: String,
        @PathVariable collectionId: String
    ): ResponseEntity<ProductCollectionsProductsListResponse> {
        val products = productCollectionService.listProducts(tenant, collectionId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(products)
    }

}
