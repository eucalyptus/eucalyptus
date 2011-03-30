#!/bin/sh

# Usage: getusercred.sh [ account [ user ] ]

ACCOUNT="eucalyptus"
USER="admin"

if [ ! -z $1 ]; then
  ACCOUNT=$1
  if [ ! -z $2 ]; then
    USER=$2
  fi
fi

echo "Retrieving credentials for arn:aws:iam::$ACCOUNT:user/$USER"

DIR=euca-$ACCOUNT-$USER

mkdir $DIR
usr/sbin/euca_conf --get-credentials $DIR/euca.zip --cred-account $ACCOUNT --cred-user $USER
( cd $DIR; unzip euca.zip ) 
