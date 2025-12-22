package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.CategoryCreateRequest
import org.wahlen.voucherengine.api.dto.response.CategoryResponse
import org.wahlen.voucherengine.persistence.model.voucher.Category
import org.wahlen.voucherengine.service.CategoryService

@RestController
@RequestMapping("/v1")
@Validated
@ApiResponses(
    value = [
        ApiResponse(responseCode = "401", description = "Unauthorized"),
        ApiResponse(responseCode = "403", description = "Forbidden")
    ]
)
class CategoryController(
    private val categoryService: CategoryService
) {

    @Operation(
        summary = "Create category",
        operationId = "createCategory",
        responses = [
            ApiResponse(responseCode = "201", description = "Category created"),
            ApiResponse(responseCode = "400", description = "Validation error")
        ]
    )
    @PostMapping("/categories")
    fun createCategory(
        @RequestHeader("tenant") tenant: String,
        @Valid @RequestBody body: CategoryCreateRequest
    ): ResponseEntity<CategoryResponse> {
        val saved = categoryService.create(tenant, body)
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved))
    }

    @Operation(
        summary = "Get category",
        operationId = "getCategory",
        responses = [
            ApiResponse(responseCode = "200", description = "Category found"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @GetMapping("/categories/{id}")
    fun getCategory(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: java.util.UUID
    ): ResponseEntity<CategoryResponse> {
        val category = categoryService.get(tenant, id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toResponse(category))
    }

    @Operation(
        summary = "List categories",
        operationId = "listCategories",
        responses = [
            ApiResponse(responseCode = "200", description = "List of categories")
        ]
    )
    @GetMapping("/categories")
    fun listCategories(@RequestHeader("tenant") tenant: String): ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.ok(categoryService.list(tenant).map(::toResponse))

    @Operation(
        summary = "Update category",
        operationId = "updateCategory",
        responses = [
            ApiResponse(responseCode = "200", description = "Category updated"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PutMapping("/categories/{id}")
    fun updateCategory(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody body: CategoryCreateRequest
    ): ResponseEntity<CategoryResponse> {
        val updated = categoryService.update(tenant, id, body) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toResponse(updated))
    }

    @Operation(
        summary = "Delete category",
        operationId = "deleteCategory",
        responses = [
            ApiResponse(responseCode = "204", description = "Category deleted"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @DeleteMapping("/categories/{id}")
    fun deleteCategory(
        @RequestHeader("tenant") tenant: String,
        @PathVariable id: java.util.UUID
    ): ResponseEntity<Void> {
        return if (categoryService.delete(tenant, id)) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    private fun toResponse(category: Category) = CategoryResponse(
        id = category.id,
        name = category.name,
        created_at = category.createdAt,
        updated_at = category.updatedAt
    )
}
