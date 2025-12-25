package org.wahlen.voucherengine.testinfra

import org.elasticmq.rest.sqs.SQSRestServer
import org.elasticmq.rest.sqs.SQSRestServerBuilder
import org.junit.jupiter.api.extension.*
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JUnit 5 extension that starts one embedded ElasticMQ instance once per JVM test run,
 * creates configured queues, and optionally clears queues before each test.
 *
 * Usage - via @SqsIntegrationTest annotation (recommended):
 *
 * @SqsIntegrationTest
 * class MyAsyncTest {
 *     // ElasticMQ server and queues are automatically set up
 *     // Properties configured via ElasticMqPropertyInitializer
 * }
 *
 * Usage - direct (advanced):
 *
 * @ExtendWith(ElasticMqExtension::class)
 * @ContextConfiguration(initializers = [ElasticMqPropertyInitializer::class])
 * @SpringBootTest
 * class SomeIT {
 *     // Custom setup if needed
 * }
 */
class ElasticMqExtension : BeforeAllCallback, BeforeEachCallback, AfterAllCallback {

    private val logger = LoggerFactory.getLogger(ElasticMqExtension::class.java)

    override fun beforeAll(context: ExtensionContext) {
        ensureStarted()
    }

    override fun beforeEach(context: ExtensionContext) {
        ensureStarted()
        if (clearQueuesBeforeEach) {
            logger.debug("Purging all queues before test: {}", context.displayName)
            purgeAllQueues()
        }
    }

    override fun afterAll(context: ExtensionContext) {
        // Intentionally no stop: keep ElasticMQ running for the whole JVM for speed.
        // If you want to stop at the end of a specific test run, call ElasticMqExtension.stop() manually.
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ElasticMqExtension::class.java)
        private val STARTED = AtomicBoolean(false)
        private val SHUTDOWN_HOOK_ADDED = AtomicBoolean(false)

        @Volatile
        private var server: SQSRestServer? = null

        @Volatile
        private var sqs: SqsClient? = null

        @Volatile
        private var endpoint: String? = null

        @Volatile
        private var region: Region = Region.EU_CENTRAL_1

        @Volatile
        private var clearQueuesBeforeEach: Boolean = true

        /**
         * Initial queues to create when ElasticMQ starts.
         * Can be customized via setInitialQueues() before extension starts.
         */
        @Volatile
        private var initialQueues: List<String> = listOf("voucherengine-async-jobs", "voucherengine-async-jobs-dlq")

        /**
         * Set initial queues to create on startup.
         * Call this before the extension starts (e.g., in a static initializer).
         */
        @JvmStatic
        fun setInitialQueues(queues: List<String>) {
            if (STARTED.get()) {
                throw IllegalStateException("Cannot set initial queues after ElasticMQ has started")
            }
            initialQueues = queues
        }

        /**
         * Enable/disable queue clearing before each test.
         * Call this early (e.g., in a static initializer) if you want to change the default.
         */
        @JvmStatic
        fun setClearQueuesBeforeEach(enabled: Boolean) {
            clearQueuesBeforeEach = enabled
        }

        /** Optionally override AWS region used by the client. Call early. */
        @JvmStatic
        fun setRegion(newRegion: Region) {
            region = newRegion
        }

        /**
         * Endpoint of embedded ElasticMQ (starts lazily if needed).
         */
        @JvmStatic
        fun endpoint(): String {
            ensureStarted()
            return endpoint!!
        }

        /**
         * SqsClient bound to embedded ElasticMQ (starts lazily if needed).
         */
        @JvmStatic
        fun sqsClient(): SqsClient {
            ensureStarted()
            return sqs!!
        }

        /**
         * Purges messages from all queues.
         * Useful for test isolation without deleting queue configuration.
         */
        @JvmStatic
        fun purgeAllQueues() {
            val client = sqsClient()
            val urls = client.listQueues(ListQueuesRequest.builder().build()).queueUrls() ?: emptyList()
            urls.forEach { url ->
                try {
                    client.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build())
                    logger.debug("Purged queue: {}", url)
                } catch (e: Exception) {
                    logger.warn("Failed to purge queue {}: {}", url, e.message)
                }
            }
        }

        /**
         * Deletes all queues (stronger isolation).
         * Use this if you need completely fresh queues for a test.
         */
        @JvmStatic
        fun deleteAllQueues() {
            val client = sqsClient()
            val urls = client.listQueues(ListQueuesRequest.builder().build()).queueUrls() ?: emptyList()
            urls.forEach { url ->
                try {
                    client.deleteQueue(DeleteQueueRequest.builder().queueUrl(url).build())
                    logger.debug("Deleted queue: {}", url)
                } catch (e: Exception) {
                    logger.warn("Failed to delete queue {}: {}", url, e.message)
                }
            }
        }

        /**
         * Optional stop method.
         * Normally not needed as ElasticMQ will be stopped when JVM exits.
         */
        @JvmStatic
        @Synchronized
        fun stop() {
            try {
                sqs?.close()
            } catch (_: Exception) {
            } finally {
                sqs = null
            }

            try {
                server?.stopAndWait()
            } catch (_: Exception) {
            } finally {
                server = null
            }

            endpoint = null
            STARTED.set(false)
            logger.info("ElasticMQ stopped")
        }

        @JvmStatic
        private fun ensureStarted() {
            if (STARTED.get()) return

            synchronized(ElasticMqExtension::class.java) {
                if (STARTED.get()) return

                val configuredPort = System.getProperty("test.elasticmq.port")?.toIntOrNull()
                val port = configuredPort ?: findFreePort()

                try {
                    logger.info("Starting ElasticMQ server on localhost:{}", port)

                    // Start REST server
                    server = SQSRestServerBuilder
                        .withPort(port)
                        .withInterface("localhost")
                        .start()

                    val endpointString = "http://localhost:$port"

                    sqs = SqsClient.builder()
                        .endpointOverride(URI.create(endpointString))
                        .region(region)
                        .credentialsProvider(
                            StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
                        )
                        .build()
                    endpoint = endpointString

                    // Create initial queues
                    initialQueues.forEach { queueName ->
                        createQueue(sqs!!, queueName)
                    }

                    addShutdownHookOnce()

                    STARTED.set(true)
                    logger.info("ElasticMQ server started successfully at {}", endpoint)
                } catch (e: Exception) {
                    logger.error("Failed to start ElasticMQ server", e)
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
                        logger.info("Shutting down ElasticMQ server (JVM shutdown)")
                        stop()
                    } catch (e: Exception) {
                        // Avoid throwing from shutdown hook
                        logger.warn("Error while stopping ElasticMQ during shutdown: {}", e.message)
                    }
                })
            }
        }

        private fun createQueue(client: SqsClient, queueName: String) {
            try {
                client.createQueue { it.queueName(queueName) }
                logger.info("Created queue: {}", queueName)
            } catch (e: Exception) {
                logger.warn("Could not create queue {}: {}", queueName, e.message)
            }
        }

        /**
         * Find a free local TCP port.
         * Note: There is a small race between selecting a free port and binding it; acceptable for tests.
         */
        private fun findFreePort(): Int =
            ServerSocket(0).use { socket -> socket.localPort }
    }
}
