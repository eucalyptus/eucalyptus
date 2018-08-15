/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cluster.proxy;

import java.io.IOException;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.cluster.common.msgs.*;
import com.eucalyptus.cluster.service.ClusterFullService;
import com.eucalyptus.cluster.service.ClusterServiceImpl;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.empyrean.EmpyreanMessage;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.async.AsyncProxy;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;
import io.vavr.control.Option;

/**
 * Proxy (native) implementation for the cluster controller service.
 */
@ComponentNamed("proxyClusterService")
public class ProxyClusterServiceImpl implements ClusterFullService {

  private static final Logger logger = Logger.getLogger( ClusterServiceImpl.class );

  private ClusterFullService proxy( ) {
    final Option<ServiceConfiguration> proxyConfOption =
        ProxyClusterManager.local( ).map( ProxyClusterManager::getConfiguration );
    if ( !proxyConfOption.isDefined() ||
        !ProxyClusterManager.local( ).map( ProxyClusterManager::isEnabled ).getOrElse( false ) ) {
      throw new IllegalStateException( "Not enabled : " + ProxyClusterManager.local( )
          .map( ProxyClusterManager::getState ).getOrElse( ProxyClusterManager.State.BROKEN ) );
    }
    return AsyncProxy.client( ClusterFullService.class, message -> {
      try {
        final BaseMessage copy = BaseMessages.deepCopy( message );
        if (!(copy instanceof ClusterServiceMessage)) {
          copy.setUserId( Principals.systemUser( ).getUserId( ) );
        }
        return copy;
      } catch ( IOException e ) {
        throw Exceptions.toUndeclared( e );
      }
    }, proxyConfOption.get( ) );
  }

  private <M extends CloudClusterMessage> M replyFor( Callable<M> replyCallable, CloudClusterMessage request ) {
    M reply;
    try {
      reply = replyCallable.call( );
    } catch ( Exception e ) {
      logger.error( e, e );
      reply = request.getReply( ).markFailed( );
    }
    reply.setCorrelationId( request.getCorrelationId( ) );
    return reply;
  }

  private <M extends EmpyreanMessage & ClusterServiceMessage, R extends EmpyreanMessage & ClusterServiceMessage> M replyForSrv( Callable<M> replyCallable, R request ) {
    M reply;
    try {
      reply = replyCallable.call( );
    } catch ( Exception e ) {
      logger.error( e, e );
      reply = request.getReply( ).markFailed( );
    }
    reply.setCorrelationId( request.getCorrelationId( ) );
    return reply;
  }

  @Override
  public BroadcastNetworkInfoResponseType broadcastNetworkInfo( final BroadcastNetworkInfoType request ) {
    return replyFor( () -> proxy( ).broadcastNetworkInfo( request ), request ).markWinning( ); // always a winner
  }

  @Override
  public ClusterAttachVolumeResponseType attachVolume( final ClusterAttachVolumeType request ) {
    return replyFor( () -> proxy( ).attachVolume( request ), request );
  }

  @Override
  public ClusterDetachVolumeResponseType detachVolume( final ClusterDetachVolumeType request ) {
    return replyFor( () -> proxy( ).detachVolume( request ), request );
  }

  @Override
  public ClusterGetConsoleOutputResponseType getConsoleOutput( final ClusterGetConsoleOutputType request ) {
    return replyFor( () -> proxy( ).getConsoleOutput( request ), request );
  }

  @Override
  public ClusterMigrateInstancesResponseType migrateInstances( final ClusterMigrateInstancesType request ) {
    return replyFor( () -> proxy( ).migrateInstances( request ), request );
  }

  @Override
  public ClusterBundleInstanceResponseType bundleInstance( final ClusterBundleInstanceType request ) {
    return replyFor( () -> proxy( ).bundleInstance( request ), request );
  }

  @Override
  public ClusterBundleRestartInstanceResponseType bundleRestartInstance( final ClusterBundleRestartInstanceType request ) {
    return replyFor( () -> proxy( ).bundleRestartInstance( request ), request );
  }

  @Override
  public ClusterCancelBundleTaskResponseType cancelBundleTask( final ClusterCancelBundleTaskType request ) {
    return replyFor( () -> proxy( ).cancelBundleTask( request ), request );
  }

  @Override
  public ClusterRebootInstancesResponseType rebootInstances( final ClusterRebootInstancesType request ) {
    return replyFor( () -> proxy( ).rebootInstances( request ), request );
  }

  @Override
  public ClusterStartInstanceResponseType startInstance( final ClusterStartInstanceType request ) {
    return replyFor( () -> proxy( ).startInstance( request ), request );
  }

  @Override
  public ClusterStopInstanceResponseType stopInstance( final ClusterStopInstanceType request ) {
    return replyFor( () -> proxy( ).stopInstance( request ), request );
  }

  @Override
  public ClusterTerminateInstancesResponseType terminateInstances( final ClusterTerminateInstancesType request ) {
    return replyFor( () -> proxy( ).terminateInstances( request ), request );
  }

  @Override
  public DescribeResourcesResponseType describeResources( final DescribeResourcesType request ) {
    return replyFor( () -> proxy( ).describeResources( request ), request );
  }

  @Override
  public DescribeSensorsResponseType describeSensors( final DescribeSensorsType request ) {
    return replyFor( () -> proxy( ).describeSensors( request ), request );
  }

  @Override
  public VmDescribeResponseType describeVms( final VmDescribeType request ) {
    return replyFor( () -> proxy( ).describeVms( request ), request );
  }

  @Override
  public VmRunResponseType runVm( final VmRunType request ) {
    return replyFor( () -> proxy( ).runVm( request ), request );
  }

  @Override
  public ModifyNodeResponseType modifyNode( final ModifyNodeType request ) {
    return replyFor( () -> proxy( ).modifyNode( request ), request );
  }

  @Override
  public ClusterDescribeServicesResponseType describeServices( final ClusterDescribeServicesType request ) {
    return replyForSrv( () -> proxy( ).describeServices( request ), request );
  }

  @Override
  public ClusterDisableServiceResponseType disableService( final ClusterDisableServiceType request ) {
    return replyForSrv( () -> proxy( ).disableService( request ), request );
  }

  @Override
  public ClusterEnableServiceResponseType enableService( final ClusterEnableServiceType request ) {
    return replyForSrv( () -> proxy( ).enableService( request ), request );
  }

  @Override
  public ClusterStartServiceResponseType startService( final ClusterStartServiceType request ) {
    return replyForSrv( () -> proxy( ).startService( request ), request );
  }

  @Override
  public ClusterStopServiceResponseType stopService( final ClusterStopServiceType request ) {
    return replyForSrv( () -> proxy( ).stopService( request ), request );
  }
}
