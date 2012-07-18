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

package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;
import com.eucalyptus.cloud.ws.DNSControl;
import com.eucalyptus.component.id.Dns;

@Provides( Dns.class )
@RunDuring( Bootstrap.Stage.CloudServiceInit )
@DependsLocal( Dns.class )
public class DNSBootstrapper extends Bootstrapper.Simple {
	private static Logger          LOG = Logger.getLogger( DNSBootstrapper.class );
	private static DNSBootstrapper singleton;

	public static Bootstrapper getInstance( ) {
		synchronized ( DNSBootstrapper.class ) {
			if ( singleton == null ) {
				singleton = new DNSBootstrapper( );
				LOG.info( "Creating DNS Bootstrapper instance." );
			} else {
				LOG.info( "Returning DNS Bootstrapper instance." );
			}
		}
		return singleton;
	}

	@Override
	public boolean load( ) throws Exception {
		return true;
	}

	@Override
	public boolean start( ) throws Exception {
		LOG.info( "Initializing DNS" );
		//The following call binds DNS ports.
		DNSControl.initialize( );
		return true;
	}

	@Override
	public boolean disable( ) throws Exception {
		return true;
	}

	@Override
	public boolean stop( ) throws Exception {
		LOG.info("Stopping DNS");
		DNSControl.stop();
		return true;
	}
}
