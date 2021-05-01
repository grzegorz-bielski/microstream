import * as k8s from "@pulumi/kubernetes"

import { provider, appNamespaceName } from "./shared"
import { httpNodeService, exportedHttpNodePort } from "./http-node"
import { frontendService, exportedFrontendPort } from "./frontend"

const ingressAppName = "microstream-ingress"
const ingressAppLabels = {
  component: ingressAppName,
}

export const ingress = new k8s.networking.v1.Ingress(
  "microstream-ingress",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: ingressAppLabels,
      annotations: {
        "kubernetes.io/ingress.class": "nginx",
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
                      name: exportedHttpNodePort.name,
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
                      name: exportedFrontendPort.name,
                    },
                  },
                },
              },
            ],
          },
        },
      ],
    },
  },
  { provider }
)
