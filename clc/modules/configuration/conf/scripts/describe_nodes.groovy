import java.util.List;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.google.common.collect.Lists;

import com.eucalyptus.config.NodeComponentInfoType;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;

List<NodeComponentInfoType> nodeInfoList = Lists.newArrayList( );
for( Cluster c : Clusters.getInstance( ).listValues( ) ) {
  for( String nodeTag : c.getNodeTags( ) ) {
    NodeComponentInfoType nodeInfo = new NodeComponentInfoType( new URL(nodeTag).getHost( ), c.getName() );
    for( VmInstance vm : VmInstances.list( ) ) {
      vm = Groovyness.expandoMetaClass(vm);
      if( nodeTag.equals( vm.getServiceTag() ) ) {
        nodeInfo.getInstances().add( vm.getInstanceId() );
      }
    }
    nodeInfoList.add( nodeInfo );
  }
}
return nodeInfoList;
    
