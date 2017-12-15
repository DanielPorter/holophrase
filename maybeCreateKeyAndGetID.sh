#!/usr/bin/env bash

bash createHolophraseKey.sh
export pubkey="$(bash getHolophraseKey.sh)"



echo "echoing res $res"
key_id="$(bash getDOKeys.sh | python parseKeysResponse.py)"
if [ $key_id = "none" ];
then
  key_id="$(bash createDOKey.sh `'{"name":"'$username'","public_key":"'"$pubkey"'"}'` | python parseKeyCreationResponse.py)"
fi

echo key_id
