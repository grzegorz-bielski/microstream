import * as k8s from "@pulumi/kubernetes"

export const provider: k8s.Provider | undefined = void 0

export const appNamespace = new k8s.core.v1.Namespace(
  "microstream",
  {},
  { provider }
)
export const appNamespaceName = appNamespace.metadata.name

export const channelNodeReplicas = 2
