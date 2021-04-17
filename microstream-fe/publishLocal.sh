#!/bin/sh

set -o errexit

docker build -t microstream-fe .
docker tag microstream-fe:latest localhost:5000/microstream-fe:latest
docker push localhost:5000/microstream-fe:latest