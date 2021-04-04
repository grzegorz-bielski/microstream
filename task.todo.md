- build backend image:
  ~~- research scala packaging~~
  ~~- setup Docker build setup with `sbt-native-packager` and `sbt-docker`~~
  ~~- run with `docker-compose`: https://doc.akka.io/docs/akka/current/remoting-artery.html#remote-configuration-nat-artery~~
  - run with `k8s` and `cluster-bootstrap`: https://doc.akka.io/docs/akka/current/additional/deploying.html#cluster-bootstrap | https://doc.akka.io/docs/akka-management/current/kubernetes-deployment/forming-a-cluster.html
- local k8s deployment:
  - create all resources for local deployment
  - generate yamls https://www.pulumi.com/blog/kubernetes-yaml-generation/ for skaffold https://skaffold.dev/docs/quickstart/
  - ???
- aws k8s deployment
- CI / CD

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
