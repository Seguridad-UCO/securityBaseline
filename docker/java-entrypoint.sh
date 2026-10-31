#!/bin/sh
# Arranque común de pdp y pep. Si hay certificados de CA propios en /certs (*.crt, *.pem, *.cer) los suma al
# almacén de confianza de Java, igual que hacía la imagen eclipse-temurin; si no hay, arranca Java sin tocar nada.
#
# La imagen corre sin root y el almacén del JDK es de solo lectura: se trabaja sobre una copia en /tmp y se la
# entrega a la JVM con -Djavax.net.ssl.trustStore. Un certificado ilegible o inválido detiene el arranque
# (falla cerrado): arrancar sin la CA pedida solo movería el error a la primera conexión TLS.
#   CA_CERTS_DIR   carpeta de certificados (default /certs)
set -eu

CERTS_DIR="${CA_CERTS_DIR:-/certs}"
STORE=/tmp/cacerts
PASSWORD=changeit

found=0
if [ -d "$CERTS_DIR" ]; then
  for cert in "$CERTS_DIR"/*.crt "$CERTS_DIR"/*.pem "$CERTS_DIR"/*.cer; do
    [ -f "$cert" ] && found=$((found + 1))
  done
fi

if [ "$found" -gt 0 ]; then
  cp "$JAVA_HOME/lib/security/cacerts" "$STORE"
  chmod u+w "$STORE"
  n=0
  for cert in "$CERTS_DIR"/*.crt "$CERTS_DIR"/*.pem "$CERTS_DIR"/*.cer; do
    [ -f "$cert" ] || continue
    n=$((n + 1))
    keytool -importcert -noprompt -trustcacerts -keystore "$STORE" -storepass "$PASSWORD" \
      -alias "custom-$n-$(basename "$cert")" -file "$cert" >/dev/null 2>&1 \
      || { echo "java-entrypoint: no se pudo importar el certificado $cert" >&2; exit 1; }
  done
  echo "java-entrypoint: $n certificado(s) de CA propios agregados desde $CERTS_DIR" >&2
  set -- "-Djavax.net.ssl.trustStore=$STORE" "-Djavax.net.ssl.trustStorePassword=$PASSWORD" "$@"
fi

exec java "$@"
