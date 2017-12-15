#!/usr/bin/env bash
DOKEY="$(cat doapikey)"
curl -X GET -H "Content-Type: application/json" -H "Authorization: Bearer $DOKEY" \
 "https://api.digitalocean.com/v2/droplets/"