# microstream.ch app

## local deps
- kind, skaffold, pulumi, 

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
