import * as k8s from "@pulumi/kubernetes"
import * as docker from "@pulumi/docker"
import * as path from "path"

import { provider, appNamespaceName } from "./shared"

const frontendAppName = "microstream-frontend"
const frontendAppLabels = {
  component: frontendAppName,
}

const frontendImage = new docker.Image("microstream-frontend-image", {
  build: path.resolve(__dirname, "../../../../microstream-fe/"),
  imageName: "localhost:5000/microstream-fe:latest",
  // registry: {
  //   server: "localhost:5000",
  //   username: "admin",
  //   password: "admin",
  // },
})

export const exportedFrontendPort = {
  name: "app-http",
  port: 80,
  targetPort: "app-http",
  protocol: "TCP",
}

export const frontendService = new k8s.core.v1.Service(
  "microstream-frontend-service",
  {
    metadata: {
      namespace: appNamespaceName,
      labels: frontendAppLabels,
    },
    spec: {
      selector: frontendAppLabels,
      ports: [exportedFrontendPort],
    },
  },
  {
    provider,
  }
)

export const frontendDeployment = new k8s.apps.v1.Deployment(
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
              image: frontendImage.imageName,
              ports: [
                {
                  name: exportedFrontendPort.targetPort,
                  containerPort: 3000,
                  protocol: exportedFrontendPort.protocol,
                },
              ],
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
