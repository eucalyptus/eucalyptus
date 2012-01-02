/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

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
