#!/bin/bash

export PYTHONPATH=`pwd`

export INI_FILE=./eucaconsole/console.ini
if [ -f $HOME/console.ini ] ; then
	echo Using $HOME/console.ini
	INI_FILE=$HOME/console.ini
fi

python -tt euca-console-server -c $INI_FILE
