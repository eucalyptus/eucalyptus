#!/bin/bash
D=`date | sed "s/ //g"`
HOST1=$1
if [ -z "$HOST1" ]; then
    HOST1="127.0.0.1"
fi
HOST2=$2
if [ -z "$HOST2" ]; then
    HOST2="127.0.0.1"
fi
mkdir -p /tmp/$D
DBPASS=`./dbPass.sh`
if ( ! mysqldump --all-databases -u eucalyptus --password="$DBPASS" --port=8777 --protocol=TCP --host=$HOST1 --skip-extended-insert --skip-opt --lock-tables=false > /tmp/$D/db.$HOST1 2>/dev/null ); then
    rm -f /tmp/$D/db.$HOST1
fi

if ( ! mysqldump --all-databases -u eucalyptus --password="$DBPASS" --port=8777 --protocol=TCP --host=$HOST2 --skip-extended-insert --skip-opt --lock-tables=false > /tmp/$D/db.$HOST2 2>/dev/null ); then
    rm -f /tmp/$D/db.$HOST2
fi
echo "/tmp/$D $HOST1 $HOST2"
