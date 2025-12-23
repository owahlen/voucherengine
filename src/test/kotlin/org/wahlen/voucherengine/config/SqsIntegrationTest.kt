package org.wahlen.voucherengine.config

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

/**
 * Meta-annotation for integration tests that require ElasticMQ (SQS).
 *
 * This annotation combines:
 * - @SpringBootTest: Loads the full Spring application context with SQS enabled
 * - @ActiveProfiles("test"): Activates the "test" profile
 * - @ContextConfiguration(initializers = [ElasticMQInitializer::class]): Starts ElasticMQ before Spring Boot autoconfiguration
 * - @Import(ElasticMQTestConfiguration::class): ElasticMQ queue setup and SQS listener factory
 *
 * Use this annotation for tests that:
 * - Send messages to SQS queues
 * - Test async job processing
 * - Test export functionality (which uses async jobs)
 *
 * You can pass custom properties to override application settings:
 * ```
 * @SqsIntegrationTest(properties = ["my.property=value"])
 * class MyAsyncTest { ... }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = [ElasticMQInitializer::class])
@Import(ElasticMQTestConfiguration::class)
annotation class SqsIntegrationTest(
    /**
     * Properties in form key=value that should be added to the Spring Environment.
     * Forwarded to @SpringBootTest.properties
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "properties")
    val properties: Array<String> = []
)
