package org.wahlen.voucherengine.api.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.wahlen.voucherengine.api.dto.request.CategoryCreateRequest
import org.wahlen.voucherengine.api.dto.response.CategoryResponse
import org.wahlen.voucherengine.persistence.model.voucher.Category
import org.wahlen.voucherengine.persistence.repository.CategoryRepository

@RestController
@RequestMapping("/v1")
@Validated
class CategoryController(
    private val categoryRepository: CategoryRepository
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
    fun createCategory(@Valid @RequestBody body: CategoryCreateRequest): ResponseEntity<CategoryResponse> {
        val saved = categoryRepository.save(Category(name = body.name))
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
    fun getCategory(@PathVariable id: java.util.UUID): ResponseEntity<CategoryResponse> {
        val category = categoryRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
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
    fun listCategories(): ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.ok(categoryRepository.findAll().map(::toResponse))

    @Operation(
        summary = "Update category",
        operationId = "updateCategory",
        responses = [
            ApiResponse(responseCode = "200", description = "Category updated"),
            ApiResponse(responseCode = "404", description = "Not found")
        ]
    )
    @PutMapping("/categories/{id}")
    fun updateCategory(@PathVariable id: java.util.UUID, @Valid @RequestBody body: CategoryCreateRequest): ResponseEntity<CategoryResponse> {
        val existing = categoryRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        existing.name = body.name
        val saved = categoryRepository.save(existing)
        return ResponseEntity.ok(toResponse(saved))
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
    fun deleteCategory(@PathVariable id: java.util.UUID): ResponseEntity<Void> {
        val existing = categoryRepository.findById(id)
        return if (existing.isPresent) {
            categoryRepository.delete(existing.get())
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun toResponse(category: Category) = CategoryResponse(
        id = category.id,
        name = category.name,
        created_at = category.createdAt,
        updated_at = category.updatedAt
    )
}
