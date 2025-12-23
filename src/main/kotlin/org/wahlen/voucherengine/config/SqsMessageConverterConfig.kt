package org.wahlen.voucherengine.config

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHeaders
import org.springframework.messaging.converter.AbstractMessageConverter
import org.springframework.messaging.converter.ByteArrayMessageConverter
import org.springframework.messaging.converter.CompositeMessageConverter
import org.springframework.messaging.converter.StringMessageConverter
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory
import org.springframework.util.MimeType
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import tools.jackson.databind.ObjectMapper

/**
 * Configuration for SQS message conversion.
 *
 * Configures Jackson-based message conversion for SQS listeners and templates,
 * allowing automatic conversion of JSON payloads to Kotlin data classes.
 */
@Configuration
class SqsMessageConverterConfig {

    /**
     * Custom Jackson message converter that uses tools.jackson.databind.ObjectMapper
     */
    class ToolsJacksonMessageConverter(
        private val objectMapper: ObjectMapper
    ) : AbstractMessageConverter(MimeType.valueOf("application/json")) {

        init {
            setSerializedPayloadClass(String::class.java)
            setStrictContentTypeMatch(false)
        }

        override fun supports(clazz: Class<*>): Boolean = true

        override fun convertFromInternal(
            message: Message<*>,
            targetClass: Class<*>,
            conversionHint: Any?
        ): Any? {
            val payload = message.payload

            // If target class is String and payload is already String, return as-is
            if (targetClass == String::class.java && payload is String) {
                return payload
            }

            // Check if JavaType header is present to determine actual target class
            val javaTypeHeader = message.headers["JavaType"]
            val actualTargetClass = if (javaTypeHeader != null && javaTypeHeader is String) {
                try {
                    Class.forName(javaTypeHeader)
                } catch (e: ClassNotFoundException) {
                    targetClass
                }
            } else {
                targetClass
            }

            // Convert JSON string/bytes to target object
            return when (payload) {
                is String -> {
                    if (actualTargetClass == String::class.java) {
                        payload
                    } else {
                        objectMapper.readValue(payload, actualTargetClass)
                    }
                }
                is ByteArray -> objectMapper.readValue(payload, actualTargetClass)
                else -> {
                    // If payload is already an object but not the target type,
                    // serialize it to JSON and deserialize to target type
                    val json = objectMapper.writeValueAsString(payload)
                    if (actualTargetClass == String::class.java) {
                        json
                    } else {
                        objectMapper.readValue(json, actualTargetClass)
                    }
                }
            }
        }

        override fun convertToInternal(
            payload: Any,
            headers: MessageHeaders?,
            conversionHint: Any?
        ): Any {
            return objectMapper.writeValueAsString(payload)
        }
    }

    /**
     * Creates a Jackson-based message converter for SQS messages.
     *
     * We don't set any payload converter here. By default, SqsMessagingMessageConverter
     * will keep the SQS message body as a string, which is what we want.
     * The actual type conversion happens in our messageHandlerMethodFactory.
     */
    @Bean
    fun sqsMessagingMessageConverter(): SqsMessagingMessageConverter {
        return SqsMessagingMessageConverter()
    }

    /**
     * Custom MessageHandlerMethodFactory that uses only our ToolsJacksonMessageConverter.
     * This prevents Spring from using its default MappingJackson2MessageConverter.
     */
    @Bean
    @Primary
    fun messageHandlerMethodFactory(objectMapper: ObjectMapper): MessageHandlerMethodFactory {
        val factory = DefaultMessageHandlerMethodFactory()

        // Create a composite converter with only our custom converter
        val compositeConverter = CompositeMessageConverter(listOf(
            ToolsJacksonMessageConverter(objectMapper),
            StringMessageConverter(),
            ByteArrayMessageConverter()
        ))

        factory.setMessageConverter(compositeConverter)
        return factory
    }

    /**
     * Configures the SQS listener container factory with custom message converter.
     */
    @Bean
    @Primary
    fun defaultSqsListenerContainerFactory(
        sqsAsyncClient: SqsAsyncClient,
        sqsMessagingMessageConverter: SqsMessagingMessageConverter
    ): SqsMessageListenerContainerFactory<Any> {
        return SqsMessageListenerContainerFactory.builder<Any>()
            .sqsAsyncClient(sqsAsyncClient)
            .configure { options ->
                options.messageConverter(sqsMessagingMessageConverter)
                options.acknowledgementMode(AcknowledgementMode.ON_SUCCESS)
            }
            .build()
    }

    /**
     * Creates an SqsTemplate with custom message converter.
     */
    @Bean
    fun sqsTemplate(
        sqsAsyncClient: SqsAsyncClient
    ): SqsTemplate {
        return SqsTemplate.builder()
            .sqsAsyncClient(sqsAsyncClient)
            .build()
    }
}

