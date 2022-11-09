#!/bin/bash -e

sed -i "s/version=.*/version=$1/" gradle.properties
