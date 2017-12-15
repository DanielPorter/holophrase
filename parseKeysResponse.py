import os, sys, json

keys=json.load(sys.stdin)['ssh_keys']

filteredKeys=[k for k in keys if k['public_key']==os.environ['pubkey']]

if len(filteredKeys)>0:
  print(filteredKeys[0]['id'])
else:
  print("none")
