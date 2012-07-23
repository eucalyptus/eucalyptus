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

package com.eucalyptus.records;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.apache.log4j.Logger;
import com.google.common.collect.Lists;

public enum RecordLevel {
  TRACE, DEBUG, INFO, WARN, FATAL, ERROR;
  private static Logger             LOG     = Logger.getLogger( RecordLevel.class );
  private BlockingQueue<Record> pending = new LinkedBlockingDeque<Record>( );
  
  public void enqueue( Record rec ) {
    if ( RecordFilter.getInstance( ).accepts( rec ) ) {
      try {
        this.pending.put( rec );
      } catch ( InterruptedException e ) {
        LOG.debug( e, e );
      }
    }
  }
  
  public List<Record> drain( ) {
    List<Record> newEvents = Lists.newArrayList( );
    this.pending.drainTo( newEvents );
    return newEvents;
  }
}
