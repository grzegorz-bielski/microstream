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

https://medium.com/@nnnsadeh/building-a-reactive-distributed-messaging-server-in-scala-and-akka-with-websockets-c70440c494e3
https://developer.lightbend.com/guides/akka-http-quickstart-scala/http-server.html
https://github.com/johanandren/chat-with-akka-http-websockets/blob/master/src/main/scala/chat/Server.scala
