/*
 * Software License Agreement (BSD License)
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * * Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the
 * following disclaimer.
 * * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other
 * materials provided with the distribution.
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
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.server;

import edu.ucsb.eucalyptus.admin.client.UserInfoWeb;

import com.eucalyptus.auth.Credentials;
import com.eucalyptus.auth.User;
import com.eucalyptus.auth.X509Cert;
import com.eucalyptus.auth.util.KeyTool;
import com.eucalyptus.util.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.cloud.entities.CertificateInfo;
import edu.ucsb.eucalyptus.cloud.entities.UserInfo;
import edu.ucsb.eucalyptus.keys.AbstractKeyStore;
import edu.ucsb.eucalyptus.keys.Hashes;
import edu.ucsb.eucalyptus.keys.UserKeyStore;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.UrlBase64;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class X509Download extends HttpServlet {

  private static Logger LOG                = Logger.getLogger( X509Download.class );

  public static String  PARAMETER_USERNAME = "user";
  public static String  PARAMETER_KEYNAME  = "keyName";
  public static String  PARAMETER_CODE     = "code";

  public void doGet( HttpServletRequest request, HttpServletResponse response ) {
    String code = request.getParameter( PARAMETER_CODE );
    String userName = request.getParameter( PARAMETER_USERNAME );
    String keyName = request.getParameter( PARAMETER_KEYNAME );
    String mimetype = "application/zip";
    Calendar now = Calendar.getInstance( );
    keyName = ( keyName == null || "".equals( keyName ) ) ? "default" : keyName;
    keyName = userName + String.format( "-%1$ty%1$tm%1$te%1$tk%1$tM%1$tS", now );
    if ( userName == null || "".equals( userName ) ) {
      hasError( "No user name provided", response );
      return;
    }
    if ( code == null || "".equals( code ) ) {
      hasError( "Wrong confirmation code", response );
      return;
    }

    UserInfoWeb user = null;
    try {
      user = EucalyptusManagement.getWebUser( userName );
      String[] certs = EucalyptusManagement.getUserCertificateAliases( userName );
      for ( String s : certs )
        if ( s.equals( keyName ) ) {
          hasError( "Certificate name already exists", response );
          return;
        }
    } catch ( Exception e ) {
      hasError( "User does not exist", response );
      return;
    }
    if ( !user.getCertificateCode( ).equals( code ) ) {
      hasError( "Confirmation code is invalid", response );
      return;
    }

    response.setContentType( mimetype );
    response.setHeader( "Content-Disposition", "attachment; filename=\"" + EucalyptusProperties.NAME_SHORT + "-" + userName + "-x509.zip\"" );
    LOG.info( "pushing out the X509 certificate for user " + userName );

    try {
      byte[] x509zip = getX509Zip( userName, keyName );
      ServletOutputStream op = response.getOutputStream( );

      response.setContentLength( x509zip.length );

      op.write( x509zip );
      op.flush( );

    } catch ( GeneralSecurityException e ) {
      e.printStackTrace( );
    } catch ( IOException e ) {
      e.printStackTrace( );
    } catch ( EucalyptusCloudException e ) {
      e.printStackTrace( );
    }
  }

  public static void hasError( String message, HttpServletResponse response ) {
    try {
      response.getWriter( ).print( EucalyptusManagement.getError( message ) );
      response.getWriter( ).flush( );
    } catch ( IOException e ) {
      e.printStackTrace( );
    }
  }

  public static byte[] getX509Zip( String userName, String newKeyName ) throws GeneralSecurityException, IOException, EucalyptusCloudException {

    KeyTool keyTool = new KeyTool( );
    KeyPair keyPair = keyTool.getKeyPair( );
    X509Certificate x509 = keyTool.getCertificate( keyPair, EucalyptusProperties.getDName( userName ) );
    X509Certificate cloudCert = null;
    try {
      x509.checkValidity( );
      Credentials.Users.addCertificate( userName, newKeyName, x509 );
      AbstractKeyStore ks = UserKeyStore.getInstance( );
      ks.addCertificate( newKeyName, x509 );
      cloudCert = ks.getCertificate( EucalyptusProperties.NAME );
      ks.store( );
    } catch ( IOException e ) {
      LOG.fatal( e, e );
    } catch ( GeneralSecurityException e ) {
      LOG.fatal( e, e );
    }
    
    String certPem = new String( UrlBase64.encode( Hashes.getPemBytes( x509 ) ) );

    String userAccessKey = Credentials.Users.getQueryId( userName );
    String userSecretKey = Credentials.Users.getSecretKey( userAccessKey );

    ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
    ZipOutputStream zipOut = new ZipOutputStream( byteOut );
    String baseName = EucalyptusProperties.NAME_SHORT + "-" + userName + "-" + Hashes.getFingerPrint( keyPair.getPublic( ) ).replaceAll( ":", "" ).toLowerCase( ).substring( 0, 8 );

    zipOut.setComment( "To setup the environment run: source /path/to/eucarc" );
    StringBuffer sb = new StringBuffer( );

    String userNumber = UserInfo.getUserNumber( userName );

    sb.append( "EUCA_KEY_DIR=$(dirname $(readlink -f ${BASH_SOURCE}))" );
    sb.append( "\nexport S3_URL=" + EucalyptusProperties.getSystemConfiguration( ).getStorageUrl( ) );
    sb.append( "\nexport EC2_URL=" + EucalyptusProperties.getSystemConfiguration( ).getStorageUrl( ).replaceAll( "Walrus", "Eucalyptus" ) );
    sb.append( "\nexport EC2_PRIVATE_KEY=${EUCA_KEY_DIR}/" + baseName + "-pk.pem" );
    sb.append( "\nexport EC2_CERT=${EUCA_KEY_DIR}/" + baseName + "-cert.pem" );
    sb.append( "\nexport EUCALYPTUS_CERT=${EUCA_KEY_DIR}/cloud-cert.pem" );
    sb.append( "\nexport EC2_ACCESS_KEY='" + userAccessKey + "'" );
    sb.append( "\nexport EC2_SECRET_KEY='" + userSecretKey + "'" );
    sb.append( "\nalias ec2-bundle-image=\"ec2-bundle-image --cert ${EC2_CERT} --privatekey ${EC2_PRIVATE_KEY} --user " + userNumber + " --ec2cert ${EUCALYPTUS_CERT}\"" );
    sb.append( "\nalias ec2-upload-bundle=\"ec2-upload-bundle -a ${EC2_ACCESS_KEY} -s ${EC2_SECRET_KEY} --url ${S3_URL} --ec2cert ${EUCALYPTUS_CERT}\"" );
    sb.append( "\n" );
    zipOut.putNextEntry( new ZipEntry( "eucarc" ) );
    zipOut.write( sb.toString( ).getBytes( ) );
    zipOut.closeEntry( );

    /** write the private key to the zip stream **/
    zipOut.putNextEntry( new ZipEntry( "cloud-cert.pem" ) );
    zipOut.write( Hashes.getPemBytes( cloudCert ) );
    zipOut.closeEntry( );

    /** write the private key to the zip stream **/
    zipOut.putNextEntry( new ZipEntry( baseName + "-pk.pem" ) );
    zipOut.write( Hashes.getPemBytes( keyPair.getPrivate( ) ) );
    zipOut.closeEntry( );

    /** write the X509 certificate to the zip stream **/
    zipOut.putNextEntry( new ZipEntry( baseName + "-cert.pem" ) );
    zipOut.write( Hashes.getPemBytes( x509 ) );
    zipOut.closeEntry( );

    /** close the zip output stream and return the bytes **/
    zipOut.close( );
    return byteOut.toByteArray( );
  }

}
