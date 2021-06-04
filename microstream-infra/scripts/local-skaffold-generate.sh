#!/usr/bin/env bash
set -eu

manifests=()

walk_dir () {
    for pathname in "$1"/*; do
        if [ -d "$pathname" ]; then
            walk_dir "$pathname"
        else
            manifests+=("$pathname")
        fi
    done

}

prepare_skaffold_conf() {

walk_dir "microstream-infra/src/app/rendered/1-manifest"

local nl
nl=$'\n'

local str_manifest

local lines
for each in ${manifests[*]}
do
  lines+=("     - $each $nl")
done

str_manifest="${lines[*]}"

# local ymlConf
(
  cat <<EOF
apiVersion: skaffold/v2beta16
kind: Config
metadata:
  name: microstream
build:
  artifacts:
  - image: localhost:5000/microstream-be
    context: microstream-be/target/docker/stage
    buildpacks:
      builder: heroku/pack:20-build
      buildpack: heroku/scala
  - image: localhost:5000/microstream-fe
    context: microstream-fe
    docker:
      dockerfile: dev.Dockerfile
    sync:
      infer:
        - "**/*.{js,jsx,ts,tsx,css,scss,html}"
deploy:
  kubectl:
    manifests:
$str_manifest
EOF
) > skaffold.yml
}

rm -f skaffold.yaml
prepare_skaffold_conf