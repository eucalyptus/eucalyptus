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
