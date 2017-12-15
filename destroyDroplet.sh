#!/usr/bin/env bash

curl -X DELETE -H "Content-Type: application/json" -H "Authorization: Bearer $DOKEY" \
 "https://api.digitalocean.com/v2/droplets/$1"