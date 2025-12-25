package org.wahlen.voucherengine.testinfra

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import org.junit.jupiter.api.extension.*
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JUnit 5 extension that starts one embedded S3Mock instance once per JVM test run.
 *
 * Usage:
 *
 * @ExtendWith(S3MockExtension::class)
 * @SpringBootTest
 * class SomeIT {
 *   // S3Mock is running and accessible
 * }
 *
 * Or use the @S3IntegrationTest meta-annotation which includes this extension.
 */
class S3MockExtension : BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private val logger = LoggerFactory.getLogger(S3MockExtension::class.java)

    override fun beforeAll(context: ExtensionContext) {
        ensureStarted()
    }

    override fun beforeEach(context: ExtensionContext) {
        ensureStarted()
        if (clearBucketsBeforeEach) {
            logger.debug("Clearing all buckets before test: {}", context.displayName)
            clearAllBuckets()
        }
    }

    override fun afterAll(context: ExtensionContext) {
        // Intentionally no stop: keep S3Mock running for the whole JVM for speed.
    }

    companion object {
        private val logger = LoggerFactory.getLogger(S3MockExtension::class.java)
        private val STARTED = AtomicBoolean(false)
        private val SHUTDOWN_HOOK_ADDED = AtomicBoolean(false)

        @Volatile
        private var container: S3MockContainer? = null

        @Volatile
        private var s3: S3Client? = null

        @Volatile
        private var endpoint: String? = null

        @Volatile
        private var region: Region = Region.EU_CENTRAL_1  // Match ElasticMQ and production config

        @Volatile
        private var clearBucketsBeforeEach: Boolean = false  // Changed default to false

        /**
         * Initial buckets to create when S3Mock starts.
         * Can be customized via setInitialBuckets() before extension starts.
         */
        @Volatile
        private var initialBuckets: List<String> = listOf("voucherengine-imports", "voucherengine-exports")

        /**
         * Enable/disable bucket clearing before each test.
         * Call this early (e.g., in a static initializer) if you want to change the default.
         */
        @JvmStatic
        fun setClearBucketsBeforeEach(enabled: Boolean) {
            clearBucketsBeforeEach = enabled
        }

        /**
         * Set initial buckets to create on startup.
         * Call this before the extension starts (e.g., in a static initializer).
         */
        @JvmStatic
        fun setInitialBuckets(buckets: List<String>) {
            if (STARTED.get()) {
                throw IllegalStateException("Cannot set initial buckets after S3Mock has started")
            }
            initialBuckets = buckets
        }

        /** Optionally override AWS region used by the client. Call early. */
        @JvmStatic
        fun setRegion(newRegion: Region) {
            region = newRegion
        }

        /**
         * Endpoint of embedded S3Mock (starts lazily if needed).
         */
        @JvmStatic
        fun endpoint(): String {
            ensureStarted()
            return endpoint!!
        }

        /**
         * S3Client bound to embedded S3Mock (starts lazily if needed).
         */
        @JvmStatic
        fun s3Client(): S3Client {
            ensureStarted()
            return s3!!
        }

        /**
         * Get list of configured initial buckets.
         */
        @JvmStatic
        fun bucketNames(): List<String> = initialBuckets

        /**
         * Clear all objects from all buckets.
         * Useful for test isolation without deleting buckets.
         */
        @JvmStatic
        fun clearAllBuckets() {
            val client = s3Client()
            val buckets = try {
                client.listBuckets().buckets().map { it.name() }
            } catch (e: Exception) {
                logger.warn("Failed to list buckets: {}", e.message)
                emptyList()
            }

            buckets.forEach { bucket ->
                try {
                    deleteAllObjectsInBucket(client, bucket)
                    logger.debug("Cleared bucket: {}", bucket)
                } catch (e: Exception) {
                    logger.warn("Failed to clear bucket {}: {}", bucket, e.message)
                }
            }
        }

        /**
         * Delete all objects in a specific bucket.
         */
        @JvmStatic
        fun clearBucket(bucketName: String) {
            val client = s3Client()
            try {
                deleteAllObjectsInBucket(client, bucketName)
                logger.debug("Cleared bucket: {}", bucketName)
            } catch (e: Exception) {
                logger.warn("Failed to clear bucket {}: {}", bucketName, e.message)
            }
        }

        /**
         * Delete and recreate all initial buckets.
         * Use this if you need completely fresh buckets for a test.
         */
        @JvmStatic
        fun resetBuckets() {
            val client = s3Client()
            
            // Delete all buckets
            try {
                val buckets = client.listBuckets().buckets().map { it.name() }
                buckets.forEach { bucket ->
                    try {
                        deleteAllObjectsInBucket(client, bucket)
                        client.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build())
                        logger.debug("Deleted bucket: {}", bucket)
                    } catch (e: Exception) {
                        logger.warn("Failed to delete bucket {}: {}", bucket, e.message)
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to list buckets for reset: {}", e.message)
            }

            // Recreate initial buckets
            initialBuckets.forEach { bucket ->
                try {
                    ensureBucketExists(client, bucket)
                    logger.debug("Recreated bucket: {}", bucket)
                } catch (e: Exception) {
                    logger.warn("Failed to recreate bucket {}: {}", bucket, e.message)
                }
            }
        }

        /**
         * Optional stop method.
         * Normally not needed as S3Mock will be stopped when JVM exits.
         */
        @JvmStatic
        @Synchronized
        fun stop() {
            try {
                s3?.close()
            } catch (_: Exception) {
            } finally {
                s3 = null
            }

            try {
                container?.stop()
            } catch (_: Exception) {
            } finally {
                container = null
            }

            endpoint = null
            STARTED.set(false)
            logger.info("S3Mock stopped")
        }

        @JvmStatic
        private fun ensureStarted() {
            if (STARTED.get()) return

            synchronized(S3MockExtension::class.java) {
                if (STARTED.get()) return

                try {
                    logger.info("Starting S3Mock container")

                    // Start S3Mock container
                    container = S3MockContainer("4.11.0")
                        .also { it.start() }

                    val endpointString = container!!.httpEndpoint

                    // Create S3 client
                    s3 = S3Client.builder()
                        .region(region)
                        .endpointOverride(URI.create(endpointString))
                        .credentialsProvider(
                            StaticCredentialsProvider.create(
                                AwsBasicCredentials.create("test", "test")
                            )
                        )
                        .forcePathStyle(true)
                        .build()

                    endpoint = endpointString

                    // Create initial buckets
                    initialBuckets.forEach { bucket ->
                        ensureBucketExists(s3!!, bucket)
                    }

                    addShutdownHookOnce()

                    STARTED.set(true)
                    logger.info("S3Mock container started successfully at {}", endpoint)
                } catch (e: Exception) {
                    logger.error("Failed to start S3Mock container", e)
                    // best-effort cleanup
                    try {
                        stop()
                    } catch (_: Exception) {
                    }
                    throw e
                }
            }
        }

        private fun addShutdownHookOnce() {
            if (SHUTDOWN_HOOK_ADDED.compareAndSet(false, true)) {
                Runtime.getRuntime().addShutdownHook(Thread {
                    try {
                        logger.info("Shutting down S3Mock container (JVM shutdown)")
                        stop()
                    } catch (e: Exception) {
                        logger.warn("Error while stopping S3Mock during shutdown: {}", e.message)
                    }
                })
            }
        }

        private fun ensureBucketExists(client: S3Client, bucketName: String) {
            try {
                client.headBucket { it.bucket(bucketName) }
                logger.debug("Bucket already exists: {}", bucketName)
            } catch (e: Exception) {
                client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build())
                logger.debug("Created bucket: {}", bucketName)
            }
        }

        private fun deleteAllObjectsInBucket(client: S3Client, bucketName: String) {
            var continuationToken: String? = null
            
            do {
                val listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .continuationToken(continuationToken)
                    .build()

                val listResponse = client.listObjectsV2(listRequest)
                val objects = listResponse.contents()

                if (objects.isNotEmpty()) {
                    val deleteRequest = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(
                            Delete.builder()
                                .objects(objects.map { obj ->
                                    ObjectIdentifier.builder().key(obj.key()).build()
                                })
                                .build()
                        )
                        .build()

                    client.deleteObjects(deleteRequest)
                }

                continuationToken = if (listResponse.isTruncated) {
                    listResponse.nextContinuationToken()
                } else {
                    null
                }
            } while (continuationToken != null)
        }
    }
}
