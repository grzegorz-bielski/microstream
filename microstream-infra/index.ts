import * as pulumi from "@pulumi/pulumi"
import * as k8s from "@pulumi/kubernetes"

// const config = new pulumi.Config()

const appNamespace = new k8s.core.v1.Namespace("microstream")
const appNamespaceName = appNamespace.metadata.name

///

const dbAppName = "microstream-db"
const dbAppLabels = {
  component: dbAppName,
}

const dbService = new k8s.core.v1.Service("microstream-db-service", {
  metadata: { labels: dbAppLabels, namespace: appNamespaceName },
  spec: {
    type: "ClusterIP",
    selector: dbAppLabels,
    ports: [
      {
        port: 5432,
        targetPort: 5432, // todo: take from config map
      },
    ],
  },
})

const dbPersistentVolumeClain = new k8s.core.v1.PersistentVolumeClaim(
  "microstream-db-pcv",
  {
    metadata: {
      namespace: appNamespaceName,
    },
    spec: {
      accessModes: ["ReadWriteOnce"],
      resources: {
        requests: {
          storage: "1Gi",
        },
      },
    },
  }
)

const dbStatefulSet = new k8s.apps.v1.StatefulSet(
  "microstream-db-statefulset",
  {
    metadata: {
      namespace: appNamespaceName,
    },
    spec: {
      serviceName: dbService.metadata.name,
      selector: {
        matchLabels: dbAppLabels,
      },
      template: {
        metadata: {
          labels: dbAppLabels,
        },
        spec: {
          containers: [
            {
              name: "postgres",
              image: "postgres:13.0",
              ports: [
                {
                  containerPort: 5432,
                  protocol: "tcp",
                },
              ],
              volumeMounts: [
                {
                  name: dbPersistentVolumeClain.metadata.name,
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
  }
)

////

const channelNodeAppName = "microstream-channel-node"
const channelNodeAppLabels = {
  component: channelNodeAppName,
  app: "microstream",
}

const channelNodeService = new k8s.core.v1.Service(
  "microstream-channel-node-service",
  {
    metadata: {
      namespace: appNamespaceName,
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

const channelNodeDeployment = new k8s.apps.v1.Deployment(
  "microstream-channel-node-deployment",
  {
    metadata: {
      name: channelNodeAppName,
      namespace: appNamespaceName,
      labels: channelNodeAppLabels,
    },
    spec: {
      replicas: 2,
      selector: {
        matchLabels: channelNodeAppLabels,
      },
      template: {
        metadata: { labels: channelNodeAppLabels },
        spec: {
          containers: [
            {
              name: channelNodeAppName,
              image: "microstream:0.0.1",
              // image:  awsx.ecr.buildAndPushImage("database-side-service", "./databaseside").image(), :thinking
              env:
                // todo: use config map
                [
                  {
                    name: "JAVA_OPTS",
                    value: "-Dconfig.resource=channel-node-k8s.conf",
                  },
                  {
                    name: "DB_HOST",
                    value: dbService.metadata.name,
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
              resources: {
                limits: {
                  memory: "1024Mi",
                },
                requests: {
                  cpu: "2",
                  memory: "1024Mi",
                },
              },
            },
          ],
        },
      },
    },
  }
)

///

const httpNodeAppName = "microstream-http-node"
const httpNodeAppLabels = {
  component: httpNodeAppName,
  app: "microstream",
}

const httpNodeService = new k8s.core.v1.Service(
  "microstream-http-node-service",
  {
    metadata: {
      namespace: appNamespaceName,
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
          port: 8080,
          targetPort: "app-http",
          protocol: "TCP",
        },
      ],
    },
  }
)

const httpNodeDeployment = new k8s.apps.v1.Deployment(
  "microstream-http-node-deployment",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: httpNodeAppLabels,
    },
    spec: {
      replicas: 1,
      selector: {
        matchLabels: httpNodeAppLabels,
      },
      template: {
        metadata: { labels: httpNodeAppLabels },
        spec: {
          containers: [
            {
              name: httpNodeAppName,
              image: "microstream:0.0.1",
              // image:  awsx.ecr.buildAndPushImage("database-side-service", "./databaseside").image(), :thinking
              env:
                // todo: use config map
                [
                  {
                    name: "JAVA_OPTS",
                    value: "-Dconfig.resource=channel-node-k8s.conf",
                  },
                  {
                    name: "DB_HOST",
                    value: dbService.metadata.name,
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
              resources: {
                limits: {
                  memory: "1024Mi",
                },
                requests: {
                  cpu: "2",
                  memory: "1024Mi",
                },
              },
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
    },
    spec: {
      ports: [
        {
          name: "app-http",
          port: 3000,
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
              image: "microstream-fe",
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

///

const ingress = new k8s.networking.v1beta1.Ingress("microstream-ingress", {
  metadata: {
    annotations: {
      "kubernetes.io/ingress.class": "nginx",
      "nginx.ingress.kubernetes.io/rewrite-target": "/$1",
    },
  },
  spec: {
    rules: [
      {
        host: "localhost",
        http: {
          paths: [
            {
              path: "/?(.*)",
              backend: {
                serviceName: frontendService.metadata.name,
                servicePort: 3000,
              },
            },
            {
              path: "/api/?(.*)",
              backend: {
                serviceName: httpNodeService.metadata.name,
                servicePort: 8080,
              },
            },
          ],
        },
      },
    ],
  },
})
