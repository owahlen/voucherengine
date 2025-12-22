package org.wahlen.voucherengine.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.wahlen.voucherengine.api.dto.common.SessionDto
import org.wahlen.voucherengine.api.dto.request.ValidationStackRequest
import org.wahlen.voucherengine.api.dto.request.VoucherValidationRequest
import org.wahlen.voucherengine.api.dto.response.QualificationGiftResult
import org.wahlen.voucherengine.api.dto.response.QualificationItemList
import org.wahlen.voucherengine.api.dto.response.QualificationLoyaltyCardResult
import org.wahlen.voucherengine.api.dto.response.StackingRulesResponse
import org.wahlen.voucherengine.api.dto.response.ValidationErrorDetail
import org.wahlen.voucherengine.api.dto.response.ValidationOrderCalculated
import org.wahlen.voucherengine.api.dto.response.ValidationOrderItemResponse
import org.wahlen.voucherengine.api.dto.response.ValidationRedeemableResponse
import org.wahlen.voucherengine.api.dto.response.ValidationRedeemableResultPayload
import org.wahlen.voucherengine.api.dto.response.ValidationRuleAssignmentResponse
import org.wahlen.voucherengine.api.dto.response.ValidationRulesAssignmentsListResponse
import org.wahlen.voucherengine.api.dto.response.ValidationSkippedDetails
import org.wahlen.voucherengine.api.dto.response.ValidationsValidateResponse
import org.wahlen.voucherengine.persistence.model.voucher.VoucherType
import org.wahlen.voucherengine.persistence.repository.ValidationRulesAssignmentRepository
import java.util.UUID

