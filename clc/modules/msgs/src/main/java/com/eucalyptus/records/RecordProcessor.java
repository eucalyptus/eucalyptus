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
import com.eucalyptus.system.Threads;
import com.google.common.collect.Lists;

@Provides( Component.any )
@RunDuring( Bootstrap.Stage.DatabaseInit )
public class RecordProcessor extends Bootstrapper implements Runnable {
  private static Logger          LOG       = Logger.getLogger( RecordProcessor.class );
  private static boolean         finished  = false;
  private static RecordProcessor singleton = new RecordProcessor( );
  private static Thread          thread    = null;
  
  @Override
  public void run( ) {
    if( !finished ) {
      LOG.info( "Started records storage thread." );
      Record rec = EventRecord.here( SystemBootstrapper.class, EventClass.SYSTEM, EventType.SYSTEM_START,  "STARTED" );
      RecordLevel.INFO.enqueue( rec );
    }
    do {
      String msg = "";
      int total = 0;
      try {
        TimeUnit.MILLISECONDS.sleep( 5000 );
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
                LOG.debug( "Received system event record of a type which cannot be stored: " + record.getClass( ).getCanonicalName( ) );
              }
            }
            db.commit( );
            LOG.debug( "Saving " + savedRecords.size( ) + " records total in " + ( System.currentTimeMillis( ) - start ) + " msec. " + msg );
          } catch ( Exception e ) {
            LOG.error( e, e );
          }
        } else {
          LOG.debug( "Found nothing to save from the event record queues." );
        }
      } catch ( InterruptedException e ) {
      }
    } while ( !finished );
    LOG.info( "Terminated records storage thread." );
  }

  public static void flush() {
    if( thread != null ) {
      thread.interrupt( );
      try {
        thread.run( );
      } catch ( Throwable t ) {
        LOG.trace( "Not saving the shutdown event:  database already stopped: " + t.getMessage( ) );
      }
    }
  }
  
  @Override
  public boolean load( Stage current ) throws Exception {
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    thread = Threads.newThread( singleton, "Records Storage" );
    thread.start( );
    Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
      
      @Override
      public void run( ) {
        finished = true;
        if ( thread != null ) {
          thread.interrupt( );
        }
      }
      
    } );
    return true;
  }
  
}
