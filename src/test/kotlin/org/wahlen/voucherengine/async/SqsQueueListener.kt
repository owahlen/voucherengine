package org.wahlen.voucherengine.async

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.messaging.handler.annotation.Header

/**
 * Test-specific listener for SQS messages
 */
class SqsQueueListener {
    val receivedMessages = mutableListOf<ReceivedMessage>()

    data class ReceivedMessage(val body: String, val senderId: String)

    @SqsListener("async-jobs")
    fun handleMessage(
        @org.springframework.messaging.handler.annotation.Payload rawMessage: String,
        @Header("SenderId") senderId: String
    ) {
        receivedMessages.add(ReceivedMessage(rawMessage, senderId))
    }

    fun reset() {
        receivedMessages.clear()
    }
}

