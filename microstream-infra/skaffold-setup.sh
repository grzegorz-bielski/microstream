#!/bin/sh
set -eux

export PULUMI_CONFIG_PASSPHRASE=passphrase

pulumi login --local

# generate yaml files for skaffold
pulumi up -y -f -s dev --cwd ./microstream-infra/src/app --suppress-outputs

rm -f skaffold.yaml
skaffold init