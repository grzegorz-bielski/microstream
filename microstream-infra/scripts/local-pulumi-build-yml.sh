#!/usr/bin/env bash
set -eu

export PULUMI_CONFIG_PASSPHRASE=passphrase

pulumi login --local
pulumi up -y -f -s dev --cwd ./microstream-infra/src/app --suppress-outputs
