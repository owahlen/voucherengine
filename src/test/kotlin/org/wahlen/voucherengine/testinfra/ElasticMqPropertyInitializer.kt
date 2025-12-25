package org.wahlen.voucherengine.testinfra

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils

/**
 * Spring ApplicationContextInitializer that configures the dynamic ElasticMQ endpoint.
 * Used by @SqsIntegrationTest and @S3IntegrationTest annotations.
 * 
 * Static properties (region, credentials) are configured in test application.yml.
 * This initializer only sets the dynamic endpoint property that depends on the running ElasticMQ instance.
 */
class ElasticMqPropertyInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // Ensure ElasticMQ is started and get its dynamic endpoint
        val endpoint = ElasticMqExtension.endpoint()
        
        // Override only the endpoint property (everything else in application.yml)
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "spring.cloud.aws.sqs.endpoint=$endpoint"
        )
    }
}
