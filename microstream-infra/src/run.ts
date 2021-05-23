import { LocalWorkspace } from "@pulumi/pulumi/automation"
import * as path from "path"

const srcPath = path.resolve("./src/app/resources")

;(async () => {
  const stack = await LocalWorkspace.createOrSelectStack({
    stackName: "dev-watch",
    workDir: srcPath,
  })

  const result = await stack.up({ onOutput: console.info })

  console.log(result)
})()
