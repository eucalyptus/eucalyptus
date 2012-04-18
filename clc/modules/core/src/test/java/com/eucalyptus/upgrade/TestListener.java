package com.eucalyptus.upgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestListener extends RunListener {
  private static Logger                         LOG       = Logger.getLogger( TestListener.class );
  private volatile boolean                      failed    = false;
  private final static Map<String, Description> passedMap = new TreeMap<String, Description>( );
  private final static Map<String, Long>        timerMap  = new HashMap<String, Long>( );
  private final static Map<String, Failure>     failedMap = new TreeMap<String, Failure>( );
  
  public static String key( Description desc ) {
    return desc.getClassName( ) + "." + ( desc.getMethodName( ) != null ? desc.getMethodName( ) : "class" );
  }
  @Override public void testFailure( Failure failure ) {
    try {
      Description desc = failure.getDescription( );
      String key = key( desc );
      LOG.info( "FAILED " + key );
      failedMap.put( key, failure );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  @Override public void testFinished( Description description ) throws Exception {
    try {
      timerMap.put( key( description ), System.currentTimeMillis( ) - timerMap.get( key( description ) ) );
      if( !failedMap.containsKey( key(description) )) {
        LOG.info( "PASSED " + key( description ) );        
      }
      passedMap.put( key( description ), description );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  @Override public void testStarted( Description description ) throws Exception {
    try {
      failed = false;
      timerMap.put( key( description ), System.currentTimeMillis( ) );
      LOG.info( "TEST_RUN" );
      LOG.info( "START  " + key( description ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  public static TestDescription getTestDescription( Description description ) throws ClassNotFoundException, NoSuchMethodException {
    TestDescription d;
    try {
      Class testClass = Class.forName( description.getClassName( ) );
      d = ( TestDescription ) testClass.getDeclaredMethod( description.getMethodName( ), new Class[] {} ).getAnnotation( TestDescription.class );
      return d;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }
  public static Test getTest( Description description ) throws ClassNotFoundException, NoSuchMethodException {
    Test d;
    try {
      Class testClass = Class.forName( description.getClassName( ) );
      d = ( Test ) testClass.getDeclaredMethod( description.getMethodName( ), new Class[] {} ).getAnnotation( Test.class );
      return d;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }
  @Override public void testRunFinished( Result result ) throws Exception {
    if ( !failedMap.isEmpty( ) ) {
      try {
        LOG.info( "FAILURES" );
        for ( String failed : failedMap.keySet( ) ) {
          errorLog( failedMap.get( failed ),  failed );
        }
      } catch ( Exception e ) {
        e.printStackTrace( );
      }
    }
    LOG.info( "SUMMARY" );
    try {
      for ( Description description : passedMap.values( ) ) {
        if ( !failedMap.containsKey( key( description ) ) ) {
          LOG.info( description ) ;
        }
      }
    } catch ( Exception e ) {
      e.printStackTrace( );
    }
    if ( !failedMap.isEmpty( ) ) {
      for ( String failed : failedMap.keySet( ) ) {
        try {
          if( passedMap.containsKey( failed ) && timerMap.containsKey( failed ) ) {
            LOG.info( failed ) ;            
          }
        } catch ( Exception e ) {
          e.printStackTrace( );
        }
      }
    }
  }
  public static void errorLog( Failure f, String key ) {
	   try {
	     LOG.error( f.getTrace() );
	     LOG.error( "+=============================================================================+" );
	     LOG.error( String.format( "| Test:      %-60.60s", key ) );
	     int i = 0;
	     for( Throwable t = f.getException( ); t != null && ++i < 10; t = t.getCause( ) ) {
	    	 if (t.getMessage() != null)
	    		 LOG.error( String.format( "| Cause:     %s", t.getMessage( ).replaceAll("\n","") ) );        
	     }
	     LOG.error( "+-----------------------------------------------------------------------------+" );
	     LOG.error(f.getTrace());
	     LOG.error( "+-----------------------------------------------------------------------------+" );
	   } catch ( Exception e ) {
	     e.printStackTrace( );
	   }
	 }
}
