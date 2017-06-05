/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.cluster.service.fake;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import com.eucalyptus.cluster.service.vm.ClusterVm;
import com.google.common.collect.Maps;
import javaslang.collection.Stream;
import javaslang.control.Option;

/**
 *
 */
public class FakeNodeState {
  private final ConcurrentMap<String,FakeNodeVmInfo> vms = Maps.newConcurrentMap( );

  public void extant( final long now, final ClusterVm vm ) {
    vms.computeIfAbsent( vm.getId( ), __ -> {
      final FakeNodeVmInfo fakeNodeVmInfo = new FakeNodeVmInfo( vm.getId( ) );
      fakeNodeVmInfo.state( now, FakeNodeVmInfo.State.Extant );
      fakeNodeVmInfo.setPublicIp( vm.getPrimaryInterface( ).getPublicAddress( ) );
      fakeNodeVmInfo.getVolumeAttachments( ).putAll( vm.getVolumeAttachments( ) );
      return fakeNodeVmInfo;
    } );
  }

  public void terminate( final long now, final String instanceId ) {
    vm( instanceId ).forEach( vm -> vm.state( now, FakeNodeVmInfo.State.Teardown ) );
  }

  public void assignAddress( final String instanceId, final String publicIp ) {
    vm( instanceId ).forEach( vm -> vm.setPublicIp( publicIp ) );
  }

  public Option<FakeNodeVmInfo> vm( final String id ) {
    return Option.of( vms.get( id ) );
  }

  public void cleanup( final long now ) {
    final long expiryAge = now - TimeUnit.MINUTES.toMillis( 2 );
    final Predicate<FakeNodeVmInfo> isExpired =
        vm -> vm.getState( )== FakeNodeVmInfo.State.Teardown && vm.getStateTimestamp( ) < expiryAge;
    final Set<String> expired = Stream.ofAll( vms.values( ) ).filter( isExpired ).map( FakeNodeVmInfo::getId ).toJavaSet( );
    vms.keySet( ).removeAll( expired );
  }
}
