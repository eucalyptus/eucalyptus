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

package com.eucalyptus.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.log4j.Logger;
import com.eucalyptus.component.Components;
import com.eucalyptus.component.Faults;
import com.eucalyptus.component.ServiceConfigurations;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Walrus;
import com.eucalyptus.empyrean.Empyrean.Arbitrator;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.Internets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ArbitratorControl {
	private static Logger                   LOG               = Logger.getLogger( ArbitratorControl.class );
	private static Map<ArbitratorConfiguration, Exception>   error             = Maps.newConcurrentMap( );
	private static Map<ArbitratorConfiguration, ArbitratorConfiguration>   okay              = Maps.newConcurrentMap( );
	private static volatile boolean wasSet;
	private static ScheduledExecutorService monitor;
	private final static int                CHECK_PERIODICITY = 5;

	public ArbitratorControl( ) {}

	public static void start( ) {}

	public static void check( ) throws Exception {
		Threads.lookup( Arbitrator.class, ArbitratorControl.class ).submit( new Runnable( ) {
			@Override
			public void run( ) {
				final List<ArbitratorConfiguration> configs = ServiceConfigurations.list( Arbitrator.class );
				for ( final ArbitratorConfiguration config : configs ) {
					if(Internets.testLocal(config.getHostName())) {
						final String hostName = config.getGatewayHost();
						if ( hostName != null ) {
							try {
								final InetAddress addr = InetAddress.getByName( hostName );
								if ( Internets.isReachable( addr, 2000 ) ) {
									ArbitratorControl.error.remove( config );
									ArbitratorControl.okay.put( config, config );
								} else {
									ArbitratorControl.error.put( config, Exceptions.filterStackTrace( new NoRouteToHostException( addr.toString( ) ) ) );
									ArbitratorControl.okay.remove(config);
								}
							} catch ( final UnknownHostException e ) {
								ArbitratorControl.error.put( config, Exceptions.filterStackTrace( e ) );
								ArbitratorControl.okay.remove(config);
							} catch ( final IOException e ) {
								ArbitratorControl.error.put( config, Exceptions.filterStackTrace( e ) );
								ArbitratorControl.okay.remove(config);
							}
//							EventRecord.here( ArbitratorControl.class, EventType.BOOTSTRAPPER_CHECK, hostName, "errorMap", error.get( hostName ).toString( ) ).debug( );
						}
					}
				}
				wasSet = true;
			}
		}
				);
		try {
			if (wasSet) {
				final Set<ArbitratorConfiguration> downArbitrators = Sets.newHashSet( error.keySet( ) );
				if ( downArbitrators.size( ) > 0 ) {
					ArbitratorConfiguration anyConfig = null;
					List<Exception> exceptions = new ArrayList<Exception>();
					for (ArbitratorConfiguration key : downArbitrators) {
						anyConfig = key;
						exceptions.add(error.get(key));
					}
					if (ArbitratorControl.okay.isEmpty()) {
						throw Faults.fatal(anyConfig, exceptions);
					} else {
						throw Faults.advisory(anyConfig, exceptions);
					}
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (wasSet) {
				ArbitratorControl.okay.clear();
				ArbitratorControl.error.clear();
				wasSet = false;
			}
		}
	}
}
