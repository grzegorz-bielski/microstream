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
