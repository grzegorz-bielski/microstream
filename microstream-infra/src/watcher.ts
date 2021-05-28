import {
  LocalWorkspace,
  Stack,
  ConcurrentUpdateError,
} from "@pulumi/pulumi/automation"
import * as fs from "fs"
import * as path from "path"
import { config as dotEnvConfig } from "dotenv"
import { Observable } from "rxjs"
import { debounceTime, switchMap, filter, tap } from "rxjs/operators"

dotEnvConfig()

const srcPath = path.resolve("../")
const excluded = ["node_modules", "target", "microstream-db-pv"]

const pulumiSrcPath = path.resolve("./src/app/resources")

class StackService {
  private constructor(private readonly stack: Stack) {}

  static async create(workDir: string, stackName: string) {
    const stack = await LocalWorkspace.createOrSelectStack({
      stackName,
      workDir,
    })

    return new StackService(stack)
  }

  up() {
    return this.stack.up({ onOutput: console.info }).catch((e) => {
      switch (e.constructor) {
        case ConcurrentUpdateError:
          console.warn("ConcurrentUpdateError detected", e)
          return unit()
        default:
          throw e
      }
    })
  }

  down() {
    return this.stack.destroy({ onOutput: console.info })
  }
}

main().catch((e) => console.log(e))
// .finally(() => console.log("またね"))

async function main() {
  const stack = await StackService.create(pulumiSrcPath, "dev-watch")

  await stack.down()
  await stack.up()

  watchDir(srcPath)
    .pipe(
      debounceTime(2000),
      filter((f) => !excluded.some((e) => f.includes(e))),
      tap((f) => console.log(`${f} changed`)),
      switchMap(() => stack.up())
    )
    .subscribe({
      error: console.error,
    })
}

function watchDir(filePath: string) {
  return new Observable<string>((observer) => {
    const watcher = fs.watch(
      filePath,
      { recursive: true },
      (_event, fileName) => observer.next(fileName)
    )

    return () => watcher.close()
  })
}

function unit() {}
