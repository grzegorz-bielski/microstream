import * as k8s from "@pulumi/kubernetes"

import { provider, appNamespaceName, channelNodeReplicas } from "./shared"
import { dbService, dbStatefulSet } from "./database"

const channelNodeAppName = "microstream-channel-node"
const channelNodeAppLabels = {
  component: channelNodeAppName,
  app: "microstream",
}

export const channelNodeServiceAccount = new k8s.core.v1.ServiceAccount(
  "microstream-channel-node-service-account",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: channelNodeAppLabels,
    },
  },
  { provider }
)

export const channelNodeRole = new k8s.rbac.v1.Role(
  "microstream-channel-node-role",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: channelNodeAppLabels,
    },
    rules: [
      {
        apiGroups: [""],
        resources: ["pods"],
        verbs: ["get", "watch", "list"],
      },
    ],
  },
  { provider }
)

export const channelNodeRoleBinding = new k8s.rbac.v1.RoleBinding(
  "microstream-channel-node-role-binding",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: channelNodeAppLabels,
    },
    roleRef: {
      apiGroup: "rbac.authorization.k8s.io",
      kind: channelNodeRole.kind,
      name: channelNodeRole.metadata.name,
    },
    subjects: [
      {
        kind: channelNodeServiceAccount.kind,
        name: channelNodeServiceAccount.metadata.name,
        namespace: appNamespaceName,
      },
    ],
  },
  { provider }
)

export const channelNodeService = new k8s.core.v1.Service(
  "microstream-channel-node-service",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: channelNodeAppLabels,
    },
    spec: {
      selector: channelNodeAppLabels,
      ports: [
        {
          name: "akka-mgmt",
          port: 8558,
          targetPort: "akka-mgmt",
          protocol: "TCP",
        },
        {
          name: "akka-remoting",
          port: 2552,
          targetPort: "akka-remoting",
          protocol: "TCP",
        },
      ],
    },
  },
  { provider }
)

export const channelNodeDeployment = new k8s.apps.v1.Deployment(
  "microstream-channel-node-deployment",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: channelNodeAppLabels,
    },
    spec: {
      replicas: channelNodeReplicas,
      selector: {
        matchLabels: channelNodeAppLabels,
      },
      template: {
        metadata: { labels: channelNodeAppLabels },
        spec: {
          serviceAccountName: channelNodeServiceAccount.metadata.name,
          containers: [
            {
              name: channelNodeAppName,
              image: "localhost:5000/microstream-be:latest",
              // image:  awsx.ecr.buildAndPushImage("database-side-service", "./databaseside").image(), :thinking
              env:
                // todo: use config map
                [
                  {
                    name: "JAVA_OPTS",
                    value: "-Dconfig.resource=channel-node-k8s.conf",
                  },
                  {
                    name: "REQUIRED_CONTACT_POINT_NR",
                    value: channelNodeReplicas.toString(),
                  },
                  {
                    name: "DB_HOST",
                    value: dbService.metadata.name,
                  },
                  {
                    name: "DB_PORT",
                    value: "5432", // taken from dbService
                  },
                ],
              readinessProbe: {
                httpGet: {
                  path: "/ready",
                  port: "akka-mgmt",
                },
                initialDelaySeconds: 10,
                periodSeconds: 5,
              },
              livenessProbe: {
                httpGet: {
                  path: "/alive",
                  port: "akka-mgmt",
                },
                initialDelaySeconds: 90,
                periodSeconds: 30,
              },
              ports: [
                {
                  name: "akka-mgmt",
                  containerPort: 8558,
                  protocol: "TCP",
                },
                {
                  name: "akka-remoting",
                  containerPort: 2552,
                  protocol: "TCP",
                },
              ],
              // resources: {
              //   limits: {
              //     memory: "1024Mi",
              //   },
              //   requests: {
              //     cpu: "2",
              //     memory: "1024Mi",
              //   },
              // },
            },
          ],
        },
      },
    },
  },
  {
    dependsOn: dbStatefulSet,
    provider,
  }
)
