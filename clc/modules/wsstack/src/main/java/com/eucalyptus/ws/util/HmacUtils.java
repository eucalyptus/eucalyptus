package com.eucalyptus.ws.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;

import com.eucalyptus.ws.AuthenticationException;

public class HmacUtils {
  private static Logger            LOG     = Logger.getLogger( HmacUtils.class );
  public static SimpleDateFormat[] iso8601 = {
    new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" ),
    new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'Z" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" ),
      new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'Z" ) };

  public static Calendar parseTimestamp( final String timestamp ) throws AuthenticationException {
    Calendar ts = Calendar.getInstance( );
    for ( SimpleDateFormat tsFormat : iso8601 ) {
      try {
//        tsFormat.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        ts.setTime( tsFormat.parse( timestamp ) );
        return ts;
      } catch ( ParseException e ) {
        LOG.info( e, e );
      }
    }
    throw new AuthenticationException( "Invalid timestamp format." );
  }

  public static String checkSignature256( final String queryKey, final String subject ) throws AuthenticationException {
    SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes( ), Hashes.Mac.HmacSHA256.toString( ) );
    try {
      Mac mac = Mac.getInstance( Hashes.Mac.HmacSHA256.toString( ) );
      mac.init( signingKey );
      byte[] rawHmac = mac.doFinal( subject.getBytes( ) );
      return Base64.encode( rawHmac ).replaceAll( "=", "" );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new AuthenticationException( "Failed to compute signature" );
    }
  }

  public static String makeV2SubjectString( String httpMethod, String host, String path, final Map<String, String> parameters ) {
    StringBuilder sb = new StringBuilder( );
    sb.append( httpMethod );
    sb.append( "\n" );
    sb.append( host );
    sb.append( "\n" );
    sb.append( path );
    sb.append( "\n" );
    String prefix = sb.toString( );
    sb = new StringBuilder( );
    NavigableSet<String> sortedKeys = new TreeSet<String>( );
    sortedKeys.addAll( parameters.keySet( ) );
    String firstKey = sortedKeys.pollFirst( );
    sb.append( java.net.URLEncoder.encode( firstKey ) ).append( "=" ).append( java.net.URLEncoder.encode( parameters.get( firstKey ).replaceAll( "\\+", " " ) ) );
    while ( ( firstKey = sortedKeys.pollFirst( ) ) != null ) {
      sb.append( "&" ).append( java.net.URLEncoder.encode( firstKey ) ).append( "=" ).append( java.net.URLEncoder.encode( parameters.get( firstKey ).replaceAll( "\\+", " " ) ) );
    }
    String subject = prefix + sb.toString( );
    LOG.info( "VERSION2: " + subject );
    return subject;
  }

}
