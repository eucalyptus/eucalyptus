/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

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
