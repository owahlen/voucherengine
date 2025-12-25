############################################
# SQS – Asynchronous job processing
############################################
# This queue setup is used for background / async
# processing (e.g. voucher exports, notifications,
# cleanup tasks).
#
# Pattern:
# - Producers send messages to the main queue
# - Consumers process messages asynchronously
# - Failed messages are retried and eventually
#   moved to a Dead Letter Queue (DLQ)
############################################

locals {
  ##########################################
  # Queue configuration (centralized)
  ##########################################
  # Keeping values in locals makes it easy to
  # tune behavior without touching resources.
  ##########################################
  queues = {
    async_jobs = {
      # Logical queue name
      name = "voucher-async-jobs"

      # How long a message is hidden after being
      # received by a consumer.
      # Should be >= max processing time.
      visibility_timeout_seconds = 300  # 5 minutes

      # How long messages are kept if not deleted.
      # Useful for debugging and replaying failures.
      message_retention_seconds = 1209600  # 14 days

      # Enable long polling to reduce empty receives
      # and unnecessary CPU/network usage.
      receive_wait_time_seconds = 20  # max allowed

      # How many times a message can be received
      # before being considered "poison" and sent
      # to the DLQ.
      max_receive_count = 3
    }
  }
}

############################################
# Dead Letter Queue (DLQ)
############################################
# Stores messages that failed processing
# multiple times.
#
# DLQs are critical for:
# - Debugging broken messages
# - Preventing infinite retry loops
# - Keeping the main queue healthy
############################################

resource "aws_sqs_queue" "async_jobs_dlq" {
  name = "voucher-async-jobs-dlq"

  # Keep failed messages long enough for
  # inspection and manual replay.
  message_retention_seconds = 1209600  # 14 days

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Type        = "dlq"
  }
}

############################################
# Main asynchronous jobs queue
############################################
# This is the queue your application workers
# will consume from.
#
# Messages that fail repeatedly are automatically
# moved to the DLQ via the redrive policy.
############################################

resource "aws_sqs_queue" "async_jobs" {
  name = local.queues.async_jobs.name

  visibility_timeout_seconds = local.queues.async_jobs.visibility_timeout_seconds
  message_retention_seconds  = local.queues.async_jobs.message_retention_seconds
  receive_wait_time_seconds  = local.queues.async_jobs.receive_wait_time_seconds

  ##########################################
  # Redrive policy (DLQ integration)
  ##########################################
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.async_jobs_dlq.arn
    maxReceiveCount     = local.queues.async_jobs.max_receive_count
  })

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Type        = "main"
  }
}

############################################
# Notes:
# - Tune visibility timeout carefully if
#   processing time changes
# - Consider per-message visibility extensions
#   for long-running jobs
# - SNS can be attached later for event fan-out
############################################
