/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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

package com.eucalyptus.auth.login;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;
import org.apache.xml.security.utils.Base64;
import com.eucalyptus.auth.AccessKeys;
import com.eucalyptus.auth.InvalidSignatureAuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.crypto.Hmac;
import com.eucalyptus.crypto.util.SecurityParameter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class Hmacv2LoginModule extends HmacLoginModuleSupport {
  private static Logger LOG = Logger.getLogger( Hmacv2LoginModule.class );

  public Hmacv2LoginModule() {
    super(2);
  }

  @Override
  public boolean authenticate( HmacCredentials credentials ) throws Exception {
    String sig = credentials.getSignature( );
    AccessKey accessKey = lookupAccessKey( credentials );
    UserPrincipal user = accessKey.getPrincipal( );
    String secretKey = accessKey.getSecretKey();
    String canonicalString = this.makeSubjectString( credentials.getVerb(), credentials.getHeaderHost(), credentials.getServicePath(), credentials.getParameters() );
    String canonicalStringWithPort = this.makeSubjectString( credentials.getVerb(), credentials.getHeaderHost() + ":" + credentials.getHeaderPort(), credentials.getServicePath(), credentials.getParameters() );
    String canonicalStringWithAwsCliPath = this.makeSubjectString( credentials.getVerb( ), credentials.getHeaderHost( ) + ":" + credentials.getHeaderPort( ), convertForAwsCli( credentials.getServicePath() ), credentials.getParameters( ) );
    String computedSig = this.getSignature( secretKey, canonicalString, credentials.getSignatureMethod() );
    String computedSigWithPort = this.getSignature( secretKey, canonicalStringWithPort, credentials.getSignatureMethod( ) );
    String computedSigWithAwsCliPath = this.getSignature( secretKey, canonicalStringWithAwsCliPath, credentials.getSignatureMethod( ) );
    if ( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) && !computedSigWithAwsCliPath.equals( sig ) ) {
      sig = sanitize( urldecode( sig ) );
      computedSig = this.getSignature( secretKey, canonicalString.replaceAll("\\+","%2B"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
      computedSigWithPort = this.getSignature( secretKey, canonicalStringWithPort.replaceAll("\\+","%2B"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
      computedSigWithAwsCliPath = this.getSignature( secretKey, canonicalStringWithAwsCliPath.replaceAll("\\+","%2B"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
      if( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) && !computedSigWithAwsCliPath.equals( sig ) ) {
        computedSig = this.getSignature( secretKey, canonicalString.replaceAll("\\+","%20"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
        computedSigWithPort = this.getSignature( secretKey, canonicalStringWithPort.replaceAll("\\+","%20"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
        computedSigWithAwsCliPath = this.getSignature( secretKey, canonicalStringWithAwsCliPath.replaceAll("\\+","%20"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
        if( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) && !computedSigWithAwsCliPath.equals( sig ) ) {
          computedSig = this.getSignature( secretKey, canonicalString.replaceAll("\\*","%2A"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
          computedSigWithPort = this.getSignature( secretKey, canonicalStringWithPort.replaceAll("\\*","%2A"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
          computedSigWithAwsCliPath = this.getSignature( secretKey, canonicalStringWithAwsCliPath.replaceAll("\\*","%2A"), credentials.getSignatureMethod( ) ).replaceAll("\\+"," ");
          if( !computedSig.equals( sig ) && !computedSigWithPort.equals( sig ) && !computedSigWithAwsCliPath.equals( sig ) ) {
            throw new InvalidSignatureAuthException( "Signature validation failed" );
          }
        }
      }
    }
    super.setCredential( credentials.getCredential( AccessKeys.getKeyType( accessKey ) ) );
    super.setPrincipal( user );
    return true;
  }

  private String makeSubjectString( String httpMethod, String host, String path, final Map<String, List<String>> parameters ) throws UnsupportedEncodingException {
    parameters.remove("");
    StringBuilder sb = new StringBuilder( );
    sb.append( httpMethod );
    sb.append( "\n" );
    sb.append( Strings.nullToEmpty( host ).toLowerCase( ) );
    sb.append( "\n" );
    sb.append( path );
    sb.append( "\n" );
    String prefix = sb.toString( );
    sb = new StringBuilder( );
    boolean addedParam = false;
    for ( final String paramName : Ordering.natural().sortedCopy( parameters.keySet( ) ) ) {
      if ( SecurityParameter.Signature.parameter().equals( paramName ) ) continue;
      List<String> paramValues = parameters.get( paramName );
      if ( paramValues.isEmpty() ) paramValues = Lists.newArrayList( "" );
      for ( final String value : Ordering.natural().sortedCopy( paramValues ) ) {
        sb.append( urlencode( paramName ) ).append( "=" ).append( urlencode( value ) ).append( "&" );
        addedParam = true;
      }
    }
    if (addedParam) sb.setLength( sb.length() - 1 );
    String subject = prefix + sb.toString( );
    signatureLogger.trace( "VERSION2: " + subject );
    return subject;
  }

  public String getSignature( final String queryKey, final String subject, final Hmac mac ) throws AuthenticationException {
    SecretKeySpec signingKey = new SecretKeySpec( queryKey.getBytes( ), mac.toString( ) );
    try {
      byte[] rawHmac = mac.digestBinary( signingKey, subject.getBytes( ) );
      return sanitize( Base64.encode( rawHmac ) );
    } catch ( Exception e ) {
      LOG.error( e, e );
      throw new AuthenticationException( "Failed to compute signature" );
    }
  }

  /**
   * Convert for compatibility with AWS CLI tools.
   *
   * 1) Trim the trailing "/"
   * 2) Convert to lower case
   *
   * @return The converted path.
   */
  private String convertForAwsCli( final String path ) {
    String converted = Strings.nullToEmpty( path );
    if ( converted.endsWith( "/" ) ) {
      converted = converted.substring( 0, converted.length() - 1 );
    }
    return converted.toLowerCase();
  }

}
