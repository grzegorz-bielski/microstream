### resources

// chat examples
https://medium.com/@nnnsadeh/building-a-reactive-distributed-messaging-server-in-scala-and-akka-with-websockets-c70440c494e3
https://github.com/johanandren/chat-with-akka-http-websockets/blob/master/src/main/scala/chat/Server.scala
https://github.com/edwardyoon/heimdallr
https://github.com/PhyrexTsai/Akka-PubSub-Chatroom
https://github.com/ezoerner/scalable-chat
https://devcenter.heroku.com/articles/play-java-websockets-advanced
https://github.com/MartinSeeler/akka-cluster-chat
https://doc.akka.io/docs/akka/1.3.1/scala/tutorial-chat-server.html

// akka cluster stuff
https://github1s.com/michael-read/akka-typed-distributed-state-blog/blob/HEAD/build.sbt
https://www.lightbend.com/blog/how-to-distribute-application-state-with-akka-cluster-part-1-getting-started
https://docs.oracle.com/javase/tutorial/essential/environment/index.html

// devops / gitops
https://itnext.io/how-to-build-a-gitops-pipeline-on-a-stack-of-aws-services-63f7670b5f95
https://www.youtube.com/watch?v=IPLj9DqEVkg

// k8s
https://luminousmen.com/post/kubernetes-101




todos: - build backend image:
  ~~- research scala packaging~~
  ~~- setup Docker build setup with `sbt-native-packager` and `sbt-docker`~~
  ~~- run with `docker-compose`: https://doc.akka.io/docs/akka/current/remoting-artery.html#remote-configuration-nat-artery~~
  - run with `k8s` and `cluster-bootstrap`: https://doc.akka.io/docs/akka/current/additional/deploying.html#cluster-bootstrap
    - ~~fix RBAC auth (logs below)~~
    - ~~fix be image publish (docker aliases)~~
    - ~~fix akka-cluster contact points (contact-point-discovery.required-contact-point-nr)~~
    - ~~fix failing ingress and routing rules~~
    ~~` kubectl port-forward service/microstream-frontend-service-bcyysv3o 3000:app-http -n microstream-gmglxllo`~~
    - inspect failing db deployment
    - fix channel-node auth:

    ```
    Forbidden to communicate with Kubernetes API server; check RBAC settings. Response: [{"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"pods is forbidden: User \"system:serviceaccount:microstream-sgpvwi73:microstream-channel-node-service-account-ziikr9wu\" cannot list resource \"pods\" in API group \"\" in the namespace \"microstream\"","reason":"Forbidden","details":{"kind":"pods"},"code":403}
    ```

    https://doc.akka.io/docs/akka-management/current/kubernetes-deployment/forming-a-cluster.html 
    https://github.com/pulumi/examples/blob/master/aws-ts-k8s-mern-voting-app/index.ts
    https://github.com/michael-read/akka-typed-distributed-state-blog/blob/master/K8s/endpoints/endpoint-deployment.yaml
    https://www.lightbend.com/blog/how-to-distribute-application-state-with-akka-cluster-part-3-kubernetes-monitoring

- local k8s deployment:
  - create all resources for local deployment
  - generate yamls https://www.pulumi.com/blog/kubernetes-yaml-generation/ for skaffold https://skaffold.dev/docs/quickstart/
  - ???
- aws k8s deployment
  - certs: https://github.com/pulumi/cert-manager-examples
- CI / CD

