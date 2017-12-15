#!/usr/bin/env bash
ssh -i ~/.ssh/holophrasekey -o StrictHostKeyChecking=no root@$1 'DEBIAN_FRONTEND=noninteractive bash -s' < $2 $3 $4