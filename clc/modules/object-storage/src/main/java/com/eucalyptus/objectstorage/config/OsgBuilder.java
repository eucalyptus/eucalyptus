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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.config;

import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;

import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.objectstorage.msgs.DeregisterObjectStorageGatewayType;
import com.eucalyptus.objectstorage.msgs.DescribeObjectStorageGatewaysType;
import com.eucalyptus.objectstorage.msgs.ModifyObjectStorageAttributeType;
import com.eucalyptus.objectstorage.msgs.RegisterObjectStorageGatewayType;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.google.common.base.Joiner;

@ComponentPart(ObjectStorage.class)
@Handles( { RegisterObjectStorageGatewayType.class, DeregisterObjectStorageGatewayType.class, DescribeObjectStorageGatewaysType.class, ModifyObjectStorageAttributeType.class } )
public class OsgBuilder extends AbstractServiceBuilder<ObjectStorageConfiguration> {
	private static final Logger LOG = Logger.getLogger(OsgBuilder.class);
	
	@Override
	public ObjectStorageConfiguration newInstance( ) {
		return new ObjectStorageConfiguration( );
	}
	
	@Override
	public ObjectStorageConfiguration newInstance( String partition, String name, String host, Integer port ) {
		return new ObjectStorageConfiguration( name, host, port );
	}
	
	@Override
	public ComponentId getComponentId( ) {
		return ComponentIds.lookup( ObjectStorage.class );
	}
	
	@Override
	public void fireLoad( ServiceConfiguration parent ) throws ServiceRegistrationException {
		try {
			if ( parent.isVmLocal( ) ) {
				EntityTransaction tx = Entities.get( parent ); 
				try {
					parent = Entities.merge( parent );
					//Load the available backends from this SC into the DB entry
					((ObjectStorageConfiguration)parent).setAvailableClients(Joiner.on(",").join(ObjectStorageProviders.list()));        	
					tx.commit( );
				} catch ( Exception ex ) {
					LOG.debug("Error merging parent transaction. Rolling back.");
					tx.rollback( );
				}       
				
				ObjectStorageProviders.getInstance( );
			}
		} catch ( Exception ex ) {
			throw Exceptions.toUndeclared( ex );
		}	
	}
	
	@Override
	public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {}
	
	@Override
	public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
		try {
			ObjectStorageProviders.flushClientInstances();
		} catch(EucalyptusCloudException e) {
			LOG.error("Error flushing client instances. Non-fatal", e);
		}
	}
	
	@Override
	public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {}
	
	@Override
	public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}
	
	@Override
	public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}
	
	
}
