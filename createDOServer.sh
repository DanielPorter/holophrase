#!/usr/bin/env bash
DOKEY="$(cat doapikey)"

curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $DOKEY" \
-d '{"name":"'$1'","region":"nyc3","size":"2gb","image":"30117044","ssh_keys":["'$2'"],"backups":false,"ipv6":true,"user_data":null,"private_networking":null,"volumes": null,"tags":["web"]}' "https://api.digitalocean.com/v2/droplets"