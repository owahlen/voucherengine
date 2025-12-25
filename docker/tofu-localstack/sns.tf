############################################
# SNS – Voucherengine event bus
############################################
# This SNS topic is used to publish domain events
# related to vouchers (created, redeemed, expired, etc.).
#
# In local development it runs on LocalStack,
# but the same definition can be reused for AWS
# without changes.
############################################

resource "aws_sns_topic" "voucherengine_events" {
  name = "voucherengine-events"

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Purpose     = "domain-events"
  }
}

############################################
# Optional: SQS subscription (event-driven async processing)
############################################
# This shows the typical pattern of wiring SNS → SQS:
# - The application publishes events to SNS
# - One or more SQS queues subscribe to those events
# - Background workers consume messages asynchronously
#
# Keep this commented out until you actually need it.
# When enabled, make sure the referenced SQS queue exists
# and has an appropriate queue policy allowing SNS to publish.
############################################

# resource "aws_sns_topic_subscription" "async_jobs_subscription" {
#   topic_arn = aws_sns_topic.voucher_events.arn
#   protocol  = "sqs"
#   endpoint  = aws_sqs_queue.queues["voucherengine-async-jobs"].arn
# }

############################################
# Notes:
# - Fan-out is easy: add more subscriptions (SQS, Lambda, HTTP)
# - SNS is ideal for loosely-coupled services
# - LocalStack behaves close enough to AWS SNS for dev/testing
############################################