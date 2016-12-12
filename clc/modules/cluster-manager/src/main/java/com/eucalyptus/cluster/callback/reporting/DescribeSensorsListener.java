/*************************************************************************
 * Copyright 2009-2015 Eucalyptus Systems, Inc.
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

package com.eucalyptus.cluster.callback.reporting;

import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.internal.vm.VmInstance;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Exceptions;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponse;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.callback.DescribeSensorCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.compute.common.internal.vm.VmInstance.VmState;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@ConfigurableClass( root = "cloud.monitor", description = "Parameters controlling cloud watch")
public class DescribeSensorsListener implements EventListener<Hertz> {

  @ConfigurableField(initial = "5", description = "How often to request information from the cluster controller")
  public static Long DEFAULT_POLL_INTERVAL_MINS = 5L;

  private Integer COLLECTION_INTERVAL_TIME_MS;

  @ConfigurableField(initial = "5", description = "The initial history size of metrics to be send from the cc to the clc")
  public static Integer HISTORY_SIZE = 5;

  private Integer MAX_WRITE_INTERVAL_MS = 86400000;
  private Integer SENSOR_QUERY_BATCH_SIZE = 10;

  private static final ConcurrentMap<String, Boolean> busyHosts = Maps.newConcurrentMap( );
  private static final Logger LOG = Logger.getLogger(DescribeSensorsListener.class);
  
  public static void register() {
    Listeners.register( Hertz.class, new DescribeSensorsListener() );
  }
  

  @Override
  public void fireEvent( final Hertz event ) {
    final long defaultPollIntervalSeconds = TimeUnit.MINUTES.toSeconds( DEFAULT_POLL_INTERVAL_MINS );
    if (!Bootstrap.isOperational() || !BootstrapArgs.isCloudController() || !event.isAsserted(defaultPollIntervalSeconds)) {
      return;
    } else {
      if (DEFAULT_POLL_INTERVAL_MINS >= 1) {
        COLLECTION_INTERVAL_TIME_MS = ((int) TimeUnit.MINUTES
            .toMillis(DEFAULT_POLL_INTERVAL_MINS) / 2);
      } else {
        COLLECTION_INTERVAL_TIME_MS = 0;
      }

      if (COLLECTION_INTERVAL_TIME_MS == 0 || HISTORY_SIZE > 15 || HISTORY_SIZE < 1) {
        LOG.debug("The instance usage report is disabled");
      } else if (COLLECTION_INTERVAL_TIME_MS <= MAX_WRITE_INTERVAL_MS) {

        try {
          if (event.isAsserted(defaultPollIntervalSeconds)) {
            if (Bootstrap.isFinished() && Hosts.isCoordinator()) {
              CloudWatchHelper.DefaultInstanceInfoProvider.refresh( );
              for ( final ServiceConfiguration ccConfig : Topology.enabledServices( ClusterController.class ) ) {
                final String ccHost = ccConfig.getHostName( );
                if ( busyHosts.replace( ccHost, false, true ) || busyHosts.putIfAbsent( ccHost, true ) == null ) {
                  Threads.lookup( Eucalyptus.class, DescribeSensorsListener.class ).submit( new Callable<Object>() {

                    @Override
                    public Object call() throws Exception {
                      final ExecutorService executorService = Threads.lookup( Eucalyptus.class, DescribeSensorsListener.class, "response-processing" ).limitTo( 4 );
                      final long startTime = System.currentTimeMillis( );
                      try {
                        final List<String> allInstanceIds = VmInstances.listWithProjection(
                            VmInstances.instanceIdProjection( ),
                            VmInstance.criterion( VmState.RUNNING ),
                            VmInstance.zoneCriterion( ccConfig.getPartition( ) ),
                            VmInstance.nonNullNodeCriterion( )
                        );
                        final Iterable<List<String>> processInts = Iterables.partition(  allInstanceIds, SENSOR_QUERY_BATCH_SIZE );
                        for ( final List<String> instIds : processInts ) {
                          final ArrayList<String> instanceIds = Lists.newArrayList( instIds );
                          /**
                           * Here this is hijacking the sensor callback in order to control the thread of execution used when firing
                           */
                          final DescribeSensorCallback msgCallback = new DescribeSensorCallback( HISTORY_SIZE,
                              COLLECTION_INTERVAL_TIME_MS, instanceIds ) {
                            @Override
                            public void fireException( Throwable e ) {}

                            @Override
                            public void fire( DescribeSensorsResponse msg ) {}
                          };
                          /**
                           * Here we actually get the future reference to the result and on a response processing thread, invoke .fire().
                           */
                          final DescribeSensorsResponse response = AsyncRequests.newRequest( msgCallback ).dispatch( ccConfig ).get( );
                          executorService.submit( new Runnable( ){
                            @Override
                            public void run() {
                              try {
                                new DescribeSensorCallback( HISTORY_SIZE,
                                    COLLECTION_INTERVAL_TIME_MS, instanceIds ).fire( response );
                              } catch ( Exception e ) {
                                Exceptions.maybeInterrupted( e );
                              }
                            }
                          } );
                        }
                      } finally {
                        /**
                         * Only and finally set the busy bit back to false.
                         */
                        busyHosts.put( ccHost, false );
                        LOG.debug( "Sensor polling for " + ccHost + " took " + ( System.currentTimeMillis( ) - startTime ) + "ms" );
                      }
                      return null;
                    }
                  } );
                } else {
                  LOG.warn( "Skipping sensors polling for "+ccHost+", previous poll not complete." );
                }
              }
            }

          }
        } catch (Exception ex) {
          LOG.error("Unable to listen for describe sensors events", ex);
        }

      } else {
        LOG.error("DEFAULT_POLL_INTERVAL_MINS : "
            + DEFAULT_POLL_INTERVAL_MINS
            + " must be less than 1440 minutes");
      }
    }
  }
}
