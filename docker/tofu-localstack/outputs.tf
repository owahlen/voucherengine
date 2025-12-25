output "async_jobs_queue_url" {
  description = "URL of the async jobs SQS queue"
  value       = aws_sqs_queue.queues["voucherengine-async-jobs"].url
}

output "async_jobs_queue_arn" {
  description = "ARN of the async jobs SQS queue"
  value       = aws_sqs_queue.queues["voucherengine-async-jobs"].arn
}

output "async_jobs_dlq_url" {
  description = "URL of the dead letter queue"
  value       = aws_sqs_queue.dlqs["voucherengine-async-jobs"].url
}

output "async_jobs_dlq_arn" {
  description = "ARN of the dead letter queue"
  value       = aws_sqs_queue.dlqs["voucherengine-async-jobs"].arn
}

output "voucher_events_topic_arn" {
  description = "ARN of the voucher events SNS topic"
  value       = aws_sns_topic.voucherengine_events.arn
}

output "localstack_endpoint" {
  description = "LocalStack endpoint for AWS services"
  value       = "http://localstack:4566"
}

output "voucherengine_exports_bucket_name" {
  description = "Name of the voucherengine exports S3 bucket"
  value       = aws_s3_bucket.voucherengine_exports.bucket
}

output "voucherengine_exports_bucket_arn" {
  description = "ARN of the voucherengine exports S3 bucket"
  value       = aws_s3_bucket.voucherengine_exports.arn
}

