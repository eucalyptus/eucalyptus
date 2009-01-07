package edu.ucsb.eucalyptus.util;

import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;

public class Admin {

  private static Logger LOG = Logger.getLogger( Admin.class );

  public static <E extends EucalyptusMessage> E makeMsg( Class<E> clazz, EucalyptusMessage regarding ) {
    try {
      E msg = clazz.newInstance();
      msg.setUserId( EucalyptusProperties.NAME );
      msg.setEffectiveUserId( EucalyptusProperties.NAME );
      StackTraceElement elem = new Throwable().fillInStackTrace().getStackTrace()[ 1 ];
      msg.setCorrelationId( String.format( "%s.%s.%s-%s", elem.getClassName(), elem.getMethodName(), elem.getLineNumber(), regarding.getCorrelationId() ) );
      return msg;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }

  private static boolean accepts( Class[] formal, Object[] actual ) {
    if ( formal.length != actual.length ) return false;
    boolean ret = true;
    for ( int i = 0; i < actual.length; i++ ) ret &= formal[ i ].isAssignableFrom( actual[ i ].getClass() );
    return ret;
  }

  public static <E extends EucalyptusMessage> E makeMsg( Class<E> clazz, Object... args ) {
    try {
      E msg = null;
      if ( args.length != 0 ) {
        for ( Constructor c : clazz.getConstructors() ) {
          if ( accepts( c.getParameterTypes(), args ) ) {
            msg = ( E ) c.newInstance( args );
          }
        }
      }
      if ( msg == null ) {
        if ( args.length > 0 )
          LOG.error( "Failed to find a constructor for requested message: " + clazz + " with args " + Lists.newArrayList( args ), new Throwable() );
        msg = clazz.newInstance();
      }
      if ( msg.getUserId() == null )
        msg.setUserId( EucalyptusProperties.NAME );
      msg.setEffectiveUserId( EucalyptusProperties.NAME );
      StackTraceElement elem = new Throwable().fillInStackTrace().getStackTrace()[ 1 ];
      EucalyptusMessage reMsg = msg;
      msg.setCorrelationId( String.format( "%s.%s.%s-%s", elem.getClassName(), elem.getMethodName(), elem.getLineNumber(), reMsg.getCorrelationId() ) );
      return msg;
    } catch ( Exception e ) {
      LOG.error( e, e );
      return null;
    }
  }
}