@Service
class ValidationStackService(
    private val voucherService: VoucherService,
    private val stackingRulesProperties: org.wahlen.voucherengine.config.StackingRulesProperties,
    private val sessionLockService: SessionLockService,
    private val validationRulesAssignmentRepository: ValidationRulesAssignmentRepository
) {

    fun validate(tenantName: String, request: ValidationStackRequest): ValidationsValidateResponse {
        if (request.redeemables.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "redeemables is required")
        }
        if (request.redeemables.size > 30) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "redeemables limit exceeded")
        }
        val uniqueKeys = request.redeemables.map { "${it.`object`}::${it.id}" }
        if (uniqueKeys.size != uniqueKeys.toSet().size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "redeemables must be unique")
        }

        val stackingRules = defaultStackingRules()
        val applicationMode = stackingRules.redeemables_application_mode ?: "ALL"
        val applicableLimit = stackingRules.applicable_redeemables_limit ?: 5

        val expand = request.options?.expand ?: emptyList()
        val includeOrder = expand.isEmpty() || expand.contains("order")
        val includeRedeemable = expand.contains("redeemable")
        val includeCategory = expand.contains("category")
        val includeValidationRules = expand.contains("validation_rules")

        var session = buildSession(request.session)
        val trackingSource = request.customer?.source_id ?: request.tracking_id
        val trackingId = trackingSource?.let(::hashTrackingId)

        val redeemables = mutableListOf<ValidationRedeemableResponse>()
        val skipped = mutableListOf<ValidationRedeemableResponse>()
        val inapplicable = mutableListOf<ValidationRedeemableResponse>()
        var applicableCount = 0
        var precedingInvalid = false
        val appliedDiscounts = mutableListOf<Long>()
        val categoryCounts = mutableMapOf<String, Int>()
        val exclusiveCategoryCounts = mutableMapOf<String, Int>()
        var exclusiveCount = 0
        var hasExclusiveApplied = false
        val exclusiveCategoryTokens = stackingRules.exclusive_categories?.map { it.lowercase() }?.toSet() ?: emptySet()
        val jointCategoryTokens = stackingRules.joint_categories?.map { it.lowercase() }?.toSet() ?: emptySet()
        val perCategoryLimit = stackingRules.applicable_redeemables_per_category_limit
        val exclusiveLimit = stackingRules.applicable_exclusive_redeemables_limit
        val exclusivePerCategoryLimit = stackingRules.applicable_exclusive_redeemables_per_category_limit
        val categoryLimits = stackingRules.applicable_redeemables_category_limits ?: emptyMap()

        val orderedRedeemables = orderRedeemables(tenantName, request, stackingRules.redeemables_sorting_rule)
        orderedRedeemables.forEach { context ->
            val redeemable = context.redeemable
            val objectType = redeemable.`object`.lowercase()
            if (precedingInvalid && applicationMode == "ALL") {
                val skippedResult = buildSkipped(
                    redeemable,
                    "preceding_validation_failed",
                    "Redeemable cannot be applied due to preceding validation failure",
                    objectType
                )
                redeemables += skippedResult
                skipped += skippedResult
                return@forEach
            }

            if (objectType == "promotion_tier" || objectType == "promotion_stack") {
                val skippedResult = buildSkipped(
                    redeemable,
                    "promotion_not_supported",
                    "Promotions are not supported yet",
                    objectType
                )
                redeemables += skippedResult
                skipped += skippedResult
                return@forEach
            }
            if (objectType != "voucher" && objectType != "gift_card" && objectType != "loyalty_card") {
                val errorResult = buildInapplicable(
                    redeemable,
                    ValidationErrorDetail("unsupported_redeemable", "Redeemable type not supported"),
                    objectType,
                    includeRedeemable = false,
                    includeCategory = false,
                    tenantName = tenantName,
                    validationRuleId = null,
                    validationRulesAssignments = null
                )
                redeemables += errorResult
                inapplicable += errorResult
                precedingInvalid = true
                return@forEach
            }

            val categoriesForValidation = if (context.categoryIds.isNotEmpty()) context.categoryIds else null
            val validation = voucherService.validateVoucher(
                tenantName,
                redeemable.id,
                VoucherValidationRequest(
                    customer = request.customer,
                    order = request.order,
                    metadata = request.metadata,
                    categories = categoriesForValidation
                )
            )

            if (!validation.valid) {
                val error = ValidationErrorDetail(validation.error?.code, validation.error?.message)
                val voucherEntity = if (includeRedeemable || includeCategory || includeValidationRules) {
                    voucherService.getByCode(tenantName, redeemable.id)
                } else null
                val assignments = if (includeValidationRules) {
                    buildAssignments(tenantName, voucherEntity)
                } else null
                val errorResult = buildInapplicable(
                    redeemable,
                    error,
                    objectType,
                    includeRedeemable,
                    includeCategory,
                    tenantName,
                    validation.validationRuleId,
                    assignments
                )
                redeemables += errorResult
                inapplicable += errorResult
                if (applicationMode == "ALL") {
                    precedingInvalid = true
                }
                return@forEach
            }

            val voucher = validation.voucher
            if (voucher?.type == null) {
                val errorResult = buildInapplicable(
                    redeemable,
                    ValidationErrorDetail("voucher_not_found", "Voucher does not exist."),
                    objectType,
                    includeRedeemable = false,
                    includeCategory = false,
                    tenantName = tenantName,
                    validationRuleId = null,
                    validationRulesAssignments = null
                )
                redeemables += errorResult
                inapplicable += errorResult
                if (applicationMode == "ALL") {
                    precedingInvalid = true
                }
                return@forEach
            }

            if (!matchesRedeemableType(objectType, voucher.type)) {
                val assignments = if (includeValidationRules) {
                    buildAssignments(tenantName, voucherService.getByCode(tenantName, redeemable.id))
                } else null
                val errorResult = buildInapplicable(
                    redeemable,
                    ValidationErrorDetail("invalid_redeemable_type", "Redeemable type does not match voucher type"),
                    objectType,
                    includeRedeemable,
                    includeCategory,
                    tenantName,
                    null,
                    assignments
                )
                redeemables += errorResult
                inapplicable += errorResult
                if (applicationMode == "ALL") {
                    precedingInvalid = true
                }
                return@forEach
            }

            if (context.categoryInfos.isNotEmpty() && hasExclusiveApplied &&
                context.categoryInfos.none { isExclusiveCategory(it, exclusiveCategoryTokens) || isJointCategory(it, jointCategoryTokens) }
            ) {
                val skippedResult = buildSkipped(
                    redeemable,
                    "exclusion_rules_not_met",
                    "Redeemable cannot be applied due to exclusion rules",
                    objectType
                )
                redeemables += skippedResult
                skipped += skippedResult
                return@forEach
            }

            if (context.categoryInfos.isNotEmpty()) {
                val exclusiveMatch = context.categoryInfos.any { isExclusiveCategory(it, exclusiveCategoryTokens) }
                if (exclusiveMatch && exclusiveLimit != null && exclusiveCount >= exclusiveLimit) {
                    val skippedResult = buildSkipped(
                        redeemable,
                        "applicable_exclusive_redeemables_limit_exceeded",
                        "Applicable exclusive redeemables limit exceeded",
                        objectType
                    )
                    redeemables += skippedResult
                    skipped += skippedResult
                    return@forEach
                }
                if (exclusiveMatch && exclusivePerCategoryLimit != null &&
                    context.categoryInfos.any { info ->
                        isExclusiveCategory(info, exclusiveCategoryTokens) &&
                            exclusiveCategoryCounts.getOrDefault(info.idToken, 0) >= exclusivePerCategoryLimit
                    }
                ) {
                    val skippedResult = buildSkipped(
                        redeemable,
                        "applicable_exclusive_redeemables_per_category_limit_exceeded",
                        "Applicable exclusive redeemables limit per category exceeded",
                        objectType
                    )
                    redeemables += skippedResult
                    skipped += skippedResult
                    return@forEach
                }
                if (perCategoryLimit != null &&
                    context.categoryInfos.any { info ->
                        categoryCounts.getOrDefault(info.idToken, 0) >= (categoryLimits[info.idToken] ?: perCategoryLimit)
                    }
                ) {
                    val skippedResult = buildSkipped(
                        redeemable,
                        "applicable_redeemables_per_category_limit_exceeded",
                        "Applicable redeemables limit per category exceeded",
                        objectType
                    )
                    redeemables += skippedResult
                    skipped += skippedResult
                    return@forEach
                }
            }

            if (applicableCount >= applicableLimit) {
                val skippedResult = buildSkipped(
                    redeemable,
                    "applicable_redeemables_limit_exceeded",
                    "Applicable redeemables limit exceeded",
                    objectType
                )
                redeemables += skippedResult
                skipped += skippedResult
                return@forEach
            }

            val discountAmount = validation.order?.discount_amount ?: 0L
            appliedDiscounts += discountAmount
            val resultPayload = ValidationRedeemableResultPayload(
                discount = validation.discount,
                gift = if (voucher.type == VoucherType.GIFT_VOUCHER) {
                    QualificationGiftResult(
                        balance = voucher.gift?.balance,
                        credits = null,
                        locked_credits = if (session?.type?.uppercase() == "LOCK") 0L else null
                    )
                } else null,
                loyalty_card = if (voucher.type == VoucherType.LOYALTY_CARD) {
                    QualificationLoyaltyCardResult(points = voucher.loyalty_card?.points ?: voucher.loyalty_card?.balance)
                } else null
            )

            val orderCalculated = if (includeOrder) {
                buildOrderCalculated(request, discountAmount, includeOrder)
            } else null

            val voucherEntity = if (includeRedeemable || includeCategory || includeValidationRules) {
                voucherService.getByCode(tenantName, redeemable.id)
            } else null
            val assignments = if (includeValidationRules) buildAssignments(tenantName, voucherEntity) else null
            val redeemableResult = ValidationRedeemableResponse(
                status = "APPLICABLE",
                id = redeemable.id,
                `object` = objectType,
                order = orderCalculated,
                applicable_to = QualificationItemList(),
                inapplicable_to = QualificationItemList(),
                result = resultPayload,
                metadata = if (includeRedeemable) voucher.metadata else null,
                categories = if (includeCategory) voucher.categories else null,
                campaign_name = if (includeRedeemable) voucherEntity?.campaign?.name else null,
                campaign_id = if (includeRedeemable) voucherEntity?.campaign?.id?.toString() else null,
                name = if (includeRedeemable) voucher.code else null,
                validation_rules_assignments = assignments
            )
            redeemables += redeemableResult
            applicableCount++
            if (context.categoryInfos.isNotEmpty()) {
                context.categoryInfos.forEach { info ->
                    categoryCounts[info.idToken] = categoryCounts.getOrDefault(info.idToken, 0) + 1
                    if (isExclusiveCategory(info, exclusiveCategoryTokens)) {
                        exclusiveCategoryCounts[info.idToken] = exclusiveCategoryCounts.getOrDefault(info.idToken, 0) + 1
                    }
                }
                if (context.categoryInfos.any { isExclusiveCategory(it, exclusiveCategoryTokens) }) {
                    exclusiveCount++
                    hasExclusiveApplied = true
                }
            }
        }

        val totalDiscount = appliedDiscounts.sum()
        val orderCalculated = buildOrderCalculated(request, totalDiscount, includeOrder)
        val valid = when (applicationMode) {
            "ALL" -> inapplicable.isEmpty() && skipped.isEmpty()
            else -> applicableCount > 0
        }

        session = sessionLockService.createLocks(tenantName, session, redeemables)

        return ValidationsValidateResponse(
            id = "valid_${UUID.randomUUID().toString().replace("-", "")}",
            valid = valid,
            redeemables = redeemables,
            skipped_redeemables = skipped.ifEmpty { null },
            inapplicable_redeemables = inapplicable.ifEmpty { null },
            order = orderCalculated,
            tracking_id = trackingId,
            session = session,
            stacking_rules = stackingRules
        )
    }

    private fun buildOrderCalculated(request: ValidationStackRequest, discountAmount: Long, includeItems: Boolean): ValidationOrderCalculated? {
        val order = request.order ?: return null
        val amount = order.amount ?: 0L
        val totalAmount = amount - discountAmount
        val items = if (includeItems) {
            order.items?.map { item ->
                val itemAmount = (item.price ?: 0L) * (item.quantity ?: 0)
                ValidationOrderItemResponse(
                    product_id = item.product_id ?: item.product?.id,
                    sku_id = item.sku_id ?: item.sku?.id,
                    quantity = item.quantity,
                    amount = itemAmount,
                    subtotal_amount = itemAmount,
                    price = item.price,
                    metadata = item.metadata,
                    discount_amount = 0L,
                    application_details = emptyList()
                )
            }
        } else null
        return ValidationOrderCalculated(
            amount = amount,
            initial_amount = amount,
            discount_amount = discountAmount,
            items_discount_amount = 0L,
            total_discount_amount = discountAmount,
            total_amount = totalAmount,
            applied_discount_amount = discountAmount,
            items_applied_discount_amount = 0L,
            total_applied_discount_amount = discountAmount,
            items = items,
            metadata = order.metadata
        )
    }

    private fun buildInapplicable(
        redeemable: org.wahlen.voucherengine.api.dto.request.RedeemableDto,
        error: ValidationErrorDetail,
        objectType: String,
        includeRedeemable: Boolean = false,
        includeCategory: Boolean = false,
        tenantName: String = "",
        validationRuleId: String? = null,
        validationRulesAssignments: ValidationRulesAssignmentsListResponse? = null
    ): ValidationRedeemableResponse {
        val voucher = if ((includeRedeemable || includeCategory) && tenantName.isNotBlank()) {
            voucherService.getByCode(tenantName, redeemable.id)
        } else null
        return ValidationRedeemableResponse(
            status = "INAPPLICABLE",
            id = redeemable.id,
            `object` = objectType,
            result = ValidationRedeemableResultPayload(error = error),
            metadata = if (includeRedeemable) voucher?.metadata else null,
            categories = if (includeCategory) voucher?.categories?.map { org.wahlen.voucherengine.api.dto.response.CategoryResponse(it.id, it.name, it.createdAt, it.updatedAt) } else null,
            campaign_name = if (includeRedeemable) voucher?.campaign?.name else null,
            campaign_id = if (includeRedeemable) voucher?.campaign?.id?.toString() else null,
            name = if (includeRedeemable) voucher?.code else null,
            validation_rule_id = validationRuleId,
            validation_rules_assignments = validationRulesAssignments
        )
    }

    private fun buildSkipped(
        redeemable: org.wahlen.voucherengine.api.dto.request.RedeemableDto,
        key: String,
        message: String,
        objectType: String
    ): ValidationRedeemableResponse =
        ValidationRedeemableResponse(
            status = "SKIPPED",
            id = redeemable.id,
            `object` = objectType,
            result = ValidationRedeemableResultPayload(details = ValidationSkippedDetails(key, message))
        )

    private fun buildAssignments(
        tenantName: String,
        voucher: org.wahlen.voucherengine.persistence.model.voucher.Voucher?
    ): ValidationRulesAssignmentsListResponse? {
        if (voucher == null) return null
        val assignments = mutableListOf<org.wahlen.voucherengine.persistence.model.validation.ValidationRulesAssignment>()
        voucher.id?.let { id ->
            assignments += validationRulesAssignmentRepository.findByRelatedObjectIdAndRelatedObjectTypeAndTenantName(id.toString(), "voucher", tenantName)
        }
        voucher.code?.let { code ->
            assignments += validationRulesAssignmentRepository.findByRelatedObjectIdAndRelatedObjectTypeAndTenantName(code, "voucher", tenantName)
        }
        if (assignments.isEmpty()) return null
        val unique = assignments.distinctBy { it.id }
        val data = unique.map {
            ValidationRuleAssignmentResponse(
                id = it.id,
                rule_id = it.ruleId,
                related_object_id = it.relatedObjectId,
                related_object_type = it.relatedObjectType,
                validation_status = it.validationStatus,
                validation_omitted_rules = it.omittedRules,
                created_at = it.createdAt,
                updated_at = it.updatedAt
            )
        }
        return ValidationRulesAssignmentsListResponse(data = data, total = data.size)
    }

    private fun orderRedeemables(
        tenantName: String,
        request: ValidationStackRequest,
        sortingRule: String?
    ): List<RedeemableContext> {
        val contexts = request.redeemables.mapIndexed { index, redeemable ->
            val voucher = voucherService.getByCode(tenantName, redeemable.id)
            val categoryInfos = voucher?.categories?.mapNotNull { category ->
                val id = category.id ?: return@mapNotNull null
                CategoryInfo(id = id, idToken = id.toString().lowercase(), nameToken = category.name?.lowercase())
            } ?: emptyList()
            val categoryIds = categoryInfos.map { it.id }
            val categories = categoryInfos.map { it.idToken }
            val categoryNames = categoryInfos.mapNotNull { it.nameToken }
            RedeemableContext(redeemable, index, categories, categoryIds, categoryNames, categoryInfos)
        }
        return if (sortingRule?.uppercase() == "CATEGORY_HIERARCHY") {
            contexts.sortedWith(compareBy<RedeemableContext> { it.sortKey }.thenBy { it.index })
        } else {
            contexts.sortedBy { it.index }
        }
    }

    private data class RedeemableContext(
        val redeemable: org.wahlen.voucherengine.api.dto.request.RedeemableDto,
        val index: Int,
        val categories: List<String>,
        val categoryIds: List<UUID>,
        val categoryNames: List<String>,
        val categoryInfos: List<CategoryInfo>
    ) {
        val sortKey: String = categoryNames.minOrNull()
            ?: categories.minOrNull()
            ?: "zzzz"
    }

    private data class CategoryInfo(
        val id: UUID,
        val idToken: String,
        val nameToken: String?
    )

    private fun isExclusiveCategory(
        info: CategoryInfo,
        exclusiveTokens: Set<String>
    ): Boolean =
        exclusiveTokens.contains(info.idToken) || (info.nameToken != null && exclusiveTokens.contains(info.nameToken))

    private fun isJointCategory(
        info: CategoryInfo,
        jointTokens: Set<String>
    ): Boolean =
        jointTokens.contains(info.idToken) || (info.nameToken != null && jointTokens.contains(info.nameToken))

    private fun defaultStackingRules(): StackingRulesResponse =
        StackingRulesResponse(
            redeemables_limit = stackingRulesProperties.redeemablesLimit,
            applicable_redeemables_limit = stackingRulesProperties.applicableRedeemablesLimit,
            applicable_redeemables_per_category_limit = stackingRulesProperties.applicableRedeemablesPerCategoryLimit,
            applicable_redeemables_category_limits = stackingRulesProperties.applicableRedeemablesCategoryLimits,
            applicable_exclusive_redeemables_limit = stackingRulesProperties.applicableExclusiveRedeemablesLimit,
            applicable_exclusive_redeemables_per_category_limit = stackingRulesProperties.applicableExclusiveRedeemablesPerCategoryLimit,
            exclusive_categories = stackingRulesProperties.exclusiveCategories.ifEmpty { null },
            joint_categories = stackingRulesProperties.jointCategories.ifEmpty { null },
            redeemables_application_mode = stackingRulesProperties.redeemablesApplicationMode,
            redeemables_sorting_rule = stackingRulesProperties.redeemablesSortingRule,
            redeemables_products_application_mode = stackingRulesProperties.redeemablesProductsApplicationMode,
            redeemables_no_effect_rule = stackingRulesProperties.redeemablesNoEffectRule,
            no_effect_skip_categories = stackingRulesProperties.noEffectSkipCategories.ifEmpty { null },
            no_effect_redeem_anyway_categories = stackingRulesProperties.noEffectRedeemAnywayCategories.ifEmpty { null },
            redeemables_rollback_order_mode = stackingRulesProperties.redeemablesRollbackOrderMode
        )

    private fun buildSession(session: SessionDto?): SessionDto? {
        if (session?.type?.uppercase() != "LOCK") return null
        val key = session.key ?: "sess_${UUID.randomUUID()}"
        return session.copy(key = key)
    }

    private fun matchesRedeemableType(objectType: String, voucherType: VoucherType): Boolean =
        when (objectType) {
            "gift_card" -> voucherType == VoucherType.GIFT_VOUCHER
            "loyalty_card" -> voucherType == VoucherType.LOYALTY_CARD
            else -> voucherType == VoucherType.DISCOUNT_VOUCHER || voucherType == VoucherType.GIFT_VOUCHER || voucherType == VoucherType.LOYALTY_CARD
        }

    private fun hashTrackingId(source: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(source.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