```
SLF4J: A number (1) of logging calls during the initialization phase have been intercepted and are
SLF4J: now being replayed. These are subject to the filtering rules of the underlying logging system.
SLF4J: See also http://www.slf4j.org/codes.html#replay
[2021-04-25 21:28:31,046] [INFO] [akka.event.slf4j.Slf4jLogger] [microstream-akka.actor.default-dispatcher-3] [] - Slf4jLogger started
[2021-04-25 21:28:31,461] [INFO] [akka.remote.artery.tcp.ArteryTcpTransport] [microstream-akka.actor.default-dispatcher-3] [ArteryTcpTransport(akka://microstream)] - Remoting started with transport [Artery tcp]; listening on address [akka://microstream@10.244.0.8:2552] with UID [7999211423320419]
[2021-04-25 21:28:31,524] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-3] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.8:2552] - Starting up, Akka version [2.6.12] ...
[2021-04-25 21:28:31,812] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-3] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.8:2552] - Registered cluster JMX MBean [akka:type=Cluster]
[2021-04-25 21:28:31,813] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-3] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.8:2552] - Started up successfully
[2021-04-25 21:28:31,988] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-7] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.8:2552] - No seed nodes found in configuration, relying on Cluster Bootstrap for joining
[2021-04-25 21:28:31,993] [INFO] [akka.cluster.sbr.SplitBrainResolver] [microstream-akka.actor.default-dispatcher-7] [akka://microstream/system/cluster/core/daemon/downingProvider] - SBR started. Config: strategy [KeepMajority], stable-after [20 seconds], down-all-when-unstable [15 seconds], selfUniqueAddress [akka://microstream@10.244.0.8:2552#7999211423320419], selfDc [default].
[2021-04-25 21:28:33,304] [INFO] [com.microstream.RootGuardian$] [microstream-akka.actor.default-dispatcher-7] [akka://microstream/user] - Starting a new node with Set(http, k8s, dc-default) role(s)
[2021-04-25 21:28:34,879] [INFO] [com.microstream.RootGuardian$] [microstream-akka.actor.default-dispatcher-7] [akka://microstream/user] - Port: 2552, Default port: 2552
[2021-04-25 21:28:34,884] [INFO] [com.microstream.ClusterObserver$] [microstream-akka.actor.default-dispatcher-3] [akka://microstream/user/cluster-observer] - Started actor akka://microstream/user/cluster-observer - (class akka.actor.typed.internal.adapter.ActorRefAdapter)
[2021-04-25 21:28:34,898] [INFO] [akka.actor.typed.ActorSystem] [microstream-akka.actor.default-dispatcher-3] [] - Http server online at http://0:0:0:0:0:0:0:0:8080/
[2021-04-25 21:28:34,962] [INFO] [akka.management.internal.HealthChecksImpl] [microstream-akka.actor.default-dispatcher-3] [HealthChecksImpl(akka://microstream)] - Loading readiness checks [(cluster-membership,akka.management.cluster.scaladsl.ClusterMembershipCheck), (sharding,akka.cluster.sharding.ClusterShardingHealthCheck)]
[2021-04-25 21:28:34,965] [INFO] [akka.management.internal.HealthChecksImpl] [microstream-akka.actor.default-dispatcher-3] [HealthChecksImpl(akka://microstream)] - Loading liveness checks []
[2021-04-25 21:28:35,024] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-3] [AkkaManagement(akka://microstream)] - Binding Akka Management (HTTP) endpoint to: 10.244.0.8:8558
[2021-04-25 21:28:35,028] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-3] [AkkaManagement(akka://microstream)] - Including HTTP management routes for ClusterHttpManagementRouteProvider
[2021-04-25 21:28:35,125] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-12] [AkkaManagement(akka://microstream)] - Including HTTP management routes for ClusterBootstrap
[2021-04-25 21:28:35,132] [INFO] [akka.management.cluster.bootstrap.ClusterBootstrap] [microstream-akka.actor.default-dispatcher-12] [ClusterBootstrap(akka://microstream)] - Using self contact point address: http://10.244.0.8:8558
[2021-04-25 21:28:35,167] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-12] [AkkaManagement(akka://microstream)] - Including HTTP management routes for HealthCheckRoutes
[2021-04-25 21:28:35,201] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-12] [AkkaManagement(akka://microstream)] - Bound Akka Management (HTTP) endpoint to: 10.244.0.8:8558
[2021-04-25 21:28:35,203] [INFO] [akka.management.cluster.bootstrap.ClusterBootstrap] [microstream-akka.actor.default-dispatcher-3] [ClusterBootstrap(akka://microstream)] - Initiating bootstrap procedure using kubernetes-api method...
[2021-04-25 21:28:35,206] [INFO] [akka.management.cluster.bootstrap.ClusterBootstrap] [microstream-akka.actor.default-dispatcher-3] [ClusterBootstrap(akka://microstream)] - Bootstrap using `akka.discovery` method: kubernetes-api
[2021-04-25 21:28:35,515] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-12] [akka://microstream/system/bootstrapCoordinator] - Locating service members. Using discovery [akka.discovery.kubernetes.KubernetesApiServiceDiscovery], join decider [akka.management.cluster.bootstrap.LowestAddressJoinDecider], scheme [http]
[2021-04-25 21:28:35,517] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-12] [akka://microstream/system/bootstrapCoordinator] - Looking up [Lookup(microstream,Some(akka-mgmt),Some(tcp))]
[2021-04-25 21:28:35,518] [INFO] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-12] [KubernetesApiServiceDiscovery(akka://microstream)] - Querying for pods with label selector: [app=microstream]. Namespace: [microstream-fpuysgev]. Port: [Some(akka-mgmt)]
[2021-04-25 21:28:36,543] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-11] [akka://microstream/system/bootstrapCoordinator] - Located service members based on: [Lookup(microstream,Some(akka-mgmt),Some(tcp))]: [ResolvedTarget(10-244-0-8.microstream-fpuysgev.pod.cluster.local,Some(8558),Some(/10.244.0.8))], filtered to [10-244-0-8.microstream-fpuysgev.pod.cluster.local:8558]
[2021-04-25 21:28:36,928] [INFO] [akka.management.cluster.bootstrap.contactpoint.HttpClusterBootstrapRoutes] [microstream-akka.actor.default-dispatcher-11] [HttpClusterBootstrapRoutes(akka://microstream)] - Bootstrap request from 10.244.0.8:45810: Contact Point returning 0 seed-nodes []
[2021-04-25 21:28:37,055] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-7] [akka://microstream/system/bootstrapCoordinator] - Contact point [akka://microstream@10.244.0.8:2552] returned [0] seed-nodes []
[2021-04-25 21:28:37,541] [INFO] [akka.management.cluster.bootstrap.LowestAddressJoinDecider] [microstream-akka.actor.default-dispatcher-19] [LowestAddressJoinDecider(akka://microstream)] - Discovered [1] contact points, confirmed [1], which is less than the required [2], retrying