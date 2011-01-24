package com.eucalyptus.records;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.SystemBootstrapper;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.system.Threads;
import com.google.common.collect.Lists;

@Provides( Component.any )
@RunDuring( Bootstrap.Stage.DatabaseInit )
public class RecordProcessor extends Bootstrapper implements Runnable, EventListener {
  private static final int RECORD_QUEUE_FLUSH_INTERVAL = 10000;
  private static Logger          LOG       = Logger.getLogger( RecordProcessor.class );
  private static boolean         finished  = false;
  private static RecordProcessor singleton = new RecordProcessor( );
  private static Thread          thread    = null;
  
  @Override
  public void run( ) {
    try {
      do {
        String msg = "";
        int total = 0;
        TimeUnit.MILLISECONDS.sleep( RECORD_QUEUE_FLUSH_INTERVAL );
        List<Record> savedRecords = Lists.newArrayList( );
        for ( RecordLevel level : RecordLevel.values( ) ) {
          List<Record> records = level.drain( );
          savedRecords.addAll( records );
          msg += level.name( ) + ":" + records.size( ) + " ";
        }
        if( !savedRecords.isEmpty( ) ) {
          long start = System.currentTimeMillis( );
          EntityWrapper<BaseRecord> db = EntityWrapper.get( BaseRecord.class );
          try {
            for ( Record record : savedRecords ) {
              if( record instanceof BaseRecord ) {
                db.add( ( BaseRecord ) record );
              } else {
                LOG.error( "Received system event record of a type which cannot be stored: " + record.getClass( ).getCanonicalName( ) );
              }
            }
            db.commit( );
            LOG.trace( "Saving " + savedRecords.size( ) + " records total in " + ( System.currentTimeMillis( ) - start ) + " msec. " + msg );
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        } else {
          LOG.trace( "Found nothing to save from the event record queues." );
        }
      } while ( !finished );
    } catch ( InterruptedException e ) {
      finished = true;
      Thread.currentThread( ).interrupted( );
    }
    LOG.info( "Terminated records storage thread." );
  }

  public static void flush() {
    Thread ref = thread;
    finished = true;
    if( ref != null ) {      
      ref.interrupt( );
      try {
        ref.run( );
      } catch ( Throwable t ) {
        LOG.trace( "Not saving the shutdown event:  database already stopped: " + t.getMessage( ) );
      }
    }
  }
  
  @Override
  public boolean load( ) throws Exception {
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    LOG.info( "Started records storage thread." );
    Record rec = EventRecord.here( SystemBootstrapper.class, EventClass.SYSTEM, EventType.SYSTEM_START,  "STARTED" );
    RecordLevel.INFO.enqueue( rec );
    thread = Threads.newThread( singleton, "Records Storage" );
    thread.start( );
    final Thread ref = thread;
    Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
      @Override
      public void run( ) {
        finished = true;
        if ( ref != null && ref.isAlive( ) ) {
          ref.interrupt( );
        }
      }
      
    } );
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {}

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    return true;
  }

  @Override
  public void fireEvent( Event event ) {
    
  }
  
}
