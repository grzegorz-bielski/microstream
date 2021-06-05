#!/usr/bin/env bash

set -o errexit

# based on: 
# - https://kind.sigs.k8s.io/docs/user/local-registry/
# - https://sookocheff.com/post/kubernetes/local-kubernetes-development-with-kind/

isAlreadyDefined=$( (kind get clusters | grep "kind") &>/dev/null && echo 1 || echo 0 )

if [ "$isAlreadyDefined" -eq 0 ]; then
  echo "Proceeding to create a new local cluster"
else 
  echo "Local cluster already created. Aborting"
  exit 0
fi


# create registry container unless it already exists
reg_name='kind-registry'
reg_port='5000'
running="$(docker inspect -f '{{.State.Running}}' "${reg_name}" 2>/dev/null || true)"
if [ "${running}" != 'true' ]; then
  docker run \
    -d --restart=always -p "127.0.0.1:${reg_port}:5000" --name "${reg_name}" \
    registry:2
fi

dbDataPath="$(pwd)/microstream-infra/data/volumes/microstream-db-pv"

# create a cluster with the local registry enabled in containerd
# `nodes` field is needed nginx ingress in kind: https://kind.sigs.k8s.io/docs/user/ingress/#create-cluster
cat <<EOF | kind create cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
containerdConfigPatches:
- |-
  [plugins."io.containerd.grpc.v1.cri".registry.mirrors."localhost:${reg_port}"]
    endpoint = ["http://${reg_name}:${reg_port}"]
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
    protocol: TCP
  - containerPort: 443
    hostPort: 443
    protocol: TCP
  extraMounts:
  - hostPath: $dbDataPath
    containerPath: /microstream-db-pv
EOF

# connect the registry to the cluster network
# (the network may already be connected)
docker network connect "kind" "${reg_name}" || true

# Document the local registry
# https://github.com/kubernetes/enhancements/tree/master/keps/sig-cluster-lifecycle/generic/1755-communicating-a-local-registry
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-registry-hosting
  namespace: kube-public
data:
  localRegistryHosting.v1: |
    host: "localhost:${reg_port}"
    help: "https://kind.sigs.k8s.io/docs/user/local-registry/"
EOF

# setup a nginx ingress for kind
# https://kind.sigs.k8s.io/docs/user/ingress/#ingress-nginx
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/provider/kind/deploy.yaml