import { LocalWorkspace } from "@pulumi/pulumi/automation"
import * as fs from "fs"
import * as path from "path"

import { Observable, from } from "rxjs"
import { debounceTime, switchMap } from "rxjs/operators"

// import * as app from "./app"

const srcPath = path.resolve("./src/app/resources")

watchDir(srcPath)
  .pipe(
    debounceTime(2000),
    switchMap(() =>
      from(
        LocalWorkspace.createOrSelectStack({
          stackName: "dev-watch",
          workDir: srcPath,
        })
      )
    ),
    switchMap((stack) => from(stack.up({ onOutput: console.info })))
  )
  .subscribe({
    complete: console.info,
    error: console.error,
  })

function watchDir(filePath: string) {
  return new Observable<string>((observer) => {
    fs.watch(filePath, { recursive: true }, (event, fileName) => {
      console.log("event", event)
      console.log("fileName", fileName)
      observer.next(fileName)
    })
  })
}
