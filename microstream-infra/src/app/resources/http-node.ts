import * as k8s from "@pulumi/kubernetes"

import {
  provider,
  appNamespaceName,
  httpNodeReplicas,
  appConfigMap,
} from "./shared"

const httpNodeAppName = "microstream-http-node"
const httpNodeAppLabels = {
  component: httpNodeAppName,
  app: "microstream",
}

export const httpNodeServiceAccount = new k8s.core.v1.ServiceAccount(
  "microstream-http-node-service-account",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: httpNodeAppLabels,
    },
  },
  {
    provider,
  }
)

export const httpNodeRole = new k8s.rbac.v1.Role(
  "microstream-http-node-role",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: httpNodeAppLabels,
    },
    rules: [
      {
        apiGroups: [""],
        resources: ["pods"],
        verbs: ["get", "watch", "list"],
      },
    ],
  },
  {
    provider,
  }
)

export const httpNodeRoleBinding = new k8s.rbac.v1.RoleBinding(
  "microstream-http-node-role-binding",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: httpNodeAppLabels,
    },
    roleRef: {
      apiGroup: "rbac.authorization.k8s.io",
      kind: httpNodeRole.kind,
      name: httpNodeRole.metadata.name,
    },
    subjects: [
      {
        kind: httpNodeServiceAccount.kind,
        name: httpNodeServiceAccount.metadata.name,
        namespace: appNamespaceName,
      },
    ],
  },
  {
    provider,
  }
)

export const exportedHttpNodePort = {
  name: "app-http",
  port: 80,
  targetPort: "app-http",
  protocol: "TCP",
}

export const httpNodeService = new k8s.core.v1.Service(
  "microstream-http-node-service",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: httpNodeAppLabels,
    },
    spec: {
      selector: httpNodeAppLabels,
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
        exportedHttpNodePort,
      ],
    },
  },
  {
    provider,
  }
)

export const httpNodeDeployment = new k8s.apps.v1.Deployment(
  "microstream-http-node-deployment",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: httpNodeAppLabels,
    },
    spec: {
      replicas: httpNodeReplicas,
      selector: {
        matchLabels: httpNodeAppLabels,
      },
      template: {
        metadata: { labels: httpNodeAppLabels },
        spec: {
          serviceAccountName: httpNodeServiceAccount.metadata.name,
          containers: [
            {
              name: httpNodeAppName,
              image: "localhost:5000/microstream-be:latest",
              // image:  awsx.ecr.buildAndPushImage("database-side-service", "./databaseside").image(), :thinking
              env: [
                {
                  name: "JAVA_OPTS",
                  valueFrom: {
                    configMapKeyRef: {
                      name: appConfigMap.metadata.name,
                      key: "HTTP_NODE_JAVA_OPTS",
                    },
                  },
                },
                {
                  name: "REQUIRED_CONTACT_POINT_NR",
                  valueFrom: {
                    configMapKeyRef: {
                      name: appConfigMap.metadata.name,
                      key: "HTTP_NODES",
                    },
                  },
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
                {
                  name: exportedHttpNodePort.targetPort,
                  containerPort: 8080,
                  protocol: exportedHttpNodePort.protocol,
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
    provider,
  }
)
