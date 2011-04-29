echo 'digraph bootstrap {
  size="80,80";
  ranksep=.2;
  nodesep=.2;
  clusterrank="local";
  rankdir="TB";'
sed 's/)\./)\n/g;
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
/(run|condition)\(/ && start == "true" {
	labelProps="fontsize=\"8.0\"";
	action=gensub("(.*run.|.*condition\\()","","g",$0);
	printf("%s -> %s [label=\"%s\\n%s\",%s];\n",from,to,on,action,labelProps);
	if(err) printf("%s -> %s [label=\"error\",color=\"gray\",%s];\n", from, err,labelProps)
}' | \
sed 's/\./=/g'
echo "}"
