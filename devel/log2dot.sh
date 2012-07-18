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

REVNOS_BACK=212

echo 'digraph bootstrap {
  size="100,200";
  rankdir=TB;
  ranksep=.2;
  nodesep=.2;
  clusterrank="local";
'
echo -e "\n\n\n"

bzr log -r-${REVNOS_BACK}.. --show-ids -n0 | sed 's/^\ *//g;s/:\( \)*/:/g;s/ \[/_/g;s/]//g' | egrep -v 'message|timestamp' | \
awk -F: '
  $1 == "revno" {
    revno=$2
    color="grey"
  }
  $1 == "revno" && $2 ~ /merge/ {
    color="green"
  }
  $1 == "revision-id" {
    rid=$2;safe_rid=gensub("[-@.]","","g",$2);
  }
  $1 == "parent" {
    omg=$2;
    boo[omg]=omg
    boo[rid]=rid
    p=sprintf("%s\n\t%s -> %s;// [label=\"%s\"];",p,gensub("[-@.]","","g",omg),safe_rid,rid);
  }
  $1 == "branch nick" {
    nick=gensub("[-@.]","X","g",$2)
    names[nick]=$2
    delete boo[rid]
    branches[nick]=sprintf("%s\n%s [label=\"%s:%s\",color=\"%s\"];",branches[$2],safe_rid,nick,gensub("_merge","","g",revno),color)
  }
  NF != 2 {}
  END{
    for(n in names) {
      nick=gensub("[-@.]","X","g",names[n])
      printf("subgraph cluster_%s {\n\tlabel = \"%s\";\n\trankdir=LR\n\tnode [style=filled];\n\t%s\n}\n",nick,names[n],branches[nick]);
    }
    print p
    printf("subgraph cluster_origins { label = \"PARENT BRANCHES\";\n}");
    for(hi in boo) {
      printf("%s [label=\"", gensub("[-@.]","","g",boo[hi]))
      system("echo -n `bzr log -r"boo[hi]" | egrep \"branch nick:|revno:\" | sed s/.\*://g`")
      printf("\",color=\"red\"];\n")
    }
    printf("}");
  }
' | sed 's/branch nick: //g'
# | sed 's/\([^ ]\)-\([^ ]\)/\1\2/g;s/@/AT/g' | sort
#      
#    print parents

echo '}'
