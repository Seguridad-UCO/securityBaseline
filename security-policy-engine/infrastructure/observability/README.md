# Observability boundary

Do not export the raw OPA input in decision logs. At most, export a redacted event with
an irreversible subject reference, application id, resource type, action, effect,
reason code, `decision_id`, correlation id according to local retention rules, and bundle
revision. Configure the sink and redaction outside the policy bundle.
