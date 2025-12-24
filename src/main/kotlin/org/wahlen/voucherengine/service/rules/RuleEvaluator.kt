package org.wahlen.voucherengine.service.rules

import org.wahlen.voucherengine.api.dto.request.OrderRequest
import org.wahlen.voucherengine.api.dto.request.VoucherValidationRequest
import org.wahlen.voucherengine.persistence.model.customer.Customer
import org.wahlen.voucherengine.persistence.model.voucher.Voucher
import java.math.BigDecimal

/**
 * Evaluates Voucherify-style validation rules (see docs/voucherify-rule-semantics.md).
 *
 * Rules are boolean predicates over a runtime context (voucher, customer, order, redemptions).
 */
object RuleEvaluator {

    data class Context(
        val voucher: Voucher,
        val customer: Customer?,
        val request: VoucherValidationRequest,
        val totalRedemptions: Int,
        val perCustomerRedemptions: Int
    )

    /**
        * Evaluate a rule payload as stored in ValidationRule.rules.
        *
        * Supported forms:
        * - {"rules": { "1": { "name": "...", "conditions": { "$gte": 100 } }, ...}, "logic": "1 and 2"}
        * - {"redemptions": { "quantity": 1, "per_customer": 1 }} (legacy)
        */
    fun evaluate(
        rulePayload: Map<String, Any?>,
        ctx: Context,
        allowedRulePrefixes: Set<String>? = null
    ): Boolean {
        // legacy redemptions-only shortcut
        if (rulePayload.containsKey("redemptions")) {
            if (allowedRulePrefixes != null && "redemptions." !in allowedRulePrefixes) {
                return true
            }
            val redemptions = rulePayload["redemptions"] as? Map<*, *> ?: return false
            val qty = (redemptions["quantity"] as? Number)?.toInt()
            val perCustomer = (redemptions["per_customer"] as? Number)?.toInt()
            if (qty != null && ctx.totalRedemptions >= qty) return false
            if (perCustomer != null && ctx.perCustomerRedemptions >= perCustomer) return false
            return true
        }

        val rulesMap = rulePayload["rules"] as? Map<*, *> ?: return false
        val logic = rulePayload["logic"] as? String

        val ruleResults = mutableMapOf<String, Boolean>()
        rulesMap.forEach { (id, value) ->
            val stringId = id?.toString() ?: return@forEach
            val map = value as? Map<*, *> ?: return@forEach
            val name = map["name"] as? String ?: return@forEach
            if (allowedRulePrefixes != null && allowedRulePrefixes.none { name.startsWith(it) }) {
                ruleResults[stringId] = true
                return@forEach
            }
            val conditions = map["conditions"] as? Map<*, *> ?: return@forEach
            ruleResults[stringId] = evaluateSingle(name, conditions, ctx)
        }

        if (logic.isNullOrBlank()) {
            // default AND of all rules
            return ruleResults.values.all { it }
        }
        return evaluateLogicExpression(logic, ruleResults)
    }

    private fun evaluateSingle(name: String, conditions: Map<*, *>, ctx: Context): Boolean {
        if (conditions.size != 1) return false
        val op = conditions.keys.firstOrNull()?.toString() ?: return false
        val operand = conditions.values.firstOrNull()

        return when {
            name.startsWith("customer.") -> evaluateCustomerRule(name.removePrefix("customer."), op, operand, ctx)
            name.startsWith("order.items.") -> evaluateOrderItemRule(name.removePrefix("order.items."), op, operand, ctx.request.order)
            name.startsWith("order.") -> evaluateOrderRule(name.removePrefix("order."), op, operand, ctx.request.order)
            name.startsWith("voucher.") -> evaluateVoucherRule(name.removePrefix("voucher."), op, operand, ctx.voucher)
            name.startsWith("campaign.") -> evaluateCampaignRule(name.removePrefix("campaign."), op, operand, ctx.voucher)
            name.startsWith("redemptions.") -> evaluateRedemptionRule(name.removePrefix("redemptions."), op, operand, ctx)
            else -> false
        }
    }

    private fun evaluateCustomerRule(path: String, op: String, operand: Any?, ctx: Context): Boolean {
        val customer = ctx.customer ?: return false
        val value: Any? = when {
            path == "id" -> customer.sourceId ?: customer.id?.toString()
            path == "email" -> customer.email
            path == "segment" -> null // not modeled; will fail
            path.startsWith("metadata.") -> customer.metadata?.get(path.removePrefix("metadata."))
            else -> null
        }
        return evaluateCondition(value, op, operand)
    }

    private fun evaluateOrderRule(path: String, op: String, operand: Any?, order: OrderRequest?): Boolean {
        val value: Any? = when (path) {
            "amount" -> order?.amount
            "currency" -> order?.currency
            "items.count" -> order?.items?.size
            else -> if (path.startsWith("metadata.")) order?.metadata?.get(path.removePrefix("metadata.")) else null
        }
        return evaluateCondition(value, op, operand)
    }

