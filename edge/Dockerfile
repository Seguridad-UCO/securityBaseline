# edge — Caddy + Coraza (WAF) + OWASP CRS v4 + rate limit por IP. ADR-032 / PLAN-HU-027 §7.1.
#
# Contexto de build = esta carpeta:  docker build -t security-edge edge/
#
# Versiones fijadas a propósito: el CRS viaja embebido en coraza-caddy (módulo Go
# github.com/corazawaf/coraza-coreruleset, fijado por go.sum del release de coraza-caddy), así que
# la versión de coraza-caddy ES la versión del CRS. Se imprime en el log de arranque (entrypoint.sh).
ARG CADDY_VERSION=2.10.0
ARG CORAZA_CADDY_VERSION=v2.1.0
ARG CADDY_RATELIMIT_VERSION=v0.1.0

FROM caddy:${CADDY_VERSION}-builder AS builder
ARG CORAZA_CADDY_VERSION
ARG CADDY_RATELIMIT_VERSION
RUN xcaddy build \
      --with github.com/corazawaf/coraza-caddy/v2@${CORAZA_CADDY_VERSION} \
      --with github.com/mholt/caddy-ratelimit@${CADDY_RATELIMIT_VERSION}

FROM caddy:${CADDY_VERSION}-alpine
ARG CORAZA_CADDY_VERSION
ENV CORAZA_CADDY_VERSION=${CORAZA_CADDY_VERSION}
RUN apk add --no-cache curl
COPY --from=builder /usr/bin/caddy /usr/bin/caddy
COPY Caddyfile /etc/caddy/Caddyfile
COPY rules/ /etc/coraza/rules/
COPY --chmod=755 entrypoint.sh /usr/local/bin/edge-entrypoint.sh
COPY --chmod=755 healthcheck.sh /usr/local/bin/edge-healthcheck.sh
COPY --chmod=755 audit-rotate.sh /usr/local/bin/edge-audit-rotate.sh
# Sin root: usuario 10001 dueño de lo único que el edge escribe (estado de Caddy, audit log y reglas generadas).
# Un bind mount en Linux debe ser escribible por el uid 10001 (ver edge/README.md).
RUN addgroup -g 10001 -S edge && adduser -u 10001 -S -G edge -H -s /sbin/nologin edge \
 && mkdir -p /var/log/security /etc/coraza/generated /data /config \
 && chown -R edge:edge /var/log/security /etc/coraza/generated /data /config

# Defaults del contrato (PLAN-HU-027 §7.1). EDGE_UPSTREAM no tiene default a propósito: sin
# upstream el edge no arranca.
ENV EDGE_MODE=detection \
    EDGE_LISTEN=:8088 \
    EDGE_INTERNAL_ALLOW_PEP=false \
    EDGE_PARANOIA=1 \
    EDGE_ANOMALY_INBOUND=5 \
    EDGE_TRUSTED_PROXIES="" \
    EDGE_RATE_AUTHORIZE=60/1m \
    EDGE_RATE_SESSION=30/1m \
    EDGE_RATE_DEFAULT=300/1m \
    EDGE_LOG_DIR=/var/log/security \
    EDGE_AUDIT_MAX_BYTES=10485760 \
    EDGE_AUDIT_KEEP=5 \
    EDGE_AUDIT_ROTATE_INTERVAL=60 \
    EDGE_DETECT_PROBE_INTERVAL=600 \
    OTEL_SERVICE_NAME=security-edge \
    DEPLOYMENT_ENVIRONMENT=local

HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=3 \
  CMD /usr/local/bin/edge-healthcheck.sh

USER 10001
EXPOSE 8088
ENTRYPOINT ["/usr/local/bin/edge-entrypoint.sh"]
