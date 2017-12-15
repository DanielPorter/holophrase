#!/usr/bin/env bash

apt-get update

apt-get install \
     apt-transport-https \
     ca-certificates \
     curl \
     gnupg2 \
     software-properties-common

curl -fsSL https://download.docker.com/linux/$(. /etc/os-release; echo "$ID")/gpg | sudo apt-key add -

add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/$(. /etc/os-release; echo "$ID") \
   $(lsb_release -cs) \
   stable"

apt-get update
apt-get install docker-ce -y

docker volume create pgdata
docker run --name holophrase-postgres -v pgdata:/var/lib/postgresql/data -e POSTGRES_PASSWORD=mysecretpassword -d postgres