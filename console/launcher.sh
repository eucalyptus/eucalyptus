#!/bin/bash

export PYTHONPATH=`pwd`

python -tt euca-console-server -c ./server/console.ini
