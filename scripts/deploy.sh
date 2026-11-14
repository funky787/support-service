#!/usr/bin/env bash
set -euo pipefail
: "${REGISTRY_IMAGE:?Set REGISTRY_IMAGE}"
: "${IMAGE_TAG:?Set IMAGE_TAG}"
export APP_IMAGE="${REGISTRY_IMAGE}:${IMAGE_TAG}"
docker compose -f compose.deploy.yaml pull app
docker compose -f compose.deploy.yaml up -d --no-build --wait
curl --fail --retry 12 --retry-delay 5 http://localhost:8080/actuator/health/readiness
printf '%s\n' "$APP_IMAGE" > .deployed-image
