/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.ws.StackConfiguration;
import com.eucalyptus.ws.util.HmacUtils;
import com.google.common.base.Function;

public class QueryTimestampHandler extends MessageStackHandler {
  private static final Logger LOG = Logger.getLogger( QueryTimestampHandler.class );
  private static final EnumSet<HmacUtils.SignatureVersion> SIGNATURE_VERSIONS = EnumSet.allOf(HmacUtils.SignatureVersion.class);
  
  @Override
  public void incomingMessage( final MessageEvent event ) throws AuthenticationException {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      final MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );      
      final Map<String, String> parameters = httpRequest.getParameters( );
      final Function<String,List<String>> headerLookup = SignatureHandlerUtils.headerLookup( httpRequest );
      final Function<String,List<String>> parameterLookup = SignatureHandlerUtils.parameterLookup( httpRequest );      
      
      final boolean hasSignatureDate = HmacUtils.hasSignatureDate( SIGNATURE_VERSIONS, headerLookup, parameterLookup );
      if ( !hasSignatureDate &&
           !parameters.containsKey( SecurityParameter.Expires.parameter( ) ) ) {
        throw new AuthenticationException( "One of the following parameters must be specified: " + SecurityParameter.Timestamp.parameter() + " OR "
          + SecurityParameter.Expires.parameter() + " OR " + SecurityParameter.X_Amz_Date.parameter() + " OR " + SecurityHeader.Date.header( ) );
      }
      final Calendar now = Calendar.getInstance( );
      final Calendar expires = Calendar.getInstance( );
      String timestamp = null;
      String exp = null;
      try {
        if ( hasSignatureDate ) {
          final Date date = HmacUtils.getSignatureDate( SIGNATURE_VERSIONS, headerLookup, parameterLookup );
          parameters.keySet().removeAll( HmacUtils.detectSignatureVariant( headerLookup, parameterLookup ).getDateParametersToRemove() );
          expires.setTimeInMillis( verifyTimestampAndCalculateExpiry( now.getTimeInMillis(), date ) );
        } else {
          exp = parameters.remove( SecurityParameter.Expires.parameter( ) );
          try {
            expires.setTime( Timestamps.parseIso8601Timestamp( exp ) );
          } catch ( Exception e ) {
            expires.setTime( Timestamps.parseIso8601Timestamp( URLDecoder.decode( exp ) ) );
          }
          // in case of Expires, for now, we accept arbitrary time in the future
          Calendar cacheExpire = ( Calendar ) now.clone( );
          cacheExpire.add( Calendar.MINUTE, 15 );
          if ( expires.after( cacheExpire ) )
            LOG.warn( "[security] Message expiration date " + expires + " is further in the future that replay cache expiration" );
        }
      } catch ( AuthenticationException a ) {
        LOG.debug( a, a );
        throw a;
      } catch ( Exception t ) {
        LOG.debug( t, t );
        throw new AuthenticationException( "Failure to parse timestamp: Timestamp=" + timestamp + " Expires=" + exp );
      }
      
      if ( now.after( expires ) ) {
        expires.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        String expiryTime = String.format( "%4d-%02d-%02dT%02d:%02d:%02d",
                                           expires.get( Calendar.YEAR ),
                                           expires.get( Calendar.MONTH ) + 1,
                                           expires.get( Calendar.DAY_OF_MONTH ),
                                           expires.get( Calendar.HOUR_OF_DAY ),
                                           expires.get( Calendar.MINUTE ),
                                           expires.get( Calendar.SECOND ) );
        throw new AuthenticationException( "Message has expired (times in UTC): Timestamp=" + timestamp + " Expires=" + exp + " Deadline=" + expiryTime );
      }
    }
  }

  private long verifyTimestampAndCalculateExpiry( final long now, 
                                                  final Date timestamp ) throws AuthenticationException {
    // allow 20 secs for clock drift
    final long maxTimestamp = now + TimeUnit.SECONDS.toMillis( StackConfiguration.CLOCK_SKEW_SEC );
    
    // make sure that the message wasn't generated in the future
    if ( maxTimestamp < timestamp.getTime() ) {
      throw new AuthenticationException( "Message was generated in the future (times in UTC): Timestamp=" + timestamp );
    }
    
    // allow caching for 15 mins + 20 secs for clock drift
    return timestamp.getTime() + TimeUnit.SECONDS.toMillis( 900 + StackConfiguration.CLOCK_SKEW_SEC );
  }
}
