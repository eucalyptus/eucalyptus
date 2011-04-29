if [ ! -e ${1} ]; then
	echo "Usage: ./devel/dot-fsm.sh path/to/java/file"
	exit 1
fi
FILE=${1}
# NOTE: change clusterrank to "local" to enable clustering
HEADER='digraph bootstrap {
  size="160,80";
  ranksep=.4;
  nodesep=.2;
  overlap="prism";
  clusterrank="global";
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
	if(from) printf("\t%s -> %s [style=\"bold\",wieght=\"2\",label=\"%s\\\\n%s\",%s];\n",from,to,on,action,labelProps);
	if(err) printf("\t%s -> %s [style=\"dashed\",wieght=\"0.5\",color=\"red\",label=\"%s\",%s];\n", to, err, on, labelProps)
}
END{
#	print "\tsubgraph cluster_all{\n\t\trank=max;"
#	for( i in states ) if(index(i,"_")==0) print "\t\t"i";"
#	print "\t}"
}
' | sed 's/\./=/g')"

SUBGRAPHS=$(echo "${TRANS}" | \
awk '
/\w*_.*->/{
	superstate=gensub(";","","g",gensub("_.*","","g",$1));
	trans[superstate]=$1"#"trans[superstate];
}
END{
	subgraphFormat="\tsubgraph cluster_%1$s{\n" \
  "\t\tlabel=\"%1$s\";\n" \
  "\t\trank=min;\n" \
  "\t\trankdir=\"LR\";\n" \
	"\t\t%2$s\n" \
	"\t}\n"
	for(i in trans) printf(subgraphFormat, i, trans[i]); 
}' | \
sed 's/\([^_]*\)_\([^#]*\)#/\1_\2 [label="\1_\2"];\n\t\t/g;s/#}/\n\t}/g'
)

echo "${HEADER}"
echo "${TRANS}"
echo "${SUBGRAPHS}"
echo "${FOOTER}"