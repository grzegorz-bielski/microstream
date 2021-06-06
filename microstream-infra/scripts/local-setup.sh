#!/usr/bin/env bash
set -eu

# create local k8 cluster in Docker
./microstream-infra/scripts/local-kind-setup.sh

# generate yaml files for skaffold
./microstream-infra/scripts/local-pulumi-build-yml.sh

# generate skaffold yml file
./microstream-infra/scripts/local-skaffold-generate.sh