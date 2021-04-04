import * as pulumi from "@pulumi/pulumi"
import * as k8s from "@pulumi/kubernetes"

const config = new pulumi.Config()

const appNamespace = new k8s.core.v1.Namespace("microstream")
const appNamespaceName = appNamespace.metadata.name

const channelAppName = "microstream"
const channelAppLabels = {
  app: "microstream", // must match conf.clustering.cluster.name
}
const channelDeployment = new k8s.apps.v1.Deployment(channelAppName, {
  metadata: {
    namespace: appNamespaceName,
    labels: channelAppLabels,
  },
  spec: {
    replicas: 2,
    selector: {
      matchLabels: channelAppLabels,
    },
    template: {
      metadata: { labels: channelAppLabels },
      spec: {
        containers: [
          {
            name: channelAppName,
            image: "microstream:0.0.1",
            // image:  awsx.ecr.buildAndPushImage("database-side-service", "./databaseside").image(), :thinking
            env: [
              {
                name: "JAVA_OPTS",
                value: "-Dconfig.resource=channel-node-k8s.conf",
              },
            ],
            readinessProbe: {
              httpGet: {
                path: "/ready",
                port: "management",
              },
            },
            livenessProbe: {
              httpGet: {
                path: "/alive",
                port: "management",
              },
            },
            ports: [
              {
                name: "management",
                containerPort: 8558,
                protocol: "TCP",
              },
              {
                name: "http",
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
})

// // Minikube and Docker desktop does not implement services of type `LoadBalancer`;
// const isLocal = config.requireBoolean("isLocal")

// // for local run in minikube / docker desktop
// // kubectl get service --name--> kubectl port-forward service/<name> 8080:80

// const appName = "nginx"
// const appLabels = { app: appName }

// const deployment = new k8s.apps.v1.Deployment(appName, {
//   spec: {
//     selector: { matchLabels: appLabels },
//     replicas: 1,
//     template: {
//       metadata: { labels: appLabels },
//       spec: { containers: [{ name: appName, image: "nginx" }] },
//     },
//   },
// })

// // Allocate an IP to the Deployment.
// const frontend = new k8s.core.v1.Service(appName, {
//   metadata: { labels: deployment.spec.template.metadata.labels },
//   spec: {
//     type: isLocal ? "ClusterIP" : "LoadBalancer",
//     ports: [{ port: 80, targetPort: 80, protocol: "TCP" }],
//     selector: appLabels,
//   },
// })

// // const ingress = new k8s.networking.v1beta1.Ingress(appName, {
// //   // metadata: {},
// //   spec: {
// //     rules: [{
// //       host: ""
// //     }],
// //   },
// // })

// // When "done", this will print the public IP.
// export const ip = isLocal
//   ? frontend.spec.clusterIP
//   : frontend.status.loadBalancer.apply(
//       (lb) => lb.ingress[0]?.ip ?? lb.ingress[0]?.hostname
//     )

// // ingress controller: https://gist.github.com/vitobotta/219e9bcdd09b9b841167c33b493b65d5
