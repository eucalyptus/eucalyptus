
#
# This script allows you to run postgres CLI tools such as "psql"
#  without providing authorization or connection parameters, provided
#  you are root. After SOURCING this script, you should be able to run
#  "psql -d database" as root. No further auth is required.
#
# NOTE: This file must be SOURCED, not executed: ". aliases.sh"
#
# author: tom.werges
#

if [ -z "$EUCALYPTUS" ]; then 
	echo "EUCALYPTUS must be set"
else
	DATA_DIR=$EUCALYPTUS/var/lib/eucalyptus/db/data/
	PORT=8777

	alias psql="psql -h $DATA_DIR -p $PORT"
	alias pg_dump="pg_dump -h $DATA_DIR -p $PORT"
	alias pg_restore="pg_restore -h $DATA_DIR -p $PORT"
fi

