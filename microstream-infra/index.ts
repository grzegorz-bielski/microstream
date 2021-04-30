import * as pulumi from "@pulumi/pulumi"
import * as k8s from "@pulumi/kubernetes"

const config = new pulumi.Config()

// const isLocal = !!config.get("isLocal") ?? false

const appNamespace = new k8s.core.v1.Namespace("microstream")
export const appNamespaceName = appNamespace.metadata.name

///

const dbAppName = "microstream-db"
const dbAppLabels = {
  component: dbAppName,
}

// local volume setup

const dbStorageClass = new k8s.storage.v1.StorageClass("microstream-db-sc", {
  metadata: {
    namespace: appNamespaceName,
    labels: dbAppLabels,
  },
  provisioner: "kubernetes.io/no-provisioner",
  volumeBindingMode: "WaitForFirstConsumer",
})

const dbPersistentVolume = new k8s.core.v1.PersistentVolume(
  "microstream-db-pv",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: dbAppLabels,
    },
    spec: {
      capacity: {
        storage: "5Gi",
      },
      accessModes: ["ReadWriteOnce"],
      persistentVolumeReclaimPolicy: "Retain",
      storageClassName: "microstream-db-storage",
      local: {
        // should be created on the disk first (!) and be available in Kind
        path: "/microstream-db-pv/",
      },
      nodeAffinity: {
        required: {
          nodeSelectorTerms: [
            {
              matchExpressions: [
                {
                  key: "kubernetes.io/hostname",
                  operator: "In",
                  values: ["kind-control-plane"],
                },
              ],
            },
          ],
        },
      },
    },
  },
  { dependsOn: dbStorageClass }
)

//

const dbPersistentVolumeClaim = new k8s.core.v1.PersistentVolumeClaim(
  "microstream-db-pvc",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: dbAppLabels,
    },
    spec: {
      accessModes: ["ReadWriteOnce"],
      storageClassName: "microstream-db-storage",
      resources: {
        requests: {
          storage: "1Gi",
        },
      },
    },
  },
  {
    dependsOn: dbPersistentVolume,
  }
)

const dbService = new k8s.core.v1.Service("microstream-db-service", {
  metadata: {
    namespace: appNamespaceName,
    labels: dbAppLabels,
    annotations: {
      "pulumi.com/skipAwait": "true", // circular await dependency with `dbStatefulSet`
    },
  },
  spec: {
    selector: dbAppLabels,
    ports: [
      {
        port: 5432,
        targetPort: "app-postgres", // todo: take from config map
      },
    ],
  },
})

const dbStatefulSet = new k8s.apps.v1.StatefulSet(
  "microstream-db-statefulset",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: dbAppLabels,
    },
    spec: {
      serviceName: dbService.metadata.name,
      selector: {
        matchLabels: dbAppLabels,
      },
      // volumeClaimTemplates
      template: {
        metadata: {
          labels: dbAppLabels,
        },
        spec: {
          volumes: [
            {
              name: "postgres-db-volume",
              persistentVolumeClaim: {
                claimName: dbPersistentVolumeClaim.metadata.name,
              },
            },
          ],
          containers: [
            {
              name: "postgres",
              image: "postgres:13.0",
              args: [
                "-c",
                // should be hardly necessary
                // but for some reason the app uses 206 connections when active (postgres default is 100)
                // and 126 after booted :thinking
                // (could be related to flaky akka projection connections :thinking)
                "max_connections=1000",
              ],
              // imagePullPolicy: isLocal ? "Never" : "Always",
              ports: [
                {
                  name: "app-postgres",
                  containerPort: 5432,
                  protocol: "TCP",
                },
              ],
              volumeMounts: [
                {
                  name: "postgres-db-volume",
                  mountPath: "/var/lib/postgresql/data",
                  subPath: "postgress",
                },
              ],
              // todo: use config map
              env: [
                {
                  name: "POSTGRES_DB",
                  value: "postgres",
                },
                {
                  name: "POSTGRES_USER",
                  value: "postgres",
                },
                {
                  name: "POSTGRES_PASSWORD",
                  value: "postgres", // todo: use secret
                },
                {
                  name: "POSTGRES_PORT",
                  value: "5432",
                },
              ],
            },
          ],
        },
      },
    },
  },
  {
    dependsOn: dbPersistentVolumeClaim,
  }
)

////

const channelNodeAppName = "microstream-channel-node"
const channelNodeAppLabels = {
  component: channelNodeAppName,
  app: "microstream",
}

