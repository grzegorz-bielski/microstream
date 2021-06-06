import * as docker from "@pulumi/docker"
import * as pulumi from "@pulumi/pulumi"
import * as path from "path"
import * as fs from "fs"
import { exec } from "child_process"
import { promises as fsp } from "fs"
import { promisify } from "util"

const backendPath = path.resolve(__dirname, "../../../../microstream-be")
const devBackendDockerCtx = `${backendPath}/`
const devBackendDockerfile = `${devBackendDockerCtx}dev.Dockerfile`
const prodBackendDockerCtx = `${backendPath}/target/docker/stage/`
const prodBackendDockerfile = `${prodBackendDockerCtx}Dockerfile`

export const backendImage = pulumi.output(
  pathExists(devBackendDockerCtx)
    .then((exists) => (exists ? Promise.resolve() : createBackendDockerfile()))
    .then(
      () =>
        new docker.Image("microstream-backend-image", {
          build: {
            context: devBackendDockerCtx,
            dockerfile: devBackendDockerfile,
          },
          imageName: "localhost:5000/microstream-be:latest",
          // registry: {
          //   server: "localhost:5000",
          //   username: "admin",
          //   password: "admin",
          // },
        })
    )
)

function pathExists(p: string) {
  return fsp
    .access(p, fs.constants.F_OK)
    .then(() => true)
    .catch(() => false)
}

function createBackendDockerfile() {
  console.log(`Generating image ${prodBackendDockerCtx} through sbt...`)

  return promisify(exec)(`sbt "docker:stage"`, { cwd: backendPath }).then(
    (streams) => {
      console.log(streams.stdout)
      console.error(streams.stdout)
    }
  )
}
