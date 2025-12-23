# Docker Development Environment

This directory contains the Docker Compose setup for local development.

## Services

### Core Services
- **voucherengine_db** - PostgreSQL database (port 5432)
- **keycloak** - Keycloak authentication server (port 8180)
- **keycloak_db** - PostgreSQL for Keycloak (port 5433)
- **localstack** - AWS services emulation (port 4566)

### Infrastructure Initialization
- **keycloak_init** - Configures Keycloak using OpenTofu
- **localstack_init** - Configures AWS resources using OpenTofu

## Quick Start

```bash
# Start all services
cd docker
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Stop and remove volumes (fresh start)
docker-compose down -v
```

## Service Endpoints

| Service | Endpoint | Credentials |
|---------|----------|-------------|
| Voucherengine DB | localhost:5432 | voucherengine/voucherengine |
| Keycloak | http://localhost:8180 | admin/password |
| Keycloak DB | localhost:5433 | postgres/password |
| LocalStack | http://localhost:4566 | test/test |

## LocalStack AWS Services

LocalStack provides local AWS service emulation:

### SQS Queues
- **voucher-async-jobs** - Main queue for async job processing
- **voucher-async-jobs-dlq** - Dead letter queue for failed jobs

### SNS Topics
- **voucher-events** - Event notifications (optional)

### Accessing LocalStack

Use `awslocal` CLI (alias for `aws` with LocalStack endpoint):

```bash
# Install awscli-local
pip install awscli-local

# List SQS queues
awslocal sqs list-queues

# Send a test message
awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/voucher-async-jobs \
  --message-body '{"test": "message"}'

# Receive messages
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/voucher-async-jobs

# View DLQ messages
awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/voucher-async-jobs-dlq
```

## OpenTofu Configuration

### Keycloak Configuration
Located in `tofu-keycloak/`:
- Creates `voucherengine` realm
- Configures OAuth2 clients (acme, manager)
- Sets up roles (ROLE_TENANT, ROLE_MANAGER)
- Adds tenant claims

### LocalStack Configuration
Located in `tofu-localstack/`:
- Creates SQS queues with DLQ
- Configures SNS topics
- Sets retry policies and timeouts

## Application Configuration

Add to your `src/main/resources/application-local.yml`:

```yaml
spring:
  cloud:
    aws:
      region:
        static: eu-central-1
      credentials:
        access-key: test
        secret-key: test
      sqs:
        endpoint: http://localhost:4566
        listener:
          auto-startup: true
      sns:
        endpoint: http://localhost:4566

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/voucherengine
```

## Troubleshooting

### LocalStack not responding
```bash
# Check health
curl http://localhost:4566/_localstack/health

# Restart
docker-compose restart localstack
```

### Queues not created
```bash
# Re-run LocalStack init
docker-compose up -d localstack_init

# Check logs
docker-compose logs localstack_init
```

### Keycloak configuration failed
```bash
# Re-run Keycloak init
docker-compose up -d keycloak_init

# Check logs
docker-compose logs keycloak_init
```

### Reset everything
```bash
# Stop and remove all data
docker-compose down -v

# Start fresh
docker-compose up -d
```

## Data Persistence

- **PostgreSQL data** - Stored in named volumes
- **Keycloak state** - Persisted in keycloak_db
- **LocalStack data** - Persisted in localstack_data volume
- **OpenTofu state** - Stored in tofu_state and tofu_localstack_state volumes

## Development Workflow

1. Start services: `docker-compose up -d`
2. Wait for initialization (check logs)
3. Run your application with `local` profile
4. Application connects to all services automatically
5. Test async jobs via SQS
6. Authenticate via Keycloak

## Environment Variables

All sensitive data uses development defaults:
- Database passwords: `password` or `voucherengine`
- Keycloak admin: `admin/password`
- AWS credentials: `test/test`
- OAuth client secrets: `*-dev-secret`

**⚠️ These are for local development only. Never use in production!**

## Integration with Tests

Tests can use the same LocalStack instance:

```kotlin
@SpringBootTest
@ActiveProfiles("test")
class MyIntegrationTest {
    // Spring Cloud AWS will use localhost:4566 automatically
    // when application-test.yml is configured
}
```

Or use Testcontainers for isolated tests (see test documentation).
