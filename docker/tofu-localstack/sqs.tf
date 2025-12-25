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
  # Define only the main queues here.
  # DLQs are automatically created with the
  # pattern: {queue_name}-dlq
  ##########################################
  queues = {
    voucherengine-async-jobs = {
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

  ##########################################
  # DLQ configuration (shared settings)
  ##########################################
  # All DLQs share these settings.
  # Individual overrides could be added later
  # if needed.
  ##########################################
  dlq_config = {
    # Keep failed messages long enough for
    # inspection and manual replay.
    message_retention_seconds = 1209600  # 14 days

    # DLQs typically don't need long visibility
    # since they're mainly for inspection.
    visibility_timeout_seconds = 300  # 5 minutes

    # Long polling for efficient consumption
    receive_wait_time_seconds = 20
  }
}

############################################
# Dead Letter Queues (DLQs)
############################################
# Automatically create a DLQ for each main
# queue using the pattern: {queue_name}-dlq
#
# DLQs are critical for:
# - Debugging broken messages
# - Preventing infinite retry loops
# - Keeping the main queue healthy
############################################

resource "aws_sqs_queue" "dlqs" {
  for_each = local.queues

  name = "${each.key}-dlq"

  visibility_timeout_seconds = local.dlq_config.visibility_timeout_seconds
  message_retention_seconds  = local.dlq_config.message_retention_seconds
  receive_wait_time_seconds  = local.dlq_config.receive_wait_time_seconds

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Type        = "dlq"
    MainQueue   = each.key
  }
}

############################################
# Main Queues
############################################
# Creates all main queues with automatic
# DLQ integration via redrive policy.
#
# Each queue references its corresponding DLQ
# created above.
############################################

resource "aws_sqs_queue" "queues" {
  for_each = local.queues

  name = each.key

  visibility_timeout_seconds = each.value.visibility_timeout_seconds
  message_retention_seconds  = each.value.message_retention_seconds
  receive_wait_time_seconds  = each.value.receive_wait_time_seconds

  ##########################################
  # Redrive policy (DLQ integration)
  ##########################################
  # Automatically connects to the DLQ created
  # for this queue.
  ##########################################
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.dlqs[each.key].arn
    maxReceiveCount     = each.value.max_receive_count
  })

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Type        = "main"
    DLQ         = "${each.key}-dlq"
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
