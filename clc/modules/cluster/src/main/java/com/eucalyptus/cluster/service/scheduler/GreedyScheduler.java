/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.cluster.service.scheduler;

import com.eucalyptus.cluster.common.msgs.VmTypeInfo;
import com.eucalyptus.cluster.service.node.ClusterNode;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 *
 */
public class GreedyScheduler implements Scheduler {

  @Override
  public String name( ) {
    return "GREEDY";
  }

  @Override
  public Option<ClusterNode> schedule(
      final Stream<ClusterNode> nodes,
      final VmTypeInfo vmTypeInfo
  ) {
    return nodes.find( Scheduler.reserve( vmTypeInfo ) );
  }
}
