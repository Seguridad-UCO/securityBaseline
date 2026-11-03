variable "DOCKERHUB_NAMESPACE" {}
variable "VERSION" {}

group "default" {
  targets = ["pdp", "pep", "opa", "keycloak-local"]
}

target "common" {
  platforms = ["linux/amd64", "linux/arm64"]
  labels = { "org.opencontainers.image.version" = "${VERSION}" }
}
target "pdp" {
  inherits = ["common"]
  context = "."
  dockerfile = "pdp/Dockerfile"
  tags = ["${DOCKERHUB_NAMESPACE}/security-pdp:${VERSION}"]
}
target "pep" {
  inherits = ["common"]
  context = "."
  dockerfile = "pep/Dockerfile"
  tags = ["${DOCKERHUB_NAMESPACE}/security-pep:${VERSION}"]
}
target "opa" {
  inherits = ["common"]
  context = "security-policy-engine"
  dockerfile = "Dockerfile"
  tags = ["${DOCKERHUB_NAMESPACE}/security-opa:${VERSION}"]
}
target "keycloak-local" {
  inherits = ["common"]
  context = "."
  dockerfile = "distribution/keycloak.local.Dockerfile"
  tags = ["${DOCKERHUB_NAMESPACE}/security-keycloak-local:${VERSION}"]
}
