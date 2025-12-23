locals {
  # Queue configuration
  queues = {
    async_jobs = {
      name                       = "voucher-async-jobs"
      visibility_timeout_seconds = 300  # 5 minutes
      message_retention_seconds  = 1209600  # 14 days
      receive_wait_time_seconds  = 20  # Long polling
      max_receive_count          = 3  # Retries before DLQ
    }
  }
}

# Dead Letter Queue
resource "aws_sqs_queue" "async_jobs_dlq" {
  name                      = "voucher-async-jobs-dlq"
  message_retention_seconds = 1209600  # 14 days

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Type        = "dlq"
  }
}

# Main Queue
resource "aws_sqs_queue" "async_jobs" {
  name                       = local.queues.async_jobs.name
  visibility_timeout_seconds = local.queues.async_jobs.visibility_timeout_seconds
  message_retention_seconds  = local.queues.async_jobs.message_retention_seconds
  receive_wait_time_seconds  = local.queues.async_jobs.receive_wait_time_seconds

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

# SNS Topic for voucher events (optional, for future use)
resource "aws_sns_topic" "voucher_events" {
  name = "voucher-events"

  tags = {
    Environment = "local"
    Service     = "voucherengine"
  }
}

# Subscribe SQS queue to SNS topic (example pattern)
# Uncomment if you want to use event-driven architecture
# resource "aws_sns_topic_subscription" "async_jobs_subscription" {
#   topic_arn = aws_sns_topic.voucher_events.arn
#   protocol  = "sqs"
#   endpoint  = aws_sqs_queue.async_jobs.arn
# }
