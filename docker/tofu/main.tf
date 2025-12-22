locals {
  realm_name = "voucherengine"

  # Central place for dev clients
  clients = {
    acme = {
      client_id     = "acme"
      name          = "acme"
      client_secret = "acme-dev-secret" # dev-only; ok to hardcode for local compose
      tenants_claim = ["acme"]
    }
  }
}

resource "keycloak_realm" "voucherengine" {
  realm   = local.realm_name
  enabled = true
}

resource "keycloak_openid_client" "clients" {
  for_each = local.clients

  realm_id  = keycloak_realm.voucherengine.id
  client_id = each.value.client_id
  name      = each.value.name

  enabled     = true
  access_type = "CONFIDENTIAL"

  # For machine-to-machine JWTs via client_credentials:
  service_accounts_enabled     = true
  standard_flow_enabled        = false
  direct_access_grants_enabled = false
  implicit_flow_enabled        = false

  # For local dev we set a known secret.
  client_secret = each.value.client_secret

  # Not used for client_credentials; keep empty to avoid accidental browser usage
  valid_redirect_uris = []
}

# Hardcoded claim "tenants"
resource "keycloak_openid_hardcoded_claim_protocol_mapper" "tenants_claim" {
  for_each = local.clients

  realm_id  = keycloak_realm.voucherengine.id
  client_id = keycloak_openid_client.clients[each.key].id

  name       = "tenants-claim"
  claim_name = "tenants"

  # JSON array in the token
  claim_value      = jsonencode(each.value.tenants_claim)
  claim_value_type = "JSON"

  # Put it into the access token JWT (used for service authZ)
  add_to_access_token = true
  add_to_id_token     = false
  add_to_userinfo     = false
}
