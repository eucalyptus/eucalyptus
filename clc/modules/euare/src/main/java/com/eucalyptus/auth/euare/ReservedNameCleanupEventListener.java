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
package com.eucalyptus.auth.euare;

import java.util.Date;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.euare.persist.entities.ReservedNameEntity;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.google.common.collect.Maps;

/**
 *
 */
public class ReservedNameCleanupEventListener implements EventListener<ClockTick> {

  private static final Logger logger = Logger.getLogger( ReservedNameCleanupEventListener.class );

  public static void register( ) {
    Listeners.register( ClockTick.class, new ReservedNameCleanupEventListener( ) );
  }

  @Override
  public void fireEvent( final ClockTick event ) {
    if ( Topology.isEnabledLocally( Euare.class ) ) {
      try ( final TransactionResource tx = Entities.transactionFor( ReservedNameEntity.class ) ) {
        final Map<String, Date> parameters = Maps.newHashMap( );
        parameters.put( "expiry", new Date( ) );
        Entities.deleteAllMatching( ReservedNameEntity.class, "WHERE expiry < :expiry", parameters );
        tx.commit();
      } catch ( final Exception e ) {
        logger.error( "Error deleting expired name reservations", e );
      }
    }
  }
}