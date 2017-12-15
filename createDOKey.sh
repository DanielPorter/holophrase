#!/usr/bin/env bash
DOKEY="$(cat doapikey)"

curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer $DOKEY" -d "$1" "https://api.digitalocean.com/v2/account/keys"

