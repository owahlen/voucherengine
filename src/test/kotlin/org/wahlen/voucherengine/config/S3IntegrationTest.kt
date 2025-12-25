package org.wahlen.voucherengine.config

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.ActiveProfiles
import org.wahlen.voucherengine.testinfra.ElasticMqExtension

/**
 * Base annotation for integration tests that require S3
 * Uses S3Mock (lightweight S3 emulation) instead of LocalStack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(ElasticMqExtension::class)
@Import(ElasticMQTestConfiguration::class, S3MockTestConfiguration::class)
annotation class S3IntegrationTest(
    /**
     * Properties in form key=value that should be added to the Spring Environment.
     * Forwarded to @SpringBootTest.properties
     */
    @get:AliasFor(annotation = SpringBootTest::class, attribute = "properties")
    val properties: Array<String> = []
)
