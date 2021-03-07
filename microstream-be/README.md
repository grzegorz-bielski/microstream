# scala3-chat

## setup

1. install [coursier](https://get-coursier.io/docs/cli-installation)
1. `cs setup`

## local run

```zsh
# start backend node
sbt '; set javaOptions += "-Dconfig.resource=channel-node.conf" ; run'

# start api-gateway node
sbt '; set javaOptions += "-Dconfig.resource=http-node.conf" ; run'
```

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
