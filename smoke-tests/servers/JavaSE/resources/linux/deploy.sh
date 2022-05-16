#!/bin/bash

JARFILE=`readlink -f $1`

cp $JARFILE app.jar
