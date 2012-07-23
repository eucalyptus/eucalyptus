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

package com.eucalyptus.entities;

import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;

public class PersistenceContextDiscovery extends ServiceJarDiscovery {
  private static Logger LOG = Logger.getLogger( PersistenceContextDiscovery.class );
  
  public PersistenceContextDiscovery( ) {}
  
  @Override
  public Double getPriority( ) {
    return 0.91d;
  }
  
  @Override
  public boolean processClass( Class candidate ) throws Exception {
    if ( PersistenceContexts.isEntityClass( candidate ) ) {
      if ( !Ats.from( candidate ).has( PersistenceContext.class ) ) {
        throw BootstrapException.throwFatal( "Database entity does not have required @PersistenceContext annotation: " + candidate.getCanonicalName( ) );
      } else {
        PersistenceContexts.addEntity( candidate );
        return true;
      }
    } else if ( PersistenceContexts.isSharedEntityClass( candidate ) ) {
      PersistenceContexts.addSharedEntity( candidate );
      return true;
    } else {
      return false;
    }
  }
    
}
