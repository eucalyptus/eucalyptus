/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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

package com.eucalyptus.www;

import static com.eucalyptus.auth.AuthenticationProperties.CredentialDownloadGenerateCertificateStrategy;
import static com.eucalyptus.auth.AuthenticationProperties.CredentialDownloadGenerateCertificateStrategy.Absent;
import static com.eucalyptus.auth.AuthenticationProperties.CredentialDownloadGenerateCertificateStrategy.Limited;
import static com.eucalyptus.auth.principal.Certificate.Util.revoked;
import static com.eucalyptus.util.CollectionUtils.propertyPredicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eucalyptus.auth.AuthenticationProperties;
import com.eucalyptus.bootstrap.Host;
import com.eucalyptus.bootstrap.Hosts;
import com.eucalyptus.cloudformation.CloudFormation;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.User.RegistrationStatus;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.cloudwatch.common.CloudWatch;
import com.eucalyptus.component.ServiceBuilder;
import com.eucalyptus.component.ServiceBuilders;
import com.eucalyptus.component.ServiceConfiguration;
import com.eucalyptus.component.ServiceUris;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.Tokens;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.autoscaling.common.AutoScaling;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflow;
import com.eucalyptus.util.Cidr;
import com.eucalyptus.util.Internets;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;

public class X509Download extends HttpServlet {
  
  private static Logger LOG                   = Logger.getLogger( X509Download.class );
  public static String  NAME_SHORT            = "euca2";
  public static String  PARAMETER_USERNAME    = "user";
  public static String  PARAMETER_ACCOUNTNAME = "account";
  public static String  PARAMETER_KEYNAME     = "keyName";
  public static String  PARAMETER_CODE        = "code";
  public static String  PARAMETER_FORCE       = "force";

