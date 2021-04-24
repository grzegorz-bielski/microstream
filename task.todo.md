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
    - fix RBAC auth (logs below)
    - ~~fix be image publish (docker aliases)~~
    - inspect failing db deployment

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

````
[2021-04-24 18:07:17,326] [INFO] [akka.event.slf4j.Slf4jLogger] [microstream-akka.actor.default-dispatcher-3] [] - Slf4jLogger started
[2021-04-24 18:07:17,686] [INFO] [akka.remote.artery.tcp.ArteryTcpTransport] [microstream-akka.actor.default-dispatcher-3] [ArteryTcpTransport(akka://microstream)] - Remoting started with transport [Artery tcp]; listening on address [akka://microstream@10.244.0.14:2552] with UID [2096835853798384509]
[2021-04-24 18:07:17,744] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-3] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.14:2552] - Starting up, Akka version [2.6.12] ...
[2021-04-24 18:07:18,030] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-3] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.14:2552] - Registered cluster JMX MBean [akka:type=Cluster]
[2021-04-24 18:07:18,030] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-3] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.14:2552] - Started up successfully
[2021-04-24 18:07:18,213] [INFO] [akka.cluster.Cluster] [microstream-akka.actor.default-dispatcher-3] [Cluster(akka://microstream)] - Cluster Node [akka://microstream@10.244.0.14:2552] - No seed nodes found in configuration, relying on Cluster Bootstrap for joining
[2021-04-24 18:07:18,218] [INFO] [akka.cluster.sbr.SplitBrainResolver] [microstream-akka.actor.default-dispatcher-3] [akka://microstream/system/cluster/core/daemon/downingProvider] - SBR started. Config: strategy [KeepMajority], stable-after [20 seconds], down-all-when-unstable [15 seconds], selfUniqueAddress [akka://microstream@10.244.0.14:2552#2096835853798384509], selfDc [default].
[2021-04-24 18:07:19,438] [INFO] [com.microstream.RootGuardian$] [microstream-akka.actor.default-dispatcher-6] [akka://microstream/user] - Starting a new node with Set(http, k8s, dc-default) role(s)
[2021-04-24 18:07:21,009] [INFO] [com.microstream.RootGuardian$] [microstream-akka.actor.default-dispatcher-6] [akka://microstream/user] - Port: 2552, Default port: 2552
[2021-04-24 18:07:21,018] [INFO] [com.microstream.ClusterObserver$] [microstream-akka.actor.default-dispatcher-3] [akka://microstream/user/cluster-observer] - Started actor akka://microstream/user/cluster-observer - (class akka.actor.typed.internal.adapter.ActorRefAdapter)
[2021-04-24 18:07:21,031] [INFO] [akka.actor.typed.ActorSystem] [microstream-akka.actor.default-dispatcher-3] [] - Http server online at http://0:0:0:0:0:0:0:0:8080/
[2021-04-24 18:07:21,088] [INFO] [akka.management.internal.HealthChecksImpl] [microstream-akka.actor.default-dispatcher-13] [HealthChecksImpl(akka://microstream)] - Loading readiness checks [(cluster-membership,akka.management.cluster.scaladsl.ClusterMembershipCheck), (sharding,akka.cluster.sharding.ClusterShardingHealthCheck)]
[2021-04-24 18:07:21,089] [INFO] [akka.management.internal.HealthChecksImpl] [microstream-akka.actor.default-dispatcher-13] [HealthChecksImpl(akka://microstream)] - Loading liveness checks []
[2021-04-24 18:07:21,133] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-13] [AkkaManagement(akka://microstream)] - Binding Akka Management (HTTP) endpoint to: 10.244.0.14:8558
[2021-04-24 18:07:21,136] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-13] [AkkaManagement(akka://microstream)] - Including HTTP management routes for ClusterHttpManagementRouteProvider
[2021-04-24 18:07:21,225] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-13] [AkkaManagement(akka://microstream)] - Including HTTP management routes for ClusterBootstrap
[2021-04-24 18:07:21,231] [INFO] [akka.management.cluster.bootstrap.ClusterBootstrap] [microstream-akka.actor.default-dispatcher-13] [ClusterBootstrap(akka://microstream)] - Using self contact point address: http://10.244.0.14:8558
[2021-04-24 18:07:21,264] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-13] [AkkaManagement(akka://microstream)] - Including HTTP management routes for HealthCheckRoutes
[2021-04-24 18:07:21,308] [INFO] [akka.management.cluster.bootstrap.ClusterBootstrap] [microstream-akka.actor.default-dispatcher-11] [ClusterBootstrap(akka://microstream)] - Initiating bootstrap procedure using kubernetes-api method...
[2021-04-24 18:07:21,308] [INFO] [akka.management.cluster.bootstrap.ClusterBootstrap] [microstream-akka.actor.default-dispatcher-11] [ClusterBootstrap(akka://microstream)] - Bootstrap using `akka.discovery` method: kubernetes-api
[2021-04-24 18:07:21,317] [INFO] [akka.management.scaladsl.AkkaManagement] [microstream-akka.actor.default-dispatcher-11] [AkkaManagement(akka://microstream)] - Bound Akka Management (HTTP) endpoint to: 10.244.0.14:8558
[2021-04-24 18:07:21,607] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-11] [akka://microstream/system/bootstrapCoordinator] - Locating service members. Using discovery [akka.discovery.kubernetes.KubernetesApiServiceDiscovery], join decider [akka.management.cluster.bootstrap.LowestAddressJoinDecider], scheme [http]
[2021-04-24 18:07:21,609] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-11] [akka://microstream/system/bootstrapCoordinator] - Looking up [Lookup(microstream,Some(akka-mgmt),Some(tcp))]
[2021-04-24 18:07:21,610] [INFO] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-11] [KubernetesApiServiceDiscovery(akka://microstream)] - Querying for pods with label selector: [app=microstream]. Namespace: [microstream-jkwgj5u5]. Port: [Some(akka-mgmt)]
[2021-04-24 18:07:22,460] [WARN] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-13] [KubernetesApiServiceDiscovery(akka://microstream)] - Forbidden to communicate with Kubernetes API server; check RBAC settings. Response: [{"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"pods is forbidden: User \"system:serviceaccount:microstream-jkwgj5u5:default\" cannot list resource \"pods\" in API group \"\" in the namespace \"microstream-jkwgj5u5\"","reason":"Forbidden","details":{"kind":"pods"},"code":403}
]
[2021-04-24 18:07:22,467] [WARN] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-13] [akka://microstream/system/bootstrapCoordinator] - Resolve attempt failed! Cause: akka.discovery.kubernetes.KubernetesApiServiceDiscovery$KubernetesApiException: Forbidden when communicating with the Kubernetes API. Check RBAC settings.
[2021-04-24 18:07:24,570] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-6] [akka://microstream/system/bootstrapCoordinator] - Looking up [Lookup(microstream,Some(akka-mgmt),Some(tcp))]
[2021-04-24 18:07:24,570] [INFO] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-6] [KubernetesApiServiceDiscovery(akka://microstream)] - Querying for pods with label selector: [app=microstream]. Namespace: [microstream-jkwgj5u5]. Port: [Some(akka-mgmt)]
[2021-04-24 18:07:24,582] [WARN] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-11] [KubernetesApiServiceDiscovery(akka://microstream)] - Forbidden to communicate with Kubernetes API server; check RBAC settings. Response: [{"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"pods is forbidden: User \"system:serviceaccount:microstream-jkwgj5u5:default\" cannot list resource \"pods\" in API group \"\" in the namespace \"microstream-jkwgj5u5\"","reason":"Forbidden","details":{"kind":"pods"},"code":403}
]
[2021-04-24 18:07:24,582] [WARN] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-11] [akka://microstream/system/bootstrapCoordinator] - Resolve attempt failed! Cause: akka.discovery.kubernetes.KubernetesApiServiceDiscovery$KubernetesApiException: Forbidden when communicating with the Kubernetes API. Check RBAC settings.
[2021-04-24 18:07:28,833] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-6] [akka://microstream/system/bootstrapCoordinator] - Looking up [Lookup(microstream,Some(akka-mgmt),Some(tcp))]
[2021-04-24 18:07:28,834] [INFO] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-6] [KubernetesApiServiceDiscovery(akka://microstream)] - Querying for pods with label selector: [app=microstream]. Namespace: [microstream-jkwgj5u5]. Port: [Some(akka-mgmt)]
[2021-04-24 18:07:28,851] [WARN] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-13] [KubernetesApiServiceDiscovery(akka://microstream)] - Forbidden to communicate with Kubernetes API server; check RBAC settings. Response: [{"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"pods is forbidden: User \"system:serviceaccount:microstream-jkwgj5u5:default\" cannot list resource \"pods\" in API group \"\" in the namespace \"microstream-jkwgj5u5\"","reason":"Forbidden","details":{"kind":"pods"},"code":403}
]
[2021-04-24 18:07:28,851] [WARN] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-13] [akka://microstream/system/bootstrapCoordinator] - Resolve attempt failed! Cause: akka.discovery.kubernetes.KubernetesApiServiceDiscovery$KubernetesApiException: Forbidden when communicating with the Kubernetes API. Check RBAC settings.
[2021-04-24 18:07:29,122] [INFO] [akka.management.internal.HealthChecksImpl] [microstream-akka.actor.default-dispatcher-6] [HealthChecksImpl(akka://microstream)] - Check [akka.management.cluster.scaladsl.ClusterMembershipCheck] not ok
[2021-04-24 18:07:34,009] [INFO] [akka.management.internal.HealthChecksImpl] [microstream-akka.actor.default-dispatcher-20] [HealthChecksImpl(akka://microstream)] - Check [akka.management.cluster.scaladsl.ClusterMembershipCheck] not ok
[2021-04-24 18:07:37,841] [INFO] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-11] [akka://microstream/system/bootstrapCoordinator] - Looking up [Lookup(microstream,Some(akka-mgmt),Some(tcp))]
[2021-04-24 18:07:37,841] [INFO] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-11] [KubernetesApiServiceDiscovery(akka://microstream)] - Querying for pods with label selector: [app=microstream]. Namespace: [microstream-jkwgj5u5]. Port: [Some(akka-mgmt)]
[2021-04-24 18:07:37,857] [WARN] [akka.management.cluster.bootstrap.internal.BootstrapCoordinator] [microstream-akka.actor.default-dispatcher-11] [akka://microstream/system/bootstrapCoordinator] - Resolve attempt failed! Cause: akka.discovery.kubernetes.KubernetesApiServiceDiscovery$KubernetesApiException: Forbidden when communicating with the Kubernetes API. Check RBAC settings.
[2021-04-24 18:07:37,857] [WARN] [akka.discovery.kubernetes.KubernetesApiServiceDiscovery] [microstream-akka.actor.default-dispatcher-11] [KubernetesApiServiceDiscovery(akka://microstream)] - Forbidden to communicate with Kubernetes API server; check RBAC settings. Response: [{"kind":"Status","apiVersion":"v1","metadata":{},"status":"Failure","message":"pods is forbidden: User \"system:serviceaccount:microstream-jkwgj5u5:default\" cannot list resource \"pods\" in API group \"\" in the namespace \"microstream-jkwgj5u5\"","reason":"Forbidden","details":{"kind":"pods"},"code":403}
]
````