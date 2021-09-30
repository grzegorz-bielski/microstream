import * as pulumi from "@pulumi/pulumi"

const config = new pulumi.Config()

const isLocal = config.require("isLocal")
