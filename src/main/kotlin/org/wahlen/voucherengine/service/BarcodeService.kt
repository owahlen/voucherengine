package org.wahlen.voucherengine.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class BarcodeService {
    fun generateCode128Png(content: String, width: Int = 300, height: Int = 100): ByteArray {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.CODE_128, width, height)
        val out = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", out)
        return out.toByteArray()
    }
}
