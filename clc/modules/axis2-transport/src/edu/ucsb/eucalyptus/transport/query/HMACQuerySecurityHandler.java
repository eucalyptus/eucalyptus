/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.transport.query;

import edu.ucsb.eucalyptus.cloud.entities.EntityWrapper;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.keys.Hashes;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class HMACQuerySecurityHandler implements QuerySecurityHandler {

  private static Logger LOG = Logger.getLogger( HMACQuerySecurityHandler.class );

  public static SimpleDateFormat[] iso8601 = {
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'Z" )
  };

  public enum SecurityParameter {
    AWSAccessKeyId, Timestamp, Expires, Signature, Authorization, Date, Content_MD5, Content_Type
  }


  enum SubResource {
    acl, logging, torrent, location
  }

  public abstract UserInfo handle(String addr, String verb, Map<String, String> parameters, Map<String, String> headers ) throws QuerySecurityException;

  protected Calendar parseTimestamp( final String timestamp ) throws QuerySecurityException
  {
    boolean found = false;
    Calendar ts = Calendar.getInstance();
    for ( SimpleDateFormat tsFormat : iso8601 )
    {
      try
      {
        found = true;
        ts.setTime( tsFormat.parse( timestamp ) );
        break;
      }
      catch ( ParseException e ) {}
    }
    if ( !found )
      throw new QuerySecurityException( "Invalid timestamp format." );
    return ts;
  }

  protected String makeSubjectString( final Map<String, String> parameters )
  {
    String paramString = "";
    Set<String> sortedKeys = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
    sortedKeys.addAll( parameters.keySet() );
    for ( String key : sortedKeys ) {
      paramString = paramString.concat( key );
      String val = parameters.get( key );
      if( val != null ) {
        paramString = paramString.concat( val.replaceAll( "\\+"," " ) );
      }
    }
    return paramString;
  }
  protected String makeV2SubjectString( String httpMethod, String host, String path, final Map<String, String> parameters )
  {
    StringBuilder sb = new StringBuilder();
    sb.append( httpMethod );
    sb.append( "\n" );
    sb.append( host );
    sb.append( "\n" );
    sb.append( path );
    sb.append( "\n" );
    String prefix = sb.toString();
    sb = new StringBuilder( );
    NavigableSet<String> sortedKeys = new TreeSet<String>( );
    sortedKeys.addAll( parameters.keySet() );
    String firstKey = sortedKeys.pollFirst();
    sb.append( java.net.URLEncoder.encode( firstKey  )).append( "=" ).append( java.net.URLEncoder.encode( parameters.get( firstKey ).replaceAll( "\\+"," " ) ) );
    while ( ( firstKey = sortedKeys.pollFirst() ) != null ) {
      sb.append( "&" ).append( java.net.URLEncoder.encode( firstKey ) ).append( "=" ).append( java.net.URLEncoder.encode( parameters.get( firstKey ).replaceAll( "\\+"," " ) ) );
    }
    String subject = prefix + sb.toString();
    LOG.info( "VERSION2: " + subject );
    return subject;
  }

  protected String makePlusSubjectString( final Map<String, String> parameters )
  {
    String paramString = "";
    Set<String> sortedKeys = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
    sortedKeys.addAll( parameters.keySet() );
    for ( String key : sortedKeys ) {
      paramString = paramString.concat( key );
      String val = parameters.get( key );
      if( val != null ) {
        paramString = paramString.concat( val );
      }
    }
    return paramString;
  }

  protected String checkSignature( final String queryKey, final String subject ) throws QuerySecurityException
  {
    SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes(), Hashes.Mac.HmacSHA1.toString() );
    try
    {
      Mac mac = Mac.getInstance( Hashes.Mac.HmacSHA1.toString() );
      mac.init( signingKey );
      byte[] rawHmac = mac.doFinal( subject.getBytes() );
      return Base64.encode( rawHmac ).replaceAll( "=", "" );
    }
    catch ( Exception e )
    {
      LOG.error( e, e );
      throw new QuerySecurityException( "Failed to compute signature" );
    }
  }
  protected String checkSignature256( final String queryKey, final String subject ) throws QuerySecurityException
  {
    SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes(), Hashes.Mac.HmacSHA256.toString() );
    try
    {
      Mac mac = Mac.getInstance( Hashes.Mac.HmacSHA256.toString() );
      mac.init( signingKey );
      byte[] rawHmac = mac.doFinal( subject.getBytes() );
      return Base64.encode( rawHmac ).replaceAll( "=", "" );
    }
    catch ( Exception e )
    {
      LOG.error( e, e );
      throw new QuerySecurityException( "Failed to compute signature" );
    }
  }

  protected UserInfo findUserId( final String queryId ) throws QuerySecurityException
  {
    String queryKey;
    UserInfo searchUser = new UserInfo();
    searchUser.setQueryId( queryId );
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    List<UserInfo> userList = db.query( searchUser );
    if ( userList.size() != 1 )
    {
      db.rollback();
      throw new QuerySecurityException( "Invalid user query id: " + queryId );
    }
    db.commit();
    return userList.get( 0 );
  }

  protected String findQueryKey( final String queryId ) throws QuerySecurityException
  {
    String queryKey;
    UserInfo searchUser = new UserInfo();
    searchUser.setQueryId( queryId );
    EntityWrapper<UserInfo> db = new EntityWrapper<UserInfo>();
    List<UserInfo> userList = db.query( searchUser );
    if ( userList.size() != 1 )
    {
      db.rollback();
      throw new QuerySecurityException( "Invalid user query id: " + queryId );
    }
    queryKey = userList.get( 0 ).getSecretKey();
    db.commit();
    return queryKey;
  }

}
