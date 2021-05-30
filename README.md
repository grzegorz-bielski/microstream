# microstream.ch app

## local dev

1. Create local k8 cluster in Docker using Kind
    ```sh
    ./microstream-infra/kind-setup.sh
    ```
2. Generate yamls for Skaffold through Pulumi
    ```sh
    ./microstream-infra/skaffold-setup.sh
    ```
3. Run Skaffold 
    ```sh
    skaffold dev
    ```
    Note that the command will fail the first time when run against newly created skaffold file complaining that the namespace is not found. A re-run should do the job.
