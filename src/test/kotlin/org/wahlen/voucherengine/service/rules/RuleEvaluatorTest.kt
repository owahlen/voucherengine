package org.wahlen.voucherengine.service.rules

import org.junit.jupiter.api.Test
import org.wahlen.voucherengine.api.dto.request.OrderItemDto
import org.wahlen.voucherengine.api.dto.request.OrderRequest
import org.wahlen.voucherengine.api.dto.request.VoucherValidationRequest
import org.wahlen.voucherengine.persistence.model.campaign.Campaign
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleEvaluatorTest {

    private val customer = Customer(
        sourceId = "cust_1",
        email = "alice@example.com",
        metadata = mapOf("tier" to "gold")
    )

    private val campaign = Campaign(
        name = "Winter",
        metadata = mapOf("region" to "EU")
    )

    private val voucher = Voucher(
        code = "WELCOME10",
        metadata = mapOf("source" to "referral"),
        campaign = campaign
    )

    private fun ctx(order: OrderRequest? = null, total: Int = 0, perCustomer: Int = 0): RuleEvaluator.Context =
        RuleEvaluator.Context(
            voucher = voucher,
            customer = customer,
            request = VoucherValidationRequest(order = order),
            totalRedemptions = total,
            perCustomerRedemptions = perCustomer
        )

    @Test
    fun `numeric and string operators pass`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "order.amount", "conditions" to mapOf("\$gte" to 1000)),
                "2" to mapOf("name" to "order.currency", "conditions" to mapOf("\$eq" to "EUR")),
                "3" to mapOf("name" to "customer.email", "conditions" to mapOf("\$contains" to "@example.com"))
            ),
            "logic" to "1 and 2 and 3"
        )
        val order = OrderRequest(id = "o1", amount = 1500, currency = "EUR")
        assertTrue(RuleEvaluator.evaluate(rules, ctx(order)))
    }

    @Test
    fun `order item sku containment works`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "order.items.sku", "conditions" to mapOf("\$contains_any" to listOf("SKU_A")))
            ),
            "logic" to "1"
        )
        val order = OrderRequest(
            id = "o2",
            amount = 500,
            items = listOf(OrderItemDto(sku_id = "SKU_A", quantity = 1, price = 500))
        )
        assertTrue(RuleEvaluator.evaluate(rules, ctx(order)))

        val orderNoMatch = OrderRequest(
            id = "o3",
            amount = 500,
            items = listOf(OrderItemDto(sku_id = "SKU_B", quantity = 1, price = 500))
        )
        assertFalse(RuleEvaluator.evaluate(rules, ctx(orderNoMatch)))
    }

    @Test
    fun `metadata and campaign rules evaluate`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "voucher.metadata.source", "conditions" to mapOf("\$eq" to "referral")),
                "2" to mapOf("name" to "campaign.metadata.region", "conditions" to mapOf("\$is" to listOf("EU", "US")))
            ),
            "logic" to "1 and 2"
        )
        assertTrue(RuleEvaluator.evaluate(rules, ctx()))
    }

    @Test
    fun `redemption counters are enforced`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "redemptions.count.total", "conditions" to mapOf("\$lt" to 5)),
                "2" to mapOf("name" to "redemptions.count.per_customer", "conditions" to mapOf("\$lt" to 2))
            ),
            "logic" to "1 and 2"
        )
        assertTrue(RuleEvaluator.evaluate(rules, ctx(total = 1, perCustomer = 1)))
        assertFalse(RuleEvaluator.evaluate(rules, ctx(total = 5, perCustomer = 1)))
        assertFalse(RuleEvaluator.evaluate(rules, ctx(total = 1, perCustomer = 2)))
    }

    @Test
    fun `redemption metadata conditions are evaluated`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "redemptions.metadata.channel", "conditions" to mapOf("\$eq" to "web"))
            ),
            "logic" to "1"
        )
        val ctxWithMetadata = RuleEvaluator.Context(
            voucher = voucher,
            customer = customer,
            request = VoucherValidationRequest(metadata = mapOf("channel" to "web")),
            totalRedemptions = 0,
            perCustomerRedemptions = 0
        )
        val ctxWithoutMetadata = RuleEvaluator.Context(
            voucher = voucher,
            customer = customer,
            request = VoucherValidationRequest(metadata = mapOf("channel" to "store")),
            totalRedemptions = 0,
            perCustomerRedemptions = 0
        )

        assertTrue(RuleEvaluator.evaluate(rules, ctxWithMetadata))
        assertFalse(RuleEvaluator.evaluate(rules, ctxWithoutMetadata))
    }

    @Test
    fun `allowed rule prefixes ignore non-customer rules`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "order.amount", "conditions" to mapOf("\$gte" to 500)),
                "2" to mapOf("name" to "customer.email", "conditions" to mapOf("\$contains" to "@example.com"))
            ),
            "logic" to "1 and 2"
        )
        val result = RuleEvaluator.evaluate(
            rules,
            ctx(order = null),
            allowedRulePrefixes = setOf("customer.")
        )
        assertTrue(result)
    }

    @Test
    fun `logic expressions short circuit`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "order.amount", "conditions" to mapOf("\$gte" to 1000)),
                "2" to mapOf("name" to "order.items.sku", "conditions" to mapOf("\$contains_any" to listOf("SKU_X"))),
                "3" to mapOf("name" to "customer.metadata.tier", "conditions" to mapOf("\$eq" to "gold"))
            ),
            "logic" to "(1 and 2) or 3"
        )
        val order = OrderRequest(
            id = "o4",
            amount = 200,
            items = listOf(OrderItemDto(sku_id = "SKU_A", quantity = 1, price = 200))
        )
        // (false and false) or true => true
        assertTrue(RuleEvaluator.evaluate(rules, ctx(order)))
    }

    @Test
    fun `negative operators and booleans`() {
        val rules = mapOf(
            "rules" to mapOf(
                "1" to mapOf("name" to "customer.email", "conditions" to mapOf("\$ne" to "bob@example.com")),
                "2" to mapOf("name" to "order.items.sku", "conditions" to mapOf("\$contains_all" to listOf("SKU_A", "SKU_B"))),
                "3" to mapOf("name" to "voucher.metadata.source", "conditions" to mapOf("\$is_not" to listOf("other"))),
                "4" to mapOf("name" to "customer.metadata.marketing_opt_in", "conditions" to mapOf("\$true" to true)),
                "5" to mapOf("name" to "order.metadata.is_test", "conditions" to mapOf("\$false" to false))
            ),
            "logic" to "1 and 2 and 3 and 4 and 5"
        )

        val enrichedCustomer = Customer(
            sourceId = customer.sourceId,
            email = customer.email,
            metadata = mapOf("marketing_opt_in" to true)
        )

        val enrichedCtx = RuleEvaluator.Context(
            voucher = voucher,
            customer = enrichedCustomer,
            request = VoucherValidationRequest(
                order = OrderRequest(
                    id = "o5",
                    amount = 100,
                    items = listOf(
                        OrderItemDto(sku_id = "SKU_A", quantity = 1, price = 50),
                        OrderItemDto(sku_id = "SKU_B", quantity = 1, price = 50)
                    ),
                    metadata = mapOf("is_test" to false)
                )
            ),
            totalRedemptions = 0,
            perCustomerRedemptions = 0
        )

        assertTrue(RuleEvaluator.evaluate(rules, enrichedCtx))
    }
}
