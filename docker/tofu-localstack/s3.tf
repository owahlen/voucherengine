############################################
# S3 – Voucherengine imports bucket
############################################

resource "aws_s3_bucket" "voucherengine_imports" {
  bucket = "voucherengine-imports"

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Type        = "imports"
  }
}

############################################
# S3 – Voucherengine exports bucket
############################################

resource "aws_s3_bucket" "voucherengine_exports" {
  bucket = "voucherengine-exports"

  tags = {
    Environment = "local"
    Service     = "voucherengine"
    Type        = "exports"
  }
}

############################################
# Lifecycle: delete import files after 1 day
############################################

resource "aws_s3_bucket_lifecycle_configuration" "voucherengine_imports" {
  bucket = aws_s3_bucket.voucherengine_imports.id

  rule {
    id     = "delete-imports-after-1-day"
    status = "Enabled"

    # Required by newer provider versions: specify filter or prefix
    filter {
      prefix = "" # apply to all objects in the bucket
    }

    expiration {
      days = 1
    }
  }
}

############################################
# Lifecycle: delete export files after 1 day
############################################

resource "aws_s3_bucket_lifecycle_configuration" "voucherengine_exports" {
  bucket = aws_s3_bucket.voucherengine_exports.id

  rule {
    id     = "delete-exports-after-1-day"
    status = "Enabled"

    # Required by newer provider versions: specify filter or prefix
    filter {
      prefix = "" # apply to all objects in the bucket
    }

    expiration {
      days = 1
    }
  }
}

############################################
# Block all public access (defensive default)
############################################

resource "aws_s3_bucket_public_access_block" "voucherengine_imports" {
  bucket = aws_s3_bucket.voucherengine_imports.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

############################################
# Block all public access (defensive default)
############################################

resource "aws_s3_bucket_public_access_block" "voucherengine_exports" {
  bucket = aws_s3_bucket.voucherengine_exports.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

############################################
# Explicitly disable versioning
# (important so expired objects are truly removed)
############################################

resource "aws_s3_bucket_versioning" "voucherengine_imports" {
  bucket = aws_s3_bucket.voucherengine_imports.id

  versioning_configuration {
    status = "Suspended"
  }
}

############################################
# Explicitly disable versioning
# (important so expired objects are truly removed)
############################################

resource "aws_s3_bucket_versioning" "voucherengine_exports" {
  bucket = aws_s3_bucket.voucherengine_exports.id

  versioning_configuration {
    status = "Suspended"
  }
}
