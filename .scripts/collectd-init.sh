#!/usr/bin/env bash

set -e

sudo apt-get update -y

sudo apt-get install -y gnupg software-properties-common

sudo apt-add-repository 'deb http://archive.ubuntu.com/ubuntu/ eoan main restricted universe'
sudo apt-add-repository 'deb http://security.ubuntu.com/ubuntu/ eoan-security main restricted universe'
echo "Package: *
Pin: release n=bionic
Pin-Priority: -10

Package: automake
Pin: release n=eoan
Pin-Priority: 500" > /etc/apt/preferences.d/myprefs.pref

echo "--------------------"
echo "Updated preferences:"
cat /etc/apt/preferences.d/myprefs.pref
echo "--------------------"

sudo apt-get install -y automake autoconf gcc make wget pkg-config 

sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0xB1998361219BD9C9
sudo apt-add-repository 'deb http://repos.azulsystems.com/ubuntu stable main'
sudo apt-get install -y zulu-8

export JAVA_HOME=/usr/lib/jvm/zulu-8-amd64

ls -FAl $JAVA_HOME

mkdir ./collectd-stage
pushd collectd-stage
export COLLECTD_HOME="$(pwd)"

wget https://github.com/collectd/collectd/releases/download/collectd-5.11.0/collectd-5.11.0.tar.bz2
tar -xvf collectd-5.11.0.tar.bz2

pushd collectd-5.11.0

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
popd

if [ ! -d "$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH" ]; then
    >&2 echo "CDP_USER_SOURCE_FOLDER_CONTAINER_PATH ('$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH') does not exist"
    exit 1
fi

GRADLEW_PATH="$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH/gradlew"
if [ ! -f "$GRADLEW_PATH" ]; then
    >&2 echo "GRADLEW_PATH ('$GRADLEW_PATH') does not exist"
    exit 1
fi

$GRADLEW_PATH -p "$CDP_USER_SOURCE_FOLDER_CONTAINER_PATH" --info --stacktrace -DisBuildServer=true --warning-mode=all :collectd:clean :collectd:resolveDependencies
