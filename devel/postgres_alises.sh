# Copyright 2009-2012 Eucalyptus Systems, Inc.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; version 3 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see http://www.gnu.org/licenses/.
#
# Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
# CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
# additional information or have any questions.

# This script allows you to run postgres CLI tools such as "psql"
#  without providing authorization or connection parameters, provided
#  you are root. After SOURCING this script, you should be able to run
#  "psql -d database" as root. No further auth is required.
#
# NOTE: This file must be SOURCED, not executed: ". aliases.sh"

if [ -z "$EUCALYPTUS" ]; then 
	echo "EUCALYPTUS must be set"
else
	DATA_DIR=$EUCALYPTUS/var/lib/eucalyptus/db/data/
	PORT=8777

	alias psql="psql -h $DATA_DIR -p $PORT"
	alias pg_dump="pg_dump -h $DATA_DIR -p $PORT"
	alias pg_restore="pg_restore -h $DATA_DIR -p $PORT"
fi
