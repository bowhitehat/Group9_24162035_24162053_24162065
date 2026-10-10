#!/bin/sh
set -eu
# Aiven supplies a project CA. Import only that certificate; never disable TLS verification.
if [ -n "${DB_CA_CERT_PEM:-}" ]; then
  umask 077
  printf '%s\n' "$DB_CA_CERT_PEM" > /tmp/aiven-ca.pem
  DB_CA_CERT_FILE=/tmp/aiven-ca.pem
fi
if [ -n "${DB_CA_CERT_FILE:-}" ]; then
  test -r "$DB_CA_CERT_FILE" || { echo "Missing MySQL CA certificate" >&2; exit 1; }
  keytool -importcert -noprompt -alias aiven-mysql -file "$DB_CA_CERT_FILE" \
    -keystore /tmp/aiven-truststore.p12 -storetype PKCS12 -storepass changeit
fi
exec java -jar /app/app.jar "$@"
