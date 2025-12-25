package org.wahlen.voucherengine.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Duration
import java.time.LocalDate
import java.util.*

/**
 * Service for S3 file operations (upload, download, pre-signed URLs).
 */
@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Value("\${aws.s3.bucket.exports:voucherengine-exports}") private val exportsBucket: String,
    @Value("\${aws.s3.bucket.imports:voucherengine-imports}") private val importsBucket: String,
    @Value("\${aws.s3.presigned-url-duration:PT1H}") private val presignedUrlDuration: Duration
) {

    /**
     * Upload export file to S3 and return pre-signed download URL.
     */
    fun uploadExport(tenantName: String, fileName: String, content: ByteArray, contentType: String): String {
        val key = buildExportKey(tenantName, fileName)
        
        val putRequest = PutObjectRequest.builder()
            .bucket(exportsBucket)
            .key(key)
            .contentType(contentType)
            .build()

        s3Client.putObject(putRequest, RequestBody.fromBytes(content))

        return generatePresignedUrl(exportsBucket, key)
    }

    /**
     * Download import file from S3.
     */
    fun downloadImport(tenantName: String, fileName: String): ByteArray {
        val key = buildImportKey(tenantName, fileName)
        
        val getRequest = GetObjectRequest.builder()
            .bucket(importsBucket)
            .key(key)
            .build()

        return s3Client.getObject(getRequest).use { response ->
            response.readAllBytes()
        }
    }

    /**
     * Generate pre-signed URL for downloading a file from S3.
     */
    fun generatePresignedUrl(bucket: String, key: String): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(presignedUrlDuration)
            .getObjectRequest(getObjectRequest)
            .build()

        val presignedRequest = s3Presigner.presignGetObject(presignRequest)
        return presignedRequest.url().toString()
    }

    /**
     * Build S3 key for export files: {tenant}/exports/{year}/{month}/{fileName}
     */
    private fun buildExportKey(tenantName: String, fileName: String): String {
        val now = LocalDate.now()
        return "$tenantName/exports/${now.year}/${now.monthValue.toString().padStart(2, '0')}/$fileName"
    }

    /**
     * Build S3 key for import files: {tenant}/imports/{year}/{month}/{fileName}
     */
    private fun buildImportKey(tenantName: String, fileName: String): String {
        val now = LocalDate.now()
        return "$tenantName/imports/${now.year}/${now.monthValue.toString().padStart(2, '0')}/$fileName"
    }
}
