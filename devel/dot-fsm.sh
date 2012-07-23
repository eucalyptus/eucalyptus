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

if [ ! -e ${1} ]; then
	echo "Usage: ./devel/dot-fsm.sh path/to/java/file"
	exit 1
fi
FILE=${1}
# NOTE: change clusterrank to "local" to enable clustering
HEADER='digraph bootstrap {
  size="160,80";
  ranksep=.1;
  nodesep=.4;
  color="gray";
  overlap="prism";
  clusterrank="global";
  center="true";
  rankdir="BT";'
FOOTER="}"
TRANS="$(cat ${FILE} | sed 's/)\./)\n/g;
s/^ *//g;
s/State\.//g;
s/Transition\.//g;
s/\.class//g;
s/\(\w\)( *\(.*\));$/\1( \2/g' | \
awk '
/new StateMachineBuilder/{start="true"}
/newAtomicStateMachine/{start="false"}
/from\(/{from=$2}
/to\(/{to=$2}
/error\(/{err=$2}
/on\(/{on=$2}
/run\(/ && start == "true" {
	states[from]=from;
	labelProps="fontsize=\"8.0\"";
	action=gensub("(.*run.|.*condition\\()","","g",$0);
	if(from) printf("\t%s -> %s [style=\"bold\",wieght=\"2\",label=<<FONT POINT-SIZE=\"12\">%s</FONT><BR/><FONT POINT-SIZE=\"12\">%s</FONT>>,%s];\n",from,to,on,action,labelProps);
	if(err) printf("\t%s -> %s [style=\"dashed\",wieght=\"0.5\",color=\"red\",label=\"%s\",%s];\n", to, err, on, labelProps)
}
END{
	print "\tsubgraph cluster_all{\n\t\trank=max;"
	for( i in states ) if(index(i,"_")==0) print "\t\t"i" [shape=\"doublecircle\",weight=\"2.0\",label=<<FONT POINT-SIZE=\"18\">"i"</FONT>>];"
	print "\t}"
}
' | sed 's/\./=/g;/^\t* \[shape.*/d')"

SUBGRAPHS=$(echo "${TRANS}" | \
awk '
/\w*_.*->/{
	superstate=gensub(";","","g",gensub("_.*","","g",$1));
	trans[superstate]=$1"#"trans[superstate];
}
END{
	subgraphFormat="\tsubgraph cluster_%1$s{\n" \
  "\t\tstyle=rounded;\n" \
  "\t\trankdir=\"BT\";\n" \
  "\t\t%1$s [shape=\"doublecircle\",weight=\"2.0\",label=<<FONT POINT-SIZE=\"18\">%1$s</FONT>>]\n" \
  "\t\t%2$s\n" \
	"\t}\n"
	for(i in trans) printf(subgraphFormat, i, trans[i]); 
}' | \
sed 's/\([^_]*\)_\([^#]*\)#/\1_\2  [label=<<FONT POINT-SIZE="10">\1_\2<\/FONT>>];\n\t\t/g;s/#}/\n\t}/g'
)

echo "${HEADER}"
echo "${SUBGRAPHS}"
echo "${TRANS}"
echo "${FOOTER}"