  public void doGet( HttpServletRequest request, HttpServletResponse response ) {
    String code = request.getParameter( PARAMETER_CODE );
    String userName = request.getParameter( PARAMETER_USERNAME );
    String accountName = request.getParameter( PARAMETER_ACCOUNTNAME );
    String mimetype = "application/zip";
    if ( accountName == null || "".equals( accountName ) ) {
      hasError( HttpServletResponse.SC_BAD_REQUEST, "No account name provided", response );
      return;
    }
    if ( userName == null || "".equals( userName ) ) {
      hasError( HttpServletResponse.SC_BAD_REQUEST, "No user name provided", response );
      return;
    }
    if ( code == null || "".equals( code ) ) {
      hasError( HttpServletResponse.SC_BAD_REQUEST, "Wrong user security code", response );
      return;
    }
    
    User user = null;
    try {
      Account account = Accounts.lookupAccountByName( accountName );
      user = account.lookupUserByName( userName );
      if ( !user.isEnabled( ) || !RegistrationStatus.CONFIRMED.equals( user.getRegistrationStatus( ) ) ) {
        hasError( HttpServletResponse.SC_FORBIDDEN, "Access is not authorized", response );
        return;
      }
    } catch ( AuthException e ) {
      hasError( HttpServletResponse.SC_BAD_REQUEST, "User does not exist", response );
      return;
    } catch ( Exception e ) {
      hasError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Fail to retrieve user data", response );
      return;      
    }
    try {
      if ( !code.equals( user.resetToken( ) ) ) {
        hasError( HttpServletResponse.SC_FORBIDDEN, "Access is not authorized", response );
        return;
      }
    } catch ( Exception e ) {
      hasError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Can not reset user security code", response );
      return;
    }
    response.setContentType( mimetype );
    response.setHeader( "Content-Disposition", "attachment; filename=\"" + X509Download.NAME_SHORT + "-" + userName + "-x509.zip\"" );
    LOG.info( "pushing out the X509 certificate for user " + userName );
    
    byte[] x509zip = null;
    try {
      x509zip = getX509Zip( user, "true".equals( request.getParameter( PARAMETER_FORCE ) ) );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      hasError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Fail to return user credentials", response );
      return;
    }
    try {
      ServletOutputStream op = response.getOutputStream( );
      
      response.setContentLength( x509zip.length );
      
      op.write( x509zip );
      op.flush( );
      
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }
  
  public static void hasError( int statusCode, String message, HttpServletResponse response ) {
    try {
      response.setStatus( statusCode );
      response.setContentType( "text/plain; charset=utf-8" );
      response.getWriter( ).print( getError( message ) );
      response.getWriter( ).flush( );
    } catch ( IOException e ) {
      e.printStackTrace( );
    }
  }
  
  private static byte[] getX509Zip( User u, boolean force ) throws Exception {
    X509Certificate cloudCert = null;
    X509Certificate x509 = null;
    String userAccessKey = null;
    String userSecretKey = null;
    KeyPair keyPair = null;
    try {
      final List<AccessKey> accessKeys = u.getKeys( );
      for ( final AccessKey k : accessKeys ) {
        if ( k.isActive( ) ) {
          userAccessKey = k.getAccessKey( );
          userSecretKey = k.getSecretKey( );
        }
      }
      if ( userAccessKey == null &&
          ( accessKeys.isEmpty( ) || (u.isSystemAdmin( ) && force) || accessKeys.size( ) < AuthenticationProperties.ACCESS_KEYS_LIMIT ) ) {
        final AccessKey k = u.createKey( );
        userAccessKey = k.getAccessKey( );
        userSecretKey = k.getSecretKey( );
      }
      if ( userAccessKey == null ) {
        throw new IllegalStateException( "Access key limit exceeded" );
      }
      final CredentialDownloadGenerateCertificateStrategy certificateStrategy =
          AuthenticationProperties.getCredentialDownloadGenerateCertificateStrategy( );
      final int nonRevokedCertificateCount = Iterables.size(
          Iterables.filter( u.getCertificates( ), propertyPredicate( false, revoked( ) ) ) );
      if ( ( certificateStrategy == Absent && nonRevokedCertificateCount == 0 ) ||
          ( certificateStrategy == Limited && nonRevokedCertificateCount < AuthenticationProperties.SIGNING_CERTIFICATES_LIMIT ) ) {
        // Include a certificate only if the user does not have one
        keyPair = Certs.generateKeyPair( );
        x509 = Certs.generateCertificate( keyPair, u.getName( ) );
        x509.checkValidity( );
        u.addCertificate( x509 );
      }
      cloudCert = SystemCredentials.lookup( Eucalyptus.class ).getCertificate( );
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      throw e;
    }
    final ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
    final ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream( byteOut );
    ZipArchiveEntry entry = null;
    zipOut.setComment( "To setup the environment run: source /path/to/eucarc" );
    StringBuilder sb = new StringBuilder( );
    //TODO:GRZE:FIXME velocity
    final String userNumber = u.getAccount( ).getAccountNumber( );
    sb.append( "EUCA_KEY_DIR=$(cd $(dirname ${BASH_SOURCE:-$0}); pwd -P)" );
    final Optional<String> computeUrl = remotePublicify( Compute.class );
    if ( computeUrl.isPresent( ) ) {
      sb.append( entryFor( "EC2_URL", null, computeUrl ) );
    } else {
      sb.append( "\necho WARN:  Eucalyptus URL is not configured. >&2" );
      ServiceBuilder<? extends ServiceConfiguration> builder = ServiceBuilders.lookup( Compute.class );
      ServiceConfiguration localConfig = builder.newInstance( Internets.localHostAddress( ),
                                                              Internets.localHostAddress( ),
                                                              Internets.localHostAddress( ),
                                                              Eucalyptus.INSTANCE.getPort( ) );
      sb.append( "\nexport EC2_URL=" + ServiceUris.remotePublicify( localConfig ) );
    }

    sb.append( entryFor( "S3_URL", "An OSG is either not registered or not configured. S3_URL is not set. " +
        "Please register an OSG and/or set a valid s3 endpoint and download credentials again. " +
        "Or set S3_URL manually to http://OSG-IP:8773/services/objectstorage",
        remotePublicify( ObjectStorage.class ) ) );
    sb.append( entryFor( "AWS_IAM_URL", "IAM service URL is not configured.",
        remotePublicify( Euare.class ) ) );
    sb.append( entryFor( "EUARE_URL", "EUARE URL is not configured.",
        remotePublicify( Euare.class ) ) );
    sb.append( entryFor( "TOKEN_URL", "TOKEN URL is not configured.",
        remotePublicify( Tokens.class ) ) );
    sb.append( entryFor( "AWS_AUTO_SCALING_URL", "Auto Scaling service URL is not configured.",
        remotePublicify( AutoScaling.class ) ) );
    sb.append( entryFor( "AWS_CLOUDFORMATION_URL", "CloudFormation service URL is not configured.",
        remotePublicify( CloudFormation.class ) ) );
    sb.append( entryFor( "AWS_CLOUDWATCH_URL", "CloudWatch service URL is not configured.",
        remotePublicify( CloudWatch.class ) ) );
    sb.append( entryFor( "AWS_ELB_URL", "Load Balancing service URL is not configured.",
        remotePublicify( LoadBalancing.class ) ) );
    sb.append( entryFor( "AWS_SIMPLEWORKFLOW_URL", null,
        remotePublicify( SimpleWorkflow.class ) ) );
    sb.append( "\nexport EUSTORE_URL=" + StackConfiguration.DEFAULT_EUSTORE_URL );
    String baseName = null;
    if ( x509 != null && keyPair != null ) {
      String fingerPrint = Certs.getFingerPrint( keyPair.getPublic( ) );
      if ( fingerPrint != null ) {
        baseName = X509Download.NAME_SHORT + "-" + u.getName() + "-" + fingerPrint.replaceAll( ":", "" ).toLowerCase().substring( 0, 8 );
        sb.append( "\nexport EC2_PRIVATE_KEY=${EUCA_KEY_DIR}/" + baseName + "-pk.pem" );
        sb.append( "\nexport EC2_CERT=${EUCA_KEY_DIR}/" + baseName + "-cert.pem" );
      }
    }
    sb.append( "\nexport EC2_JVM_ARGS=-Djavax.net.ssl.trustStore=${EUCA_KEY_DIR}/jssecacerts" );
    sb.append( "\nexport EUCALYPTUS_CERT=${EUCA_KEY_DIR}/cloud-cert.pem" );
    sb.append( "\nexport EC2_ACCOUNT_NUMBER='" + u.getAccount( ).getAccountNumber( ) + "'" );
    sb.append( "\nexport EC2_ACCESS_KEY='" + userAccessKey + "'" );
    sb.append( "\nexport EC2_SECRET_KEY='" + userSecretKey + "'" );
    sb.append( "\nexport AWS_ACCESS_KEY='" + userAccessKey + "'" );
    sb.append( "\nexport AWS_SECRET_KEY='" + userSecretKey + "'" );
    sb.append( "\nexport AWS_CREDENTIAL_FILE=${EUCA_KEY_DIR}/iamrc" );
    sb.append( "\nexport EC2_USER_ID='" + userNumber + "'" );
    sb.append( "\nalias ec2-bundle-image=\"ec2-bundle-image --cert ${EC2_CERT} --privatekey ${EC2_PRIVATE_KEY} --user ${EC2_ACCOUNT_NUMBER} --ec2cert ${EUCALYPTUS_CERT}\"" );
    sb.append( "\nalias ec2-upload-bundle=\"ec2-upload-bundle -a ${EC2_ACCESS_KEY} -s ${EC2_SECRET_KEY} --url ${S3_URL}\"" );
    sb.append( "\n" );
    zipOut.putArchiveEntry( entry = new ZipArchiveEntry( "eucarc" ) );
    entry.setUnixMode( 0600 );
    zipOut.write( sb.toString( ).getBytes( StandardCharsets.UTF_8 ) );
    zipOut.closeArchiveEntry();

    sb = new StringBuilder( );
    sb.append( "AWSAccessKeyId=" ).append( userAccessKey ).append( '\n' );
    sb.append( "AWSSecretKey=" ).append( userSecretKey );
    zipOut.putArchiveEntry( entry = new ZipArchiveEntry( "iamrc" ) );
    entry.setUnixMode( 0600 );
    zipOut.write( sb.toString( ).getBytes( StandardCharsets.UTF_8 ) );
    zipOut.closeArchiveEntry( );

    /** write the private key to the zip stream **/
    zipOut.putArchiveEntry( entry = new ZipArchiveEntry( "cloud-cert.pem" ) );
    entry.setUnixMode( 0600 );
    zipOut.write( PEMFiles.getBytes( cloudCert ) );
    zipOut.closeArchiveEntry( );

    zipOut.putArchiveEntry( entry = new ZipArchiveEntry( "jssecacerts" ) );
    entry.setUnixMode( 0600 );
    KeyStore tempKs = KeyStore.getInstance( "jks" );
    tempKs.load( null );
    tempKs.setCertificateEntry( "eucalyptus", cloudCert );
    ByteArrayOutputStream bos = new ByteArrayOutputStream( );
    tempKs.store( bos, "changeit".toCharArray( ) );
    zipOut.write( bos.toByteArray( ) );
    zipOut.closeArchiveEntry( );

    if ( x509 != null && keyPair != null && baseName != null ) {
      /** write the private key to the zip stream **/
      zipOut.putArchiveEntry( entry = new ZipArchiveEntry( baseName + "-pk.pem" ) );
      entry.setUnixMode( 0600 );
      zipOut.write( PEMFiles.getBytes(
          "RSA PRIVATE KEY",
          Crypto.getCertificateProvider().getEncoded( keyPair.getPrivate() )
      ) );
      zipOut.closeArchiveEntry();

      /** write the X509 certificate to the zip stream **/
      zipOut.putArchiveEntry( entry = new ZipArchiveEntry( baseName + "-cert.pem" ) );
      entry.setUnixMode( 0600 );
      zipOut.write( PEMFiles.getBytes( x509 ) );
      zipOut.closeArchiveEntry();
    }
    /** close the zip output stream and return the bytes **/
    zipOut.close( );
    return byteOut.toByteArray( );
  }

  private static Optional<String> remotePublicify( final Class<? extends ComponentId> componentClass ) {
    Optional<String> url = Optional.absent( );
    if ( Topology.isEnabled( componentClass ) ) try {
      url = Optional.of( portUpdate( hostMap( ServiceUris.remotePublicify( Topology.lookup( componentClass ) ) ) ).toString() );
    } catch ( final Exception e ) {
      LOG.error( "Failed to get URL for service " + componentClass.getSimpleName( ), e );
    }
    return url;
  }

  @SuppressWarnings( "ConstantConditions" )
  private static URI hostMap( final URI uri ) {
    final Optional<Cidr> hostMatcher = InetAddresses.isInetAddress( uri.getHost( ) ) ?
        Cidr.parse( ).apply( AuthenticationProperties.CREDENTIAL_DOWNLOAD_HOST_MATCH ) :
        Optional.<Cidr>absent( );
    if ( hostMatcher.isPresent( ) ) {
      final Host host = Hosts.lookup( InetAddresses.forString( uri.getHost( ) ) );
      if ( host != null ) {
        final Optional<InetAddress> mappedHost = Iterables.tryFind( host.getHostAddresses( ), hostMatcher.get( ) );
        if ( mappedHost.isPresent( ) ) {
          return URI.create( uri.toString( ).replaceFirst( uri.getHost( ), mappedHost.get( ).getHostAddress( ) ) );
        }
      }
    }
    return uri;
  }

  private static URI portUpdate( final URI uri ) {
    int port = Objects.firstNonNull(
        AuthenticationProperties.CREDENTIAL_DOWNLOAD_PORT == null ?
            null :
            Ints.tryParse( AuthenticationProperties.CREDENTIAL_DOWNLOAD_PORT ),
        StackConfiguration.PORT );
    try {
      return port != uri.getPort( ) ?
        new URI( uri.getScheme( ), uri.getUserInfo( ), uri.getHost( ), port, uri.getPath( ), uri.getQuery( ), uri.getFragment( ) ) :
        uri;
    } catch ( URISyntaxException e ) {
      return uri;
    }
  }

  private static String entryFor( final String variable,
                                  final String warning,
                                  final Optional<String> url ) {
    if ( url.isPresent( ) ) {
      return "\nexport " + variable + "=" + url.get( );
    } else if ( warning != null ) {
      return "\necho WARN:  " + warning + " >&2";
    } else {
      return "";
    }
  }

  private static String getError( String message ) {
    StringBuilder builder = new StringBuilder( );
    builder.append( "Getting credentials failed:\n" );
    builder.append( message );
    return builder.toString( );
  }
  
}
