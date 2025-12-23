package org.wahlen.voucherengine.persistence.model.async

enum class AsyncJobStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}
