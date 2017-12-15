#!/usr/bin/env bash
DEBIAN_FRONTEND=noninteractive

sudo apt-get install git -y

git clone $1 tobuild