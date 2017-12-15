#!/usr/bin/env bash
DEBIAN_FRONTEND=noninteractive

sudo apt-get install git -y

rm -rf tobuild
git clone $1 tobuild