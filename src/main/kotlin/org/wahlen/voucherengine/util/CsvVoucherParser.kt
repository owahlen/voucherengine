package org.wahlen.voucherengine.util

import org.wahlen.voucherengine.api.dto.request.VoucherCreateRequest
import org.wahlen.voucherengine.api.dto.common.DiscountDto
import org.wahlen.voucherengine.api.dto.common.DiscountType
import java.io.BufferedReader
import java.io.StringReader

/**
 * Utility for parsing CSV voucher data.
 * 
 * Expected CSV format:
 * code,type,discount_type,discount_value,active
 * VOUCHER1,DISCOUNT_VOUCHER,AMOUNT,1000,true
 * VOUCHER2,DISCOUNT_VOUCHER,PERCENT,10,true
 */
object CsvVoucherParser {

    fun parseCsv(csvContent: String): List<VoucherCreateRequest> {
        val reader = BufferedReader(StringReader(csvContent))
        val vouchers = mutableListOf<VoucherCreateRequest>()
        
        // Read header line
        val header = reader.readLine() ?: throw IllegalArgumentException("CSV is empty")
        val headers = header.split(",").map { it.trim() }
        
        // Validate required headers
        require("code" in headers) { "CSV must have 'code' column" }
        require("type" in headers) { "CSV must have 'type' column" }
        
        // Read data lines
        reader.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            
            val values = line.split(",").map { it.trim() }
            if (values.size != headers.size) {
                throw IllegalArgumentException("CSV line has ${values.size} columns but header has ${headers.size}")
            }
            
            val row = headers.zip(values).toMap()
            
            val voucher = VoucherCreateRequest(
                code = row["code"],
                type = row["type"] ?: "DISCOUNT_VOUCHER",
                discount = if (row.containsKey("discount_type") && row.containsKey("discount_value")) {
                    DiscountDto(
                        type = DiscountType.valueOf(row["discount_type"] ?: "AMOUNT"),
                        amount_off = if (row["discount_type"] == "AMOUNT") row["discount_value"]?.toLongOrNull() else null,
                        percent_off = if (row["discount_type"] == "PERCENT") row["discount_value"]?.toIntOrNull() else null
                    )
                } else null,
                active = row["active"]?.toBoolean() ?: true,
                metadata = emptyMap()
            )
            
            vouchers.add(voucher)
        }
        
        return vouchers
    }
}
