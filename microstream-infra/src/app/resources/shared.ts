import { config as dotEnvConfig } from "dotenv"
import * as k8s from "@pulumi/kubernetes"

export const exportedDbPortNumber = 5432

export const channelNodeReplicas = 2
export const httpNodeReplicas = 1

// todo: take from pulumi secret
export const dbCredentials = {
  username: "postgres",
  password: "postgres",
}

// shared conf

const sharedAppName = "microstream-shared"
const sharedAppLabels = {
  component: sharedAppName,
}

export const provider = new k8s.Provider("render-yaml", {
  renderYamlToDirectory: "rendered",
})

export const appNamespace = new k8s.core.v1.Namespace(
  "microstream",
  {
    metadata: {
      labels: sharedAppLabels,
    },
  },
  { provider }
)
export const appNamespaceName = appNamespace.metadata.name

export const appConfigMap = new k8s.core.v1.ConfigMap(
  "microstream-shared-config-map",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: sharedAppLabels,
    },
    data: {
      POSTGRES_DB: "postgres",
      POSTGRES_PORT: exportedDbPortNumber.toString(),
      CHANNEL_NODES: channelNodeReplicas.toString(),
      CHANNEL_NODE_JAVA_OPTS: "-Dconfig.resource=channel-node-k8s.conf",
      HTTP_NODES: httpNodeReplicas.toString(),
      HTTP_NODE_JAVA_OPTS: "-Dconfig.resource=http-node-k8s.conf",
    },
  },
  { provider }
)
