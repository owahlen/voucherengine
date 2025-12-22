package org.wahlen.voucherengine.api.controller

import org.springframework.data.domain.Sort

fun parseSort(order: String, mapping: Map<String, String>, defaultKey: String): Sort {
    val isDesc = order.startsWith("-")
    val key = order.removePrefix("-")
    val property = mapping[key] ?: mapping[defaultKey] ?: defaultKey
    return if (isDesc) Sort.by(property).descending() else Sort.by(property).ascending()
}
