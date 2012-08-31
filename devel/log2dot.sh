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
#
# This file may incorporate work covered under the following copyright
# and permission notice:
#
#   Software License Agreement (BSD License)
#
#   Copyright (c) 2008, Regents of the University of California
#   All rights reserved.
#
#   Redistribution and use of this software in source and binary forms,
#   with or without modification, are permitted provided that the
#   following conditions are met:
#
#     Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer
#     in the documentation and/or other materials provided with the
#     distribution.
#
#   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
#   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
#   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
#   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
#   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
#   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
#   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
#   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
#   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
#   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
#   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
#   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
#   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
#   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
#   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
#   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.

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
