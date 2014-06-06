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

package com.eucalyptus.reporting.modules.backend;

import com.eucalyptus.component.id.Reporting;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.Exceptions;
import com.google.common.util.concurrent.SettableFuture;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsResponse;
import edu.ucsb.eucalyptus.msgs.DescribeSensorsType;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.BootstrapArgs;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cluster.callback.DescribeSensorCallback;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.vm.VmInstance;
import com.eucalyptus.vm.VmInstances;
import com.eucalyptus.vm.VmInstance.VmState;

import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.ClusterController;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@ConfigurableClass( root = "cloud.monitor", description = "Parameters controlling cloud watch and reporting")
public class DescribeSensorsListener implements EventListener<Hertz> {

  @ConfigurableField(initial = "5", description = "How often the reporting system requests information from the cluster controller")
  public static Long DEFAULT_POLL_INTERVAL_MINS = 5L;
  public static final int REPORTING_NUM_THREADS = 4;

  private Integer COLLECTION_INTERVAL_TIME_MS;

  @ConfigurableField(initial = "5", description = "The initial history size of metrics to be send from the cc to the clc")
  public static Integer HISTORY_SIZE = 5;

  private Integer MAX_WRITE_INTERVAL_MS = 86400000;
  private Integer SENSOR_QUERY_BATCH_SIZE = 10;

  private static final AtomicBoolean busy = new AtomicBoolean( false );
  private static final Logger LOG = Logger.getLogger(DescribeSensorsListener.class);
  
  public static void register() {
    Listeners.register( Hertz.class, new DescribeSensorsListener() );
  }
  

  @Override
  public void fireEvent( final Hertz event ) {
    if (!Bootstrap.isOperational() || !BootstrapArgs.isCloudController() || !event.isAsserted(DEFAULT_POLL_INTERVAL_MINS)) {
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

          if (event.isAsserted(TimeUnit.MINUTES
              .toSeconds(DEFAULT_POLL_INTERVAL_MINS))) {
            if (Bootstrap.isFinished() && Hosts.isCoordinator()) {
              if ( busy.compareAndSet( false, true ) ) {
                Threads.lookup( Reporting.class ).limitTo( REPORTING_NUM_THREADS ).submit( new Callable<Object>() {

                  @Override
                  public Object call() throws Exception {
                    try {

                      List<VmInstance> instList = VmInstances.list( VmState.RUNNING );

                      List<String> instIdList = Lists.newArrayList();

                      for ( final VmInstance inst : instList ) {
                        instIdList.add( inst.getInstanceId() );
                      }
                      Iterable<List<String>> processInts = Iterables.paddedPartition( instIdList, SENSOR_QUERY_BATCH_SIZE );


                      for ( final ServiceConfiguration ccConfig : Topology.enabledServices( ClusterController.class ) ) {
                        for ( List<String> instIds : processInts ) {

                          ArrayList<String> instanceIds = Lists.newArrayList( instIds );
                          Iterables.removeIf( instanceIds, Predicates.isNull() );
                          //                  LOG.info("DecribeSensorCallback about to be sent");
                          /**
                           * Here this is hijacking the sensor callback in order to control the thread of execution used when invoking the
                           */
                          final DescribeSensorCallback msgCallback = new DescribeSensorCallback( HISTORY_SIZE,
                                                                                                 COLLECTION_INTERVAL_TIME_MS, instanceIds ) {
                            @Override
                            public void fireException( Throwable e ) {}

                            @Override
                            public void fire( DescribeSensorsResponse msg ) {}
                          };
                          /**
                           * Here we actually get the future reference to the result and, from this thread, invoke .fire().
                           */
                          Future<DescribeSensorsResponse> ret = AsyncRequests.newRequest( msgCallback ).dispatch( ccConfig );
                          try {
                            new DescribeSensorCallback( HISTORY_SIZE,
                                                        COLLECTION_INTERVAL_TIME_MS, instanceIds ).fire(ret.get());
        //                  LOG.info("DecribeSensorCallback has been successfully executed");
                          } catch ( Exception e ) {
                            Exceptions.maybeInterrupted( e );
                          }
                        }
                      }
                    } finally {
                      /**
                       * Only and finally set the busy bit back to false.
                       */
                      busy.set( false );
                    }
                    return null;
                  }
                } );
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
