locals {
  realm_name = "voucherengine"

  # Central place for dev clients
  clients = {
    acme = {
      client_id     = "acme"
      name          = "acme"
      client_secret = "acme-dev-secret" # dev-only; ok to hardcode for local compose
      tenants_claim = ["acme"]
      role_name     = "ROLE_TENANT"
    }
    manager = {
      client_id     = "manager"
      name          = "manager"
      client_secret = "manager-dev-secret"
      tenants_claim = null
      role_name     = "ROLE_MANAGER"
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

resource "keycloak_role" "roles" {
  for_each = toset(["ROLE_TENANT", "ROLE_MANAGER"])

  realm_id = keycloak_realm.voucherengine.id
  name     = each.value
}

resource "keycloak_user_roles" "service_account_roles" {
  for_each = local.clients

  realm_id = keycloak_realm.voucherengine.id
  user_id  = keycloak_openid_client.clients[each.key].service_account_user_id
  role_ids = [keycloak_role.roles[each.value.role_name].id]
}

# Hardcoded claim "tenants"
resource "keycloak_openid_hardcoded_claim_protocol_mapper" "tenants_claim" {
  for_each = { for key, value in local.clients : key => value if value.tenants_claim != null }

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
