#!/usr/bin/env bash

set -e

sudo apt-get update -y

sudo apt-get install -y gnupg
sudo apt-get install -y software-properties-common

sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9
sudo apt-add-repository 'deb http://repos.azulsystems.com/ubuntu stable main'

sudo apt-get install -y autoconf
sudo apt-get install -y automake
sudo apt-get install -y zulu-8
sudo apt-get install -y gcc
sudo apt-get install -y make


export JAVA_HOME=/usr/lib/jvm/zulu-8-amd64

ls -FAl $JAVA_HOME

ROOT_DIR="$(pwd)"
export COLLECTD_HOME=$ROOT_DIR/collectd

wget https://github.com/collectd/collectd/releases/download/collectd-5.11.0/collectd-5.11.0.tar.bz2
tar -xvf collectd-5.11.0.tar.bz2 --directory $COLLECTD_HOME

pushd $COLLECTD_HOME/collectd-5.11.0

autoconf -v
./configure --with-java="$JAVA_HOME" --disable-dependency-tracking
automake -v

COLLECTD_LIB=$COLLECTD_HOME/lib
mkdir $COLLECTD_LIB

make collectd-api.jar
cp ./collectd-api.jar $COLLECTD_LIB

ls -FAl $COLLECTD_HOME
ls -FAl $COLLECTD_LIB

popd