    private fun evaluateOrderItemRule(path: String, op: String, operand: Any?, order: OrderRequest?): Boolean {
        val items = order?.items ?: return false
        if (path == "sku") {
            val skus = items.mapNotNull { it.sku_id }
            return evaluateCondition(skus, op, operand)
        }
        if (path == "product") {
            val products = items.mapNotNull { it.product_id }
            return evaluateCondition(products, op, operand)
        }
        return items.any { item ->
            val value: Any? = when (path) {
                "quantity" -> item.quantity
                "price" -> item.price
                else -> if (path.startsWith("metadata.")) null else null
            }
            evaluateCondition(value, op, operand)
        }
    }

    private fun evaluateVoucherRule(path: String, op: String, operand: Any?, voucher: Voucher): Boolean {
        val value: Any? = when {
            path == "code" -> voucher.code
            path.startsWith("metadata.") -> voucher.metadata?.get(path.removePrefix("metadata."))
            else -> null
        }
        return evaluateCondition(value, op, operand)
    }

    private fun evaluateCampaignRule(path: String, op: String, operand: Any?, voucher: Voucher): Boolean {
        val campaign = voucher.campaign ?: return false
        val value: Any? = when {
            path == "id" -> campaign.id?.toString()
            path.startsWith("metadata.") -> campaign.metadata?.get(path.removePrefix("metadata."))
            else -> null
        }
        return evaluateCondition(value, op, operand)
    }

    private fun evaluateRedemptionRule(path: String, op: String, operand: Any?, ctx: Context): Boolean {
        val value: Any? = when {
            path == "count.total" -> ctx.totalRedemptions
            path == "count.per_customer" -> ctx.perCustomerRedemptions
            path.startsWith("metadata.") -> ctx.request.metadata?.get(path.removePrefix("metadata."))
            else -> null
        }
        return evaluateCondition(value, op, operand)
    }

    private fun evaluateCondition(value: Any?, op: String, operand: Any?): Boolean {
        return when (op) {
            "\$eq" -> value != null && valuesEqual(value, operand)
            "\$ne" -> value != null && !valuesEqual(value, operand)
            "\$gt" -> numericCompare(value, operand) { a, b -> a > b }
            "\$gte" -> numericCompare(value, operand) { a, b -> a >= b }
            "\$lt" -> numericCompare(value, operand) { a, b -> a < b }
            "\$lte" -> numericCompare(value, operand) { a, b -> a <= b }
            "\$is" -> {
                val list = operand as? Collection<*> ?: return false
                value != null && list.any { valuesEqual(value, it) }
            }
            "\$is_not" -> {
                val list = operand as? Collection<*> ?: return false
                value != null && list.none { valuesEqual(value, it) }
            }
            "\$contains" -> when (value) {
                is String -> operand is String && value.contains(operand)
                is Collection<*> -> operand != null && value.any { valuesEqual(it, operand) }
                else -> false
            }
            "\$contains_any" -> {
                val list = operand as? Collection<*> ?: return false
                val valueList = when (value) {
                    is Collection<*> -> value
                    else -> return false
                }
                valueList.any { candidate -> list.any { valuesEqual(candidate, it) } }
            }
            "\$contains_all" -> {
                val list = operand as? Collection<*> ?: return false
                val valueList = when (value) {
                    is Collection<*> -> value
                    else -> return false
                }
                list.all { required -> valueList.any { valuesEqual(it, required) } }
            }
            "\$true" -> value == true
            "\$false" -> value == false
            else -> false
        }
    }

    private fun valuesEqual(a: Any?, b: Any?): Boolean {
        return when {
            a is Number && b is Number -> toBigDecimal(a) == toBigDecimal(b)
            else -> a == b
        }
    }

    private fun numericCompare(value: Any?, operand: Any?, cmp: (BigDecimal, BigDecimal) -> Boolean): Boolean {
        if (value !is Number || operand !is Number) return false
        return cmp(toBigDecimal(value), toBigDecimal(operand))
    }

    private fun toBigDecimal(n: Number): BigDecimal = when (n) {
        is BigDecimal -> n
        is Double, is Float -> BigDecimal((n as Number).toDouble())
        else -> BigDecimal(n.toLong())
    }

    private fun evaluateLogicExpression(expression: String, results: Map<String, Boolean>): Boolean {
        val tokens = tokenize(expression)
        var index = 0

        lateinit var parseExpression: () -> Boolean
        lateinit var parseTerm: () -> Boolean

        parseTerm = {
            if (index >= tokens.size) {
                false
            } else {
                when (val token = tokens[index++]) {
                    "(" -> {
                        val inner = parseExpression()
                        if (index < tokens.size && tokens[index] == ")") index++
                        inner
                    }
                    ")" -> false
                    else -> results[token] ?: false
                }
            }
        }

        parseExpression = {
            var result = parseTerm()
            while (index < tokens.size) {
                val op = tokens[index]
                if (op != "and" && op != "or") break
                index++
                val rhs = parseTerm()
                result = if (op == "and") result && rhs else result || rhs
            }
            result
        }

        return parseExpression()
    }

    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        expr.forEach { ch ->
            when (ch) {
                ' ' -> {
                    if (current.isNotEmpty()) {
                        tokens += current.toString()
                        current = StringBuilder()
                    }
                }
                '(' , ')' -> {
                    if (current.isNotEmpty()) {
                        tokens += current.toString()
                        current = StringBuilder()
                    }
                    tokens += ch.toString()
                }
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) tokens += current.toString()
        return tokens
    }
}
