#!/bin/bash -e

grep -Po "version=\K[0-9]+.[0-9]+.[0-9]+" gradle.properties
