# microstream

An _(unfinished)_ chat app that used to be a playground for trying some ideas.

## backend
Done in Scala 2, downgraded from Scala 3 as it wasn't ready-enough at that moment.
Heavy usage of actor paradigm using akka-typed and akka-cluster with sharding, routers and receptionists. Uses event sourcing with half baked CQRS. Persistence is backend by Postgres, both for read and write sides. Uses slick with codegen for type-safe queries. No tests whatsoever... yet!
## infra
Infrastructure as code using pulumi and targeting generic kubernetes cluster. The application level cluster backend by akka-cluster is split into two roles - `http` nodes and `backend` nodes. Those are deployed as separate k8s deployments. Only `http` nodes can communicate with the frontend server. Nginx based ingress works as an api-gateway to the system.
Includes a pretty neat in-cluster local development environment in which pulumi renders k8s yamls and generates skaffold config, which handles the rest. The local k8s cluster lives inside Docker thanks to kind and has its own docker registry, so should be quite portable.
No remote deployment setup and CI at the moment though.

## frontend
Universal TS app using next.js. Totally unfinished, even more than the rest. 

## local deps
- Docker
- node, npm
- sbt, scala, java
- kind, skaffold, pulumi

## local dev

1. Create local k8 cluster in Docker, render ymls, generate skaffold config
    ```sh
    ./microstream-infra/local-setup.sh
    ```
2. Run skaffold 
    ```sh
    skaffold dev
    ```
    Note that the command will fail the first time when run against newly created skaffold file complaining that the namespace is not found. A re-run should do the job.
