package org.wahlen.voucherengine.config

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.wahlen.voucherengine.testinfra.ElasticMqExtension
import org.wahlen.voucherengine.testinfra.ElasticMqPropertyInitializer

/**
 * Meta-annotation for integration tests that require ElasticMQ (SQS).
 *
 * This annotation combines:
 * - @SpringBootTest: Loads the full Spring application context with SQS enabled
 * - @ActiveProfiles("test"): Activates the "test" profile
 * - @ExtendWith(ElasticMqExtension::class): Starts ElasticMQ once per JVM, creates queues
 * - @ContextConfiguration: Registers dynamic ElasticMQ endpoint via initializer
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
 *
 * Note: Static AWS properties are in test application.yml.
 * ElasticMQ is started automatically on a random port with queues pre-created.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(ElasticMqExtension::class)
@ContextConfiguration(initializers = [ElasticMqPropertyInitializer::class])
annotation class SqsIntegrationTest(
    /**
     * Properties in form key=value that should be added to the Spring Environment.
     * Forwarded to @SpringBootTest.properties
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "properties")
    val properties: Array<String> = []
)
