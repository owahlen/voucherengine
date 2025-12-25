package org.wahlen.voucherengine.config

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.wahlen.voucherengine.testinfra.ElasticMqExtension
import org.wahlen.voucherengine.testinfra.ElasticMqPropertyInitializer
import org.wahlen.voucherengine.testinfra.S3MockExtension
import org.wahlen.voucherengine.testinfra.S3MockPropertyInitializer

/**
 * Meta-annotation for integration tests that require S3Mock (and ElasticMQ/SQS).
 * 
 * This annotation combines:
 * - @SpringBootTest: Loads the full Spring application context
 * - @ActiveProfiles("test"): Activates the "test" profile
 * - @ExtendWith: Starts S3Mock and ElasticMQ once per JVM, creates buckets and queues
 * - @ContextConfiguration: Registers dynamic endpoints via initializers
 * 
 * Use this annotation for tests that:
 * - Upload/download files to/from S3
 * - Test export functionality (which uses both S3 and async jobs)
 * - Test async job processing with S3 storage
 * 
 * Note: Static AWS properties are in test application.yml.
 * S3Mock and ElasticMQ are started automatically on random ports with resources pre-created.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(S3MockExtension::class, ElasticMqExtension::class)
@ContextConfiguration(initializers = [S3MockPropertyInitializer::class, ElasticMqPropertyInitializer::class])
annotation class S3IntegrationTest(
    /**
     * Properties in form key=value that should be added to the Spring Environment.
     * Forwarded to @SpringBootTest.properties
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "properties")
    val properties: Array<String> = []
)
