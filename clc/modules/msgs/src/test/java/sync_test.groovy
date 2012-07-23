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

import java.util.concurrent.TimeUnit
import javax.persistence.EntityTransaction
import org.apache.log4j.Logger
import com.eucalyptus.bootstrap.Databases
import com.eucalyptus.component.Faults
import com.eucalyptus.component.ServiceConfiguration
import com.eucalyptus.component.Topology
import com.eucalyptus.component.Faults.CheckException
import com.eucalyptus.component.id.Eucalyptus
import com.eucalyptus.entities.Entities


Logger LOG = Logger.getLogger( "hi" );
10.times {
  def t = new Thread().start {
    long startTime = System.currentMillis( );
    long sleepTime = 1000L * Math.random( );
    TimeUnit.MILLISECONDS.sleep( stime );
    ServiceConfiguration conf = Topology.lookup( Eucalyptus.class );
    String msg = "${new Date( startTime )} Slept for ${sleepTime}: hosts=${Databases.ActiveHostSet.HOSTS.get()} dbs=${Databases.ActiveHostSet.ACTIVATED.get( )} volatile=${Databases.isVolatile( )}";
    CheckException fault = Faults.advisory( conf, new Exception( msg ) );
    final EntityTransaction db = Entities.get( CheckException.class );
    try {
      Entities.persist( fault );
      db.commit( );
    } catch ( final Exception ex ) {
      LOG.error( "Failed to persist error information for: " + fault, ex );
      db.rollback( );
    }
  }
}
