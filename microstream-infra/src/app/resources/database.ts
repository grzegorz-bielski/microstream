import * as k8s from "@pulumi/kubernetes"

import { provider, appNamespaceName, channelNodeReplicas } from "./shared"

const dbAppName = "microstream-db"
const dbAppLabels = {
  component: dbAppName,
}

// todo: some of the `dependsOn` might not be necessary

// local volume setup

export const dbStorageClass = new k8s.storage.v1.StorageClass(
  "microstream-db-sc",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: dbAppLabels,
    },
    provisioner: "kubernetes.io/no-provisioner",
    volumeBindingMode: "WaitForFirstConsumer",
  },
  { provider }
)

export const dbPersistentVolume = new k8s.core.v1.PersistentVolume(
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
  {
    dependsOn: dbStorageClass,
    provider,
  }
)

//

export const dbPersistentVolumeClaim = new k8s.core.v1.PersistentVolumeClaim(
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
    provider,
  }
)

export const dbService = new k8s.core.v1.Service(
  "microstream-db-service",
  {
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
  },
  { provider }
)

// prettier-ignore
const maxPgConnections =
  channelNodeReplicas * (
    20 +  // read side pool
    40 +  // journal pool
    40    // projection pool
  ) + 50 // buffer

export const dbStatefulSet = new k8s.apps.v1.StatefulSet(
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
              args: ["-c", `max_connections=${maxPgConnections}`],
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
    provider,
  }
)
