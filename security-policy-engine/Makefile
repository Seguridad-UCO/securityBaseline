.PHONY: validate test bundle run

validate:
	docker compose run --rm opa-tools ./scripts/validate

test:
	docker compose run --rm opa-tools ./scripts/test

bundle:
	docker compose run --rm opa-tools ./scripts/bundle

run: bundle
	docker compose up --build
