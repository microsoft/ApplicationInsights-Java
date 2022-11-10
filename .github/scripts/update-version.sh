#!/bin/bash -e

version=$1

sed -i "s/version=.*/version=$version/" gradle.properties
