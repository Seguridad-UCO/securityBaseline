FROM openpolicyagent/opa:1.10.0-static

WORKDIR /app
COPY bundle.tar.gz /app/bundle.tar.gz
COPY infrastructure/opa/opa.yaml /app/opa.yaml

EXPOSE 8181
ENTRYPOINT ["opa", "run", "--server", "--addr=0.0.0.0:8181", "--config-file=/app/opa.yaml", "/app/bundle.tar.gz"]
