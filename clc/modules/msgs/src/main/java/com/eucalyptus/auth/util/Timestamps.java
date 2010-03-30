package com.eucalyptus.auth.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.login.AuthenticationException;

public class Timestamps {
  private static Logger LOG = Logger.getLogger( Timestamps.class );
  public static Calendar parseTimestamp( final String timestamp ) throws AuthenticationException {
    Calendar ts = Calendar.getInstance( );
    for ( String tsPattern : Timestamps.iso8601 ) {
      try {
        SimpleDateFormat tsFormat = new SimpleDateFormat( tsPattern );
        tsFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        ts.setTime( tsFormat.parse( timestamp ) );
        return ts;
      } catch ( ParseException e ) {
        LOG.debug( e, e );
      }
    }
    throw new AuthenticationException( "Invalid timestamp format." );
  }

  private static String[] iso8601 = {
  "yyyy-MM-dd'T'HH:mm:ss",
  "yyyy-MM-dd'T'HH:mm:ssZ",
  "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
  "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
  "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z",
  "yyyy-MM-dd'T'HH:mm:ss'Z'",
  "yyyy-MM-dd'T'HH:mm:ss'Z'Z" };

}
