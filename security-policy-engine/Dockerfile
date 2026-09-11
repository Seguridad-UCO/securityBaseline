FROM openpolicyagent/opa:1.10.0-static

WORKDIR /app
COPY policies /app/policies
COPY data /app/data
COPY infrastructure/opa/opa.yaml /app/opa.yaml

EXPOSE 8181
ENTRYPOINT ["opa", "run", "--server", "--addr=0.0.0.0:8181", "--config-file=/app/opa.yaml", "/app/policies", "/app/data"]