const channelNodeServiceAccount = new k8s.core.v1.ServiceAccount(
  "microstream-channel-node-service-account",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: channelNodeAppLabels,
    },
  }
)

const channelNodeRole = new k8s.rbac.v1.Role("microstream-channel-node-role", {
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
})

const channelNodeRoleBinding = new k8s.rbac.v1.RoleBinding(
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
  }
)

const channelNodeService = new k8s.core.v1.Service(
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
  }
)

const channelNodeReplicas = 2

const channelNodeDeployment = new k8s.apps.v1.Deployment(
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
              // imagePullPolicy: isLocal ? "Never" : "Always",
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
  }
)

export const channelNodePods = channelNodeDeployment.spec.template.metadata.name

///

const httpNodeAppName = "microstream-http-node"
const httpNodeAppLabels = {
  component: httpNodeAppName,
  app: "microstream",
}

const httpNodeServiceAccount = new k8s.core.v1.ServiceAccount(
  "microstream-http-node-service-account",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: httpNodeAppLabels,
    },
  }
)

const httpNodeRole = new k8s.rbac.v1.Role("microstream-http-node-role", {
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
})

const httpNodeRoleBinding = new k8s.rbac.v1.RoleBinding(
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
  }
)

const httpNodeService = new k8s.core.v1.Service(
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
        {
          name: "app-http",
          port: 80,
          targetPort: "app-http",
          protocol: "TCP",
        },
      ],
    },
  }
)

const httpNodeReplicas = 2

const httpNodeDeployment = new k8s.apps.v1.Deployment(
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
              // imagePullPolicy: isLocal ? "Never" : "Always",
              // image:  awsx.ecr.buildAndPushImage("database-side-service", "./databaseside").image(), :thinking
              env:
                // todo: use config map
                [
                  {
                    name: "JAVA_OPTS",
                    value: "-Dconfig.resource=http-node-k8s.conf",
                  },
                  {
                    name: "REQUIRED_CONTACT_POINT_NR",
                    value: httpNodeReplicas.toString(),
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
                  name: "app-http",
                  containerPort: 8080,
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
  }
)

///

const frontendAppName = "microstream-frontend"
const frontendAppLabels = {
  component: frontendAppName,
}

const frontendService = new k8s.core.v1.Service(
  "microstream-frontend-service",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: frontendAppLabels,
    },
    spec: {
      selector: frontendAppLabels,
      ports: [
        {
          name: "app-http",
          port: 80,
          targetPort: "app-http",
          protocol: "TCP",
        },
      ],
    },
  }
)

const frontendDeployment = new k8s.apps.v1.Deployment(
  "microstream-frontend-deployment",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: frontendAppLabels,
    },
    spec: {
      replicas: 1,
      selector: {
        matchLabels: frontendAppLabels,
      },
      template: {
        metadata: {
          labels: frontendAppLabels,
        },
        spec: {
          containers: [
            {
              name: frontendAppName,
              image: "localhost:5000/microstream-fe:latest",
              ports: [
                {
                  name: "app-http",
                  containerPort: 3000,
                  protocol: "TCP",
                },
              ],
            },
          ],
        },
      },
    },
  }
)

/// dashboard

// const k8sDashboard = new k8s.yaml.ConfigFile("microstream-dashboard-ui", {
//   file:
//     "https://raw.githubusercontent.com/kubernetes/dashboard/v2.0.0/aio/deploy/recommended.yaml",
// })

// needs user: https://github.com/kubernetes/dashboard/blob/master/docs/user/access-control/creating-sample-user.md
// https://kubernetes.io/docs/tasks/access-application-cluster/web-ui-dashboard/

///

const ingressAppName = "microstream-ingress"
const ingressAppLabels = {
  component: ingressAppName,
}

const ingress = new k8s.networking.v1.Ingress("microstream-ingress", {
  metadata: {
    namespace: appNamespaceName,
    labels: ingressAppLabels,
    annotations: {
      "kubernetes.io/ingress.class": "nginx",
      // "nginx.ingress.kubernetes.io/rewrite-target": "/$1",
    },
  },
  spec: {
    rules: [
      {
        http: {
          paths: [
            {
              path: "/api",
              pathType: "Prefix",
              backend: {
                service: {
                  name: httpNodeService.metadata.name,
                  port: {
                    name: "app-http",
                  },
                },
              },
            },
            {
              path: "/",
              pathType: "Prefix",
              backend: {
                service: {
                  name: frontendService.metadata.name,
                  port: {
                    name: "app-http",
                  },
                },
              },
            },
          ],
        },
      },
    ],
  },
})
