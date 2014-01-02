/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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

package com.eucalyptus.blockstorage.config;

import javax.persistence.EntityTransaction;
import org.apache.log4j.Logger;

import com.eucalyptus.blockstorage.Storage;
import com.eucalyptus.blockstorage.StorageManagers;
import com.eucalyptus.blockstorage.msgs.DeregisterStorageControllerType;
import com.eucalyptus.blockstorage.msgs.DescribeStorageControllersType;
import com.eucalyptus.blockstorage.msgs.ModifyStorageControllerAttributeType;
import com.eucalyptus.blockstorage.msgs.RegisterStorageControllerType;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.component.AbstractServiceBuilder;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceRegistrationException;
import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.LogUtil;
import com.google.common.base.Joiner;

/**
 * Storage controller builder is responsible for handling the StorageManagers cache and ensuring that the proper values are
 * populated and removed on the registration-lifecycle of the SC.
 * @author zhill
 *
 */
@ComponentPart( Storage.class )
@Handles( { RegisterStorageControllerType.class, DeregisterStorageControllerType.class, DescribeStorageControllersType.class, ModifyStorageControllerAttributeType.class } )
public class StorageControllerBuilder extends AbstractServiceBuilder<StorageControllerConfiguration> {
  
	/**
	 * Configure the block storage backend based on the blockstoragemanager found in the DB.
	 */
	@Override
  public void fireLoad( ServiceConfiguration parent ) throws ServiceRegistrationException {  	
    try {
      if ( parent.isVmLocal( ) ) {
      	LOG.info("Firing LOAD for local config: " + parent.getName());
        EntityTransaction tx = Entities.get( parent ); 
        try {
        	parent = Entities.merge( parent );
        	//Load the available backends from this SC into the DB entry
        	((StorageControllerConfiguration)parent).setAvailableBackends(Joiner.on(",").join(StorageManagers.list()));        	
        	tx.commit( );
        } catch ( Exception ex ) {
        	LOG.debug("Error merging parent transaction. Rolling back.");
          tx.rollback( );
        }       
       
        String propertyBackend = ( ( StorageControllerConfiguration ) parent ).getBlockStorageManager( );
        StorageManagers.getInstance( propertyBackend );
      }
    } catch ( Exception ex ) {
      throw Exceptions.toUndeclared( ex );
    }
  }

  private static Logger LOG = Logger.getLogger( StorageControllerBuilder.class );

  @Override
  public ComponentId getComponentId( ) {
    return ComponentIds.lookup( Storage.class );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( ) {
    return new StorageControllerConfiguration( );
  }
  
  @Override
  public StorageControllerConfiguration newInstance( String partition, String name, String host, Integer port ) {  	
  		return new StorageControllerConfiguration( partition, name, host, port );	
  }
  
  @Override
  public boolean checkAdd( String partition, String name, String host, Integer port ) throws ServiceRegistrationException {
    try {
      final Partition part = Partitions.lookup( this.newInstance( partition, name, host, port ) );
      part.syncKeysToDisk( );
    } catch ( final Exception ex ) {
      Logs.extreme( ).error( ex, ex );
      throw new ServiceRegistrationException( String.format( "Unexpected error caused storage controller registration to fail for: partition=%s name=%s host=%s port=%d",
                                                             partition, name, host, port ), ex );
    }
    return super.checkAdd( partition, name, host, port );
  }

  /**
   * Flush the block storage manager from the cache such that the next startup cycle (or registration) will catch the new one.
   */
  @Override
  public void fireStop( ServiceConfiguration config ) throws ServiceRegistrationException {
  	try {
      if ( config.isVmLocal( ) ) {
      	LOG.info("Firing STOP for local config: " + config.getName());  
        StorageManagers.flushManagerInstances();
      }
    } catch ( Exception ex ) {
      throw Exceptions.toUndeclared( ex );
    }
  }
  
  @Override
  public void fireStart( ServiceConfiguration config ) throws ServiceRegistrationException {
    if ( config.isVmLocal( ) ) {
    	LOG.info("Firing START for local config: " + config.getName());      
      java.lang.System.setProperty( "euca.storage.name", config.getName( ) );
      LOG.info( LogUtil.subheader( "Setting euca.storage.name=" + config.getName( ) + " for: " + LogUtil.dumpObject( config ) ) );
    }
  }

  @Override
  public void fireEnable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireDisable( ServiceConfiguration config ) throws ServiceRegistrationException {}

  @Override
  public void fireCheck( ServiceConfiguration config ) throws ServiceRegistrationException {}
  
}
