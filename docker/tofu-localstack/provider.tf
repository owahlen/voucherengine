provider "aws" {
  region                      = "eu-central-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  # LocalStack-friendly defaults
  s3_use_path_style           = true

  endpoints {
    s3  = "http://localstack:4566"
    sqs = "http://localstack:4566"
    sns = "http://localstack:4566"
  }
}
