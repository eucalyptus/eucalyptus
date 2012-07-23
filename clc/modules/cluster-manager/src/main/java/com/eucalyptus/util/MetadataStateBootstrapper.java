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

package com.eucalyptus.util;

import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.vm.VmType;

@Provides( Eucalyptus.class )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
public class MetadataStateBootstrapper extends Bootstrapper.Simple {
  private static Logger LOG = Logger.getLogger( MetadataStateBootstrapper.class );
  
  @Override
  public boolean start( ) throws Exception {
    try {
      if ( Hosts.isCoordinator( ) ) {
        ensureCountersExist( );
        ensureVmTypesExist( );
      }
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
    }
    return true;
  }
  
  private static void ensureCountersExist( ) {
    UniqueIds.nextId( );
    UniqueIds.nextId( Eucalyptus.class );
  }
  
  private static void ensureVmTypesExist( ) {
    EntityWrapper<VmType> db = EntityWrapper.get( VmType.class );
    try {
      if ( db.query( new VmType( ) ).size( ) == 0 ) { //TODO: make defaults configurable?
        db.add( new VmType( "m1.small", 1, 2, 128 ) );
        db.add( new VmType( "c1.medium", 1, 5, 256 ) );
        db.add( new VmType( "m1.large", 2, 10, 512 ) );
        db.add( new VmType( "m1.xlarge", 2, 20, 1024 ) );
        db.add( new VmType( "c1.xlarge", 4, 20, 2048 ) );
      }
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
    }
  }
  
}
