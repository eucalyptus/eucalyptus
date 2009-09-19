package com.eucalyptus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Event;
import com.eucalyptus.event.EventListener;
import com.google.common.collect.Lists;

public class DebugUtil {
  private static Logger LOG = Logger.getLogger( DebugUtil.class );
  public static boolean DEBUG = "DEBUG".equals( System.getProperty( "euca.log.level" ) );

  public static StackTraceElement getMyStackTraceElement( ) {
    Exception e = new Exception( );
    e.fillInStackTrace( );
    for ( StackTraceElement ste : e.getStackTrace( ) ) {
      if ( ste.getClassName( ).startsWith( EntityWrapper.class.getCanonicalName( ).replaceAll( "\\.EntityWrapper.*", "" ) ) || ste.getMethodName( ).equals( "getEntityWrapper" ) ) {
        continue;
      } else {
        return ste;
      }
    }
    throw new RuntimeException( "BUG: Reached bottom of stack trace without finding any relevent frames." );
  }

  public static Throwable checkForCauseOfInterest( Throwable e, Class<? extends Throwable>... interestingExceptions ) {
    Throwable cause = e;
    Throwable rootCause = e;
    List interesting = Arrays.asList( interestingExceptions );
    for ( int i = 0; i < 100 && cause != null; i++, rootCause = cause, cause = cause.getCause( ) ) {
      if ( interesting.contains( cause.getClass( ) ) ) {
        return cause;
      }
    }
    LOG.debug( "-> Ignoring unrelated exception: ex=" + e.getClass( ) + " root=" + rootCause );
    return new ExceptionNotRelatedException( interesting.toString( ), e );
  }

}
