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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.util;

import static com.eucalyptus.crypto.util.Timestamps.Type;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.SecurityHeader;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.eucalyptus.crypto.util.Timestamps;
import com.eucalyptus.util.Strings;
import com.eucalyptus.ws.protocol.RequiredQueryParams;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class HmacUtils {
  private static final Logger LOG = Logger.getLogger( HmacUtils.class );
  
  private static final List<String> PARAMETERS_V1 = ImmutableList.of(
      RequiredQueryParams.SignatureVersion.toString( ), 
      SecurityParameter.Signature.parameter(),
      SecurityParameter.AWSAccessKeyId.parameter() );

  private static final List<String> PARAMETERS_V2 = ImmutableList.of(
      RequiredQueryParams.SignatureVersion.toString(),
      SecurityParameter.SignatureMethod.parameter(),
      SecurityParameter.Signature.parameter(),
      SecurityParameter.AWSAccessKeyId.parameter() );

  private static final List<String> PARAMETERS_V4_QUERY = ImmutableList.of(
      SecurityParameter.X_Amz_Algorithm.parameter(),
      SecurityParameter.X_Amz_Algorithm.parameter(),
      SecurityParameter.X_Amz_Signature.parameter(),
      SecurityParameter.X_Amz_SignedHeaders.parameter() );

  public enum SignatureVariant {
    SignatureV1Standard( SignatureVersion.SignatureV1 ) {
      @Override
      protected Date getTimestamp( @Nonnull final Function<String, List<String>> headerLookup, 
                                   @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return getSignatureDateFrom( "parameter", parameterLookup, SecurityParameter.Timestamp.parameter(), Type.ISO_8601 );
      }

      @Override
      public Collection<String> getParametersToRemove() {
        return PARAMETERS_V1;
      }

      @Override
      public Collection<String> getDateParametersToRemove() {
        return Collections.singleton( SecurityParameter.Timestamp.parameter() );
      }

      @Override
      public String getSignature( @Nonnull final Function<String, List<String>> headerLookup, 
                                  @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return lookupUniqueRequired( parameterLookup, "parameter", SecurityParameter.Signature.parameter() );
      }

      @Override
      public String getAccessKeyId( @Nonnull final Function<String, List<String>> headerLookup, 
                                    @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return lookupUniqueRequired( parameterLookup, "parameter", SecurityParameter.AWSAccessKeyId.parameter() );
      }

      @Override
      public Hmac getSignatureMethod( @Nonnull final Function<String, List<String>> headerLookup, 
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return Hmac.HmacSHA1;
      }

      @Override
      public Map<String, String> getAuthorizationParameters( @Nonnull final Function<String, List<String>> headerLookup, 
                                                             @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        throw new AuthenticationException("Authorization parameters not found");
      }

      @Override
      public String getSecurityToken( @Nonnull final Function<String, List<String>> headerLookup,
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return lookupUnique( parameterLookup, "parameter", SecurityParameter.SecurityToken.parameter() );
      }
    },
    SignatureV2Standard( SignatureVersion.SignatureV2 ) {
      @Override
      protected Date getTimestamp( @Nonnull final Function<String, List<String>> headerLookup,
                                   @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return SignatureV1Standard.getTimestamp( headerLookup, parameterLookup );
      }

      @Override
      public Collection<String> getParametersToRemove() {
        return PARAMETERS_V2;
      }

      @Override
      public Collection<String> getDateParametersToRemove() {
        return SignatureV1Standard.getDateParametersToRemove();
      }

      @Override
      public String getSignature( @Nonnull final Function<String, List<String>> headerLookup, 
                                  @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return SignatureV1Standard.getSignature( headerLookup, parameterLookup );
      }

      @Override
      public String getAccessKeyId( @Nonnull final Function<String, List<String>> headerLookup, 
                                    @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return SignatureV1Standard.getAccessKeyId( headerLookup, parameterLookup );
      }

      @Override
      public Hmac getSignatureMethod( @Nonnull final Function<String, List<String>> headerLookup, 
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        String sigMethod = lookupUnique( parameterLookup, "parameter", SecurityParameter.SignatureMethod.parameter() );
        sigMethod = ( ( sigMethod == null ) ? "HMACSHA1" : sigMethod );
        try {
          return Hmac.valueOf( "HmacSHA" + sigMethod.substring( 7 ) );
        } catch ( IllegalArgumentException e ) {
          throw new AuthenticationException( "Invalid signature method: " + sigMethod );
        }
      }

      @Override
      public Map<String, String> getAuthorizationParameters( @Nonnull final Function<String, List<String>> headerLookup,
                                                             @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return SignatureV1Standard.getAuthorizationParameters( headerLookup, parameterLookup );
      }

      @Override
      public String getSecurityToken( @Nonnull final Function<String, List<String>> headerLookup,
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return SignatureV1Standard.getSecurityToken( headerLookup, parameterLookup );
      }
    },
    SignatureV4Standard( SignatureVersion.SignatureV4 ) {
      private final Splitter CSV_SPLITTER = Splitter.onPattern("\\s*,\\s*").trimResults().omitEmptyStrings();
      private final Splitter NVP_SPLITTER = Splitter.onPattern("\\s*=\\s*").limit(2).trimResults().omitEmptyStrings();

      @Override
      protected Date getTimestamp( @Nonnull final Function<String, List<String>> headerLookup,
                                   @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        
        Date timestamp = getSignatureDateFrom( "header", headerLookup, SecurityHeader.X_Amz_Date.header(), Type.ISO_8601 );
        if ( timestamp == null ) {
          timestamp = getSignatureDateFrom( "header", headerLookup, SecurityHeader.Date.header(), Type.RFC_2616 );
        }
        return timestamp;
      }

      @Override
      public Collection<String> getParametersToRemove() {
        return Collections.emptyList();
      }

      @Override
      public Collection<String> getDateParametersToRemove() {
        return Collections.emptyList();
      }

      @Override
      public String getSignature( @Nonnull final Function<String, List<String>> headerLookup, 
                                  @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return getAuthorizationParameters( headerLookup, parameterLookup ).get( "Signature" );
      }

      @Override
      public String getAccessKeyId( @Nonnull final Function<String, List<String>> headerLookup, 
                                    @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return getSignatureCredential( headerLookup, parameterLookup ).getAccessKeyId();
      }

      @Override
      public Hmac getSignatureMethod( @Nonnull final Function<String, List<String>> headerLookup, 
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return Hmac.HmacSHA256;
      }

      @Override
      public Map<String,String> getAuthorizationParameters( @Nonnull final Function<String, List<String>> headerLookup,
                                                            @Nonnull final Function<String, List<String>> parameterLookup) throws AuthenticationException {
        final String auth = lookupUniqueRequired( headerLookup, "header", HttpHeaders.Names.AUTHORIZATION )
            .replaceFirst(SecurityHeader.Value.AWS4_HMAC_SHA256.value(),"").trim();
        final Iterable<String> authParts = CSV_SPLITTER.split( auth );
        final Map<String,String> authParams = Maps.newHashMap();
        for ( final String nvp : authParts ) {
          final Iterable<String> nameAndValue = NVP_SPLITTER.split( nvp );
          final String name = Iterables.get( nameAndValue, 0, "" );
          final String value = Iterables.get( nameAndValue, 1, "" );
          if ( !name.isEmpty() && !value.isEmpty() ) {
            authParams.put( name, value );
          }
        }
        
        if ( !authParams.keySet().containsAll( Lists.newArrayList( "Credential", "SignedHeaders", "Signature" ) ) ) {
          throw new AuthenticationException( "Invalid authorization header: " + auth );
        }
        
        return authParams;
      }

      private SignatureCredential getSignatureCredential( @Nonnull final Function<String, List<String>> headerLookup,
                                                          @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return new SignatureCredential( getAuthorizationParameters( headerLookup, parameterLookup ).get( "Credential" ) );
      }

      @Override
      public String getSecurityToken( @Nonnull final Function<String, List<String>> headerLookup,
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return lookupUnique( headerLookup, "header", SecurityParameter.X_Amz_Security_Token.parameter() );
      }
    },
    SignatureV4Query( SignatureVersion.SignatureV4 ) {
      @Override
      protected Date getTimestamp( @Nonnull final Function<String, List<String>> headerLookup,
                                   @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {

        Date timestamp = getSignatureDateFrom( "parameter", parameterLookup, SecurityParameter.X_Amz_Date.parameter(), Type.ISO_8601 );
        if ( timestamp == null ) {
          timestamp = getSignatureDateFrom( "header", headerLookup, SecurityHeader.Date.header(), Type.RFC_2616 );
        }
        return timestamp;
      }

      @Override
      public Collection<String> getParametersToRemove() {
        return PARAMETERS_V4_QUERY;
      }

      @Override
      public Collection<String> getDateParametersToRemove() {
        return Collections.singleton( SecurityParameter.X_Amz_Date.parameter() );
      }

      @Override
      public String getSignature( @Nonnull final Function<String, List<String>> headerLookup,
                                  @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return lookupUniqueRequired( parameterLookup, "parameter", SecurityParameter.X_Amz_Signature.parameter() );
      }

      @Override
      public String getAccessKeyId( @Nonnull final Function<String, List<String>> headerLookup,
                                    @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return new SignatureCredential( lookupUniqueRequired( parameterLookup, "parameter", SecurityParameter.X_Amz_Signature.parameter() ) ).getAccessKeyId();
      }

      @Override
      public Hmac getSignatureMethod( @Nonnull final Function<String, List<String>> headerLookup,
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return Hmac.HmacSHA256;
      }

      @Override
      public Map<String, String> getAuthorizationParameters( @Nonnull final Function<String, List<String>> headerLookup, 
                                                             @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return ImmutableMap.<String,String>builder()
            .put( "Credential", lookupUniqueRequired( parameterLookup, "parameter", SecurityParameter.X_Amz_Credential.parameter() ) )
            .put( "Signature", getSignature( headerLookup, parameterLookup ) )
            .put( "SignedHeaders", lookupUniqueRequired( parameterLookup, "parameter", SecurityParameter.X_Amz_SignedHeaders.parameter() ) )
            .build();
      }

      @Override
      public String getSecurityToken( @Nonnull final Function<String, List<String>> headerLookup,
                                      @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException {
        return lookupUnique( parameterLookup, "parameter", SecurityParameter.X_Amz_Security_Token.parameter() );
      }
    };

    private final SignatureVersion version;
    
    private SignatureVariant( final SignatureVersion version ) {
      this.version = version;
    }

    @Nonnull
    public SignatureVersion getVersion() {
      return version;
    }

    protected abstract Date getTimestamp( @Nonnull final Function<String,List<String>> headerLookup,
                                          @Nonnull final Function<String,List<String>> parameterLookup ) throws AuthenticationException;


    @Nullable
    private static Date getSignatureDateFrom( final String where,
                                              final Function<String,List<String>> lookup,
                                              final String name,
                                              final Type type ) throws AuthenticationException {
      final String value = lookupUnique( lookup, where, name );
      if ( value != null ) {
        return Timestamps.parseTimestamp( value, type );
      }
      return null;
    }

    public abstract Collection<String> getParametersToRemove();

    public abstract Collection<String> getDateParametersToRemove();

    public abstract String getSignature( @Nonnull final Function<String, List<String>> headerLookup,
                                         @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException;

    public abstract String getAccessKeyId( @Nonnull final Function<String, List<String>> headerLookup,
                                           @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException;

    public abstract Hmac getSignatureMethod( @Nonnull final Function<String, List<String>> headerLookup,
                                             @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException;

    public abstract Map<String,String> getAuthorizationParameters( @Nonnull final Function<String, List<String>> headerLookup,
                                                                   @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException;

    public abstract String getSecurityToken( @Nonnull final Function<String, List<String>> headerLookup,
                                             @Nonnull final Function<String, List<String>> parameterLookup ) throws AuthenticationException;
  }

  public static Function<String,List<String>> headerLookup( final Map<String,List<String>> values ) {
    return forMap( values, Strings.lower() );
  }

  public static Function<String,List<String>> parameterLookup( final Map<String,List<String>> values ) {
    return forMap( values, Functions.<String>identity() );
  }

  private static Function<String,List<String>> forMap( final Map<String,List<String>> values, Function<String,String> keyConversion ) {
    return Functions.compose( Functions.forMap( values, Collections.<String>emptyList() ), keyConversion );
  }

  public enum SignatureVersion {
    SignatureV1(1),
    SignatureV2(2),
    SignatureV4(4);

    private int value;
    
    private SignatureVersion( final int value ) {
      this.value = value;
    }
    
    public int value() {
      return value;
    }
  }

  /**
   * Detect a signature variant from headers / url parameters
   *
   * @param headerLookup Function to get an HTTP header value
   * @param parameterLookup Function to get a parameter value
   * @return The detected signature variant
   * @throws AuthenticationException if a signature variant was not detected or was invalid
   */
  @Nonnull
  public static SignatureVariant detectSignatureVariant( @Nonnull final Function<String,List<String>> headerLookup,
                                                         @Nonnull final Function<String,List<String>> parameterLookup  ) throws AuthenticationException {
    final SignatureVariant variant;
    if ( SecurityHeader.Value.AWS4_HMAC_SHA256.matches( lookupUnique( headerLookup, "header", HttpHeaders.Names.AUTHORIZATION ) ) ) {
      variant = SignatureVariant.SignatureV4Standard;      
    } else if ( lookupUnique( parameterLookup, "parameter", SecurityParameter.X_Amz_Algorithm.parameter() ) != null
        || lookupUnique( parameterLookup, "parameter", SecurityParameter.X_Amz_Date.parameter() ) != null ) {
      variant = SignatureVariant.SignatureV4Query;
    } else {
      String signatureVersion = lookupUnique( parameterLookup, "parameter", RequiredQueryParams.SignatureVersion.toString() );  
      if ( "1".equals( signatureVersion ) ) {
        variant = SignatureVariant.SignatureV1Standard;  
      } else if ( "2".equals( signatureVersion ) || signatureVersion == null ) {
        variant = SignatureVariant.SignatureV2Standard;
      } else {
        throw new AuthenticationException("Unsupported signature version " + signatureVersion );
      }
    }
    return variant;    
  }
  
  /**
   * Check if a date value can be found.
   *
   * @param versions The versions to allow
   * @param headerLookup Function to get an HTTP header value
   * @param parameterLookup Function to get a parameter value
   * @return True if a date value is present
   */
  public static boolean hasSignatureDate( @Nonnull final EnumSet<SignatureVersion> versions,
                                          @Nonnull final Function<String,List<String>> headerLookup,
                                          @Nonnull final Function<String,List<String>> parameterLookup ) throws AuthenticationException {
    return getSignatureDateInternal( versions, headerLookup, parameterLookup ) != null;
  }

  /**
   * Locate and return the date for a signature.
   * 
   * @param versions The versions to allow
   * @param headerLookup Function to get an HTTP header value
   * @param parameterLookup Function to get a parameter value
   * @return The date
   * @throws AuthenticationException If a date could not be located
   */
  @Nonnull
  public static Date getSignatureDate( @Nonnull final EnumSet<SignatureVersion> versions,
                                       @Nonnull final Function<String,List<String>> headerLookup,
                                       @Nonnull final Function<String,List<String>> parameterLookup ) throws AuthenticationException {
    final Date signatureDate = getSignatureDateInternal( versions, headerLookup, parameterLookup );
    if ( signatureDate == null) {
      throw new AuthenticationException("Date not found.");      
    }
    return signatureDate;
  }

  public static String getSignature( final String queryKey, final String subject, final Hmac mac ) throws AuthenticationException {
    SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes( ), mac.toString( ) );
    try {
      Mac digest = mac.getInstance( );
      digest.init( signingKey );
      byte[] rawHmac = digest.doFinal( subject.getBytes( ) );
      return Base64.encode( rawHmac ).replaceAll( "=", "" );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new AuthenticationException( "Failed to compute signature" );
    }
  }

  public static String makeSubjectString( final Map<String, String> parameters ) {
    String paramString = "";
    Set<String> sortedKeys = new TreeSet<String>( String.CASE_INSENSITIVE_ORDER );
    sortedKeys.addAll( parameters.keySet( ) );
    for ( String key : sortedKeys )
      paramString = paramString.concat( key ).concat( parameters.get( key ).replaceAll( "\\+", " " ) );
    try {
      return new String(URLCodec.decodeUrl( paramString.getBytes() ) );
    } catch ( DecoderException e ) {
      return paramString;
    }
  }

  public static String makeV2SubjectString( String httpMethod, String host, String path, final Map<String, String> parameters ) {
    parameters.remove("");
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
    if( firstKey != null ) { 
      sb.append( urlEncode( firstKey ) ).append( "=" ).append( urlEncode( parameters.get( firstKey ).replaceAll( "\\+", " " ) ) );
    } 
    while ( ( firstKey = sortedKeys.pollFirst( ) ) != null ) {
      sb.append( "&" ).append( urlEncode( firstKey ) ).append( "=" ).append( urlEncode( parameters.get( firstKey ).replaceAll( "\\+", " " ) ) );
    }
    String subject = prefix + sb.toString( );
    LOG.trace( "VERSION2: " + subject );
    return subject;
  }

  public static String urlEncode( String s ) {
    try {
      return new URLCodec().encode( s ,"UTF-8" );
    } catch ( UnsupportedEncodingException e ) {
      return s;
    }
  }

  private static String lookupUnique( final Function<String,List<String>> lookup, String type, String name ) throws AuthenticationException {
    String result = null;
    final Iterable<String> values = lookup.apply( name );
    if ( values != null ) for ( String value : values ) {
      if ( result == null ) {
        result = value;
      } else if ( value != null && !value.equals( result ) ) {
        throw new AuthenticationException("Duplicate " + type + " for " + name +" with incorrect value " + value + ", expected " + result);
      }
    }
    return result;
  }

  private static String lookupUniqueRequired( final Function<String,List<String>> lookup, String type, String name ) throws AuthenticationException {
    String value = lookupUnique( lookup, type, name );
    if ( value == null ) {
      throw new AuthenticationException( "Missing required parameter: " +name );
    }
    return value;
  }
  /**
   * TODO:GUAVA:Optional 
   */
  @Nullable
  private static Date getSignatureDateInternal( @Nonnull final EnumSet<SignatureVersion> versions,
                                                @Nonnull final Function<String,List<String>> headerLookup,
                                                @Nonnull final Function<String,List<String>> parameterLookup ) throws AuthenticationException {
    final SignatureVariant variant = detectSignatureVariant( headerLookup, parameterLookup );
    if ( !versions.contains( variant.getVersion() ) ) {
      return null;
    }

    return variant.getTimestamp( headerLookup, parameterLookup );
  }

  /**
   * A signature V4 credential.
   * 
   * A slash('/')-separated string that is formed by concatenating your Access 
   * Key ID and your credential scope components. Credential scope comprises 
   * the date (YYYYMMDD), the AWS region, the service name, and a special 
   * termination string (aws4_request). For example, the following string 
   * represents the Credential parameter for an IAM request in the US East 
   * Region.
   *
   *   |-- Access Key ID --|------- Credential Scope ---------|
   *   AKIAIOSFODNN7EXAMPLE/20111015/us-east-1/iam/aws4_request
   *
   * Important
   *
   *   You must use lowercase characters for the region, service name, and 
   *   special termination string.
   */
  public static class SignatureCredential {
    @Nonnull private final String accessKeyId;
    @Nonnull private final String date;
    @Nonnull private final String region;
    @Nonnull private final String serviceName;
    @Nonnull private final String terminator;

    public SignatureCredential( @Nonnull final String credential ) throws AuthenticationException {
      final String[] credentialParts = credential.trim().split( "/" );
      if ( credentialParts.length != 5 ) {
        throw new AuthenticationException("Invalid credential (missing part): [" + credential + "]");
      }

      accessKeyId = credentialParts[0];
      date = credentialParts[1];
      region = credentialParts[2];
      serviceName = credentialParts[3];
      terminator = credentialParts[4];
    }

    public void verify( @Nonnull  final Date timestamp,
                        @Nullable final String region,
                        @Nullable final String serviceName,
                        @Nullable final String terminator ) throws AuthenticationException {
      final String expectedDate = Timestamps.formatShortIso8601Date( timestamp );
      internalVerify( "date", expectedDate, this.date );
      internalVerify( "region", region, this.region );
      internalVerify( "service name", serviceName, this.serviceName );
      internalVerify( "termination string", terminator, this.terminator );
    }

    private void internalVerify( final String what,
                                 final String expect,
                                 final String value ) throws AuthenticationException {
      if ( expect != null && !expect.equals( value ) ) {
        throw new AuthenticationException("Expected "+what+" does not match credential "+what+" [" + expect + " != " + value + "]");
      }
    }

    @Nonnull
    public String getAccessKeyId() {
      return accessKeyId;
    }

    @Nonnull
    public String getDate() {
      return date;
    }

    @Nonnull
    public String getRegion() {
      return region;
    }

    @Nonnull
    public String getServiceName() {
      return serviceName;
    }

    @Nonnull
    public String getTerminator() {
      return terminator;
    }
    
    @Nonnull
    public String getCredentialScope() {
      return date + "/" + region + "/" + serviceName + "/" + terminator;
    }
  }  
}
