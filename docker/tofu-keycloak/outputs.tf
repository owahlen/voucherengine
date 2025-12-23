output "realm_name" {
  value = keycloak_realm.voucherengine.realm
}

output "client_ids" {
  value = {
    for k, v in local.clients :
    k => v.client_id
  }
}
