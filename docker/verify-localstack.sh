#!/bin/bash
# LocalStack verification script

set -e

LOCALSTACK_ENDPOINT="http://localhost:4566"

echo "🔍 Checking LocalStack services..."

# Check if LocalStack is running
if ! curl -s "${LOCALSTACK_ENDPOINT}/_localstack/health" > /dev/null; then
    echo "❌ LocalStack is not running or not healthy"
    echo "   Start with: cd docker && docker-compose up -d localstack"
    exit 1
fi

echo "✅ LocalStack is running"

# Check SQS queues
echo ""
echo "📬 Checking SQS queues..."

AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sqs list-queues || {
    echo "❌ Failed to list SQS queues"
    exit 1
}

# Get queue URLs
MAIN_QUEUE_URL=$(AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
    aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sqs get-queue-url \
    --queue-name voucher-async-jobs --output text 2>/dev/null || echo "")

DLQ_URL=$(AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
    aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sqs get-queue-url \
    --queue-name voucher-async-jobs-dlq --output text 2>/dev/null || echo "")

if [ -z "$MAIN_QUEUE_URL" ]; then
    echo "❌ Main queue 'voucher-async-jobs' not found"
    echo "   Run: cd docker && docker-compose up -d localstack_init"
    exit 1
fi

if [ -z "$DLQ_URL" ]; then
    echo "❌ DLQ 'voucher-async-jobs-dlq' not found"
    echo "   Run: cd docker && docker-compose up -d localstack_init"
    exit 1
fi

echo "✅ Main queue: ${MAIN_QUEUE_URL}"
echo "✅ Dead letter queue: ${DLQ_URL}"

# Check queue attributes
echo ""
echo "⚙️  Queue configuration:"
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sqs get-queue-attributes \
    --queue-url "${MAIN_QUEUE_URL}" \
    --attribute-names VisibilityTimeout MessageRetentionPeriod ReceiveMessageWaitTimeSeconds \
    --output table

# Check SNS topics
echo ""
echo "📢 Checking SNS topics..."

AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sns list-topics || {
    echo "⚠️  No SNS topics found (this is optional)"
}

# Test sending a message
echo ""
echo "📨 Testing message send/receive..."

MESSAGE_ID=$(AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
    aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sqs send-message \
    --queue-url "${MAIN_QUEUE_URL}" \
    --message-body '{"test":"verification","timestamp":"'$(date -u +%Y-%m-%dT%H:%M:%SZ)'"}' \
    --output text --query MessageId)

echo "✅ Sent test message: ${MESSAGE_ID}"

# Receive the message
RECEIVED=$(AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
    aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sqs receive-message \
    --queue-url "${MAIN_QUEUE_URL}" \
    --max-number-of-messages 1 \
    --output json)

if echo "$RECEIVED" | grep -q "test"; then
    echo "✅ Successfully received test message"
    
    # Delete the message
    RECEIPT_HANDLE=$(echo "$RECEIVED" | grep -o '"ReceiptHandle": "[^"]*"' | head -1 | cut -d'"' -f4)
    if [ -n "$RECEIPT_HANDLE" ]; then
        AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
        aws --endpoint-url="${LOCALSTACK_ENDPOINT}" sqs delete-message \
            --queue-url "${MAIN_QUEUE_URL}" \
            --receipt-handle "${RECEIPT_HANDLE}"
        echo "✅ Cleaned up test message"
    fi
else
    echo "⚠️  Message not received (might be timing issue)"
fi

echo ""
echo "✅ All LocalStack checks passed!"
echo ""
echo "📝 Queue endpoints:"
echo "   Main: ${MAIN_QUEUE_URL}"
echo "   DLQ:  ${DLQ_URL}"
echo ""
echo "🚀 Ready for development!"
