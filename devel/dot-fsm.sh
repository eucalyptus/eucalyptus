echo 'digraph bootstrap {
  size="80,80";
  ranksep=.2;
  nodesep=.2;
  clusterrank="local";
  rankdir="TB";'
sed '
	s/)\./)\n/g;
	s/^ *//g;
	s/State\.//g;
	s/Transition\.//g;
	s/\.class//g;
	s/\(\w\)( *\(.*\));$/\1( \2/g
' | \
awk '
/new StateMachineBuilder/{start="true"}
/newAtomicStateMachine/{start="false"}
/from\(/{from=$2}
/to\(/{to=$2}
/error\(/{err=$2}
/on\(/{on=$2}
/(run|condition)\(/ && start == "true" {
	run=gensub("\.run.|\.condition.","","g",$0);
	printf("%s -> %s [label=\"%s\\n%s\"];\n",from,to,on,run);
	if(err) printf("%s -> %s [label=\"error\"];\n", from, err)
}' | \
sed 's/\./=/g'
echo "}"
