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
package com.eucalyptus.cluster.service.fake;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import com.eucalyptus.cluster.service.vm.VmInfo;
import com.google.common.collect.Maps;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 *
 */
public class FakeNodeState {
  private final ConcurrentMap<String,FakeNodeVmInfo> vms = Maps.newConcurrentMap( );

  public void extant( final VmInfo vm ) {
    vms.computeIfAbsent( vm.getId( ), __ -> {
      final FakeNodeVmInfo fakeNodeVmInfo = new FakeNodeVmInfo( vm.getId( ) );
      fakeNodeVmInfo.state( System.currentTimeMillis( ), FakeNodeVmInfo.State.Extant );
      fakeNodeVmInfo.setPublicIp( vm.getPrimaryInterface( ).getPublicAddress( ) );
      fakeNodeVmInfo.getVolumeAttachments( ).putAll( vm.getVolumeAttachments( ) );
      return fakeNodeVmInfo;
    } );
  }

  public void terminate( final String instanceId ) {
    vm( instanceId ).forEach( vm -> vm.state( System.currentTimeMillis( ), FakeNodeVmInfo.State.Teardown ) );
  }

  public void assignAddress( final String instanceId, final String publicIp ) {
    vm( instanceId ).forEach( vm -> vm.setPublicIp( publicIp ) );
  }

  public Option<FakeNodeVmInfo> vm( final String id ) {
    return Option.of( vms.get( id ) );
  }

  public void cleanup( ) {
    final long expiryAge = System.currentTimeMillis( ) - TimeUnit.MINUTES.toMillis( 2 );
    final Predicate<FakeNodeVmInfo> isExpired =
        vm -> vm.getState( )== FakeNodeVmInfo.State.Teardown && vm.getStateTimestamp( ) < expiryAge;
    final Set<String> expired = Stream.ofAll( vms.values( ) ).filter( isExpired ).map( FakeNodeVmInfo::getId ).toJavaSet( );
    vms.keySet( ).removeAll( expired );
  }
}
