#!/usr/bin/env bash
if [ ! -f ~/.ssh/holophrasekey ]; then
    ssh-keygen -t rsa -f ~/.ssh/holophrasekey -C $USER -q -N ""
fi