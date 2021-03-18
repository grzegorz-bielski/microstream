import * as pulumi from "@pulumi/pulumi"
import * as k8s from "@pulumi/kubernetes"

const config = new pulumi.Config()

// Minikube and Docker desktop does not implement services of type `LoadBalancer`;
const isLocal = config.requireBoolean("isLocal")

// for local run in minikube / docker desktop
// kubectl get service --name--> kubectl port-forward service/<name> 8080:80

const appName = "nginx"
const appLabels = { app: appName }
const deployment = new k8s.apps.v1.Deployment(appName, {
  spec: {
    selector: { matchLabels: appLabels },
    replicas: 1,
    template: {
      metadata: { labels: appLabels },
      spec: { containers: [{ name: appName, image: "nginx" }] },
    },
  },
})

// Allocate an IP to the Deployment.
const frontend = new k8s.core.v1.Service(appName, {
  metadata: { labels: deployment.spec.template.metadata.labels },
  spec: {
    type: isLocal ? "ClusterIP" : "LoadBalancer",
    ports: [{ port: 80, targetPort: 80, protocol: "TCP" }],
    selector: appLabels,
  },
})

// When "done", this will print the public IP.
export const ip = isLocal
  ? frontend.spec.clusterIP
  : frontend.status.loadBalancer.apply(
      (lb) => lb.ingress[0]?.ip ?? lb.ingress[0]?.hostname
    )
