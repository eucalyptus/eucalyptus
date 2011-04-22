echo 'digraph bootstrap {
  size="80,80";
  ranksep=.4;
  nodesep=.4;
  clusterrank="local";'
sed 's/)\./)\n/g;s/^ *//g;s/State\.//g;s/Transition\.//g;s/\.class//g;s/run( *\(.*\));$/run:\1/g' | \
awk '
/new StateMachineBuilder/{start="true"}
/newAtomicStateMachine/{start="false"}
/on/{on=$2}
/from/{from=$2}
/to/{to=$2}
/error/{err=$2}
/run:/ && start == "true" {
	run=gensub("run:","","g",$0);
	printf("%s -> %s [label=\"%s %s\"];\n",from,to,on,run);
	if(err) printf("%s -> %s [label=\"error\"];\n", from, err)
}' | \
sed 's/\./_/g'
echo "}"