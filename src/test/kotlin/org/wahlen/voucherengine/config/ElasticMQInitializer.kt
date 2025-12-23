package org.wahlen.voucherengine.config

import org.elasticmq.rest.sqs.SQSRestServer
import org.elasticmq.rest.sqs.SQSRestServerBuilder
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

/**
 * ApplicationContextInitializer that starts ElasticMQ before Spring Boot autoconfiguration.
 * This ensures ElasticMQ is available when AWS SQS autoconfiguration runs.
 *
 * This is initialized BEFORE @Configuration classes and BEFORE Spring Boot autoconfiguration,
 * solving the timing issue where SQS beans try to connect before ElasticMQ is ready.
 */
class ElasticMQInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    companion object {
        private val logger = LoggerFactory.getLogger(ElasticMQInitializer::class.java)
        private var sqsServer: SQSRestServer? = null
        private const val ELASTICMQ_PORT = 9324

        @JvmStatic
        @Synchronized
        fun ensureStarted() {
            if (sqsServer == null) {
                try {
                    logger.info("Starting ElasticMQ server on port $ELASTICMQ_PORT")
                    sqsServer = SQSRestServerBuilder
                        .withPort(ELASTICMQ_PORT)
                        .withInterface("localhost")
                        .start()
                    logger.info("ElasticMQ server started successfully on port $ELASTICMQ_PORT")

                    // Register shutdown hook
                    Runtime.getRuntime().addShutdownHook(Thread {
                        logger.info("Shutting down ElasticMQ server")
                        sqsServer?.stopAndWait()
                        sqsServer = null
                    })
                } catch (e: Exception) {
                    logger.error("Failed to start ElasticMQ server", e)
                    throw e
                }
            }
        }
    }

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // Start ElasticMQ before Spring Boot autoconfiguration
        ensureStarted()

        // Set AWS SQS properties to point to local ElasticMQ
        // These properties will be used by Spring Cloud AWS autoconfiguration
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "spring.cloud.aws.sqs.endpoint=http://localhost:$ELASTICMQ_PORT",
            "spring.cloud.aws.region.static=eu-central-1",
            "spring.cloud.aws.credentials.access-key=test",
            "spring.cloud.aws.credentials.secret-key=test"
        )

        logger.info("ElasticMQ initialized and properties configured for Spring context")
    }
}

