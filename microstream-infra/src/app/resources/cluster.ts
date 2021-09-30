import * as k8s from "@pulumi/kubernetes"
import * as eks from "@pulumi/eks"

export const provider = new k8s.Provider("render-yaml", {
  renderYamlToDirectory: "rendered",
})
