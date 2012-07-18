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

if [ -z "${1}" ]; then
  echo "Usage: diffcount.sh REVNO"
  exit 1
fi
REVNO=$1 
COUNT='BEGIN{line=0;comment=0}/^\ *\*/{comment++}{line++}END{printf("%d (%d,%d) # line-comments (lines,comments)", line-comment,line,comment)}'
echo Deleted $(bzr log -r${REVNO} -v -S | awk '/^\ *D/{print $2}' | xargs -i bzr cat -r$((REVNO-1)) {} | awk "${COUNT}")
echo Modified $(bzr log -r${REVNO} -v -S | awk '/^\ *M/{print $2}' | xargs -i bzr log -r${REVNO} -p {} | egrep '^[+-][^+-]' | awk "${COUNT}")
echo Renamed $(bzr log -r${REVNO} -v -S | awk '/^\ *R/{print $2}' | xargs -i bzr cat -r$((REVNO-1)) {} | awk "${COUNT}")
echo Added $(bzr log -r${REVNO} -v -S | awk '/^\ *A/{print $2}' | xargs -i bzr cat -r$((REVNO)) {} | awk "${COUNT}")
