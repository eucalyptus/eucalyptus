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

package com.eucalyptus.crypto.util;

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
