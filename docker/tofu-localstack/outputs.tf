output "async_jobs_queue_url" {
  description = "URL of the async jobs SQS queue"
  value       = aws_sqs_queue.async_jobs.url
}

output "async_jobs_queue_arn" {
  description = "ARN of the async jobs SQS queue"
  value       = aws_sqs_queue.async_jobs.arn
}

output "async_jobs_dlq_url" {
  description = "URL of the dead letter queue"
  value       = aws_sqs_queue.async_jobs_dlq.url
}

output "async_jobs_dlq_arn" {
  description = "ARN of the dead letter queue"
  value       = aws_sqs_queue.async_jobs_dlq.arn
}

output "voucher_events_topic_arn" {
  description = "ARN of the voucher events SNS topic"
  value       = aws_sns_topic.voucher_events.arn
}

output "localstack_endpoint" {
  description = "LocalStack endpoint for AWS services"
  value       = "http://localstack:4566"
}
