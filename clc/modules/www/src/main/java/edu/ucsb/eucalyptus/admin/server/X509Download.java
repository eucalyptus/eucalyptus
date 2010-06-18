/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 */

package edu.ucsb.eucalyptus.admin.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.SystemCredentialProvider;
import com.eucalyptus.auth.Users;
import com.eucalyptus.auth.crypto.Certs;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.bootstrap.Component;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;

public class X509Download extends HttpServlet {
  
  private static Logger LOG                = Logger.getLogger( X509Download.class );
  public static String  NAME_SHORT         = "euca2";
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
    
    User user = null;
    try {
      user = Users.lookupUser( userName );
    } catch ( Exception e ) {
      hasError( "User does not exist", response );
      return;
    }
    if ( !user.checkToken( code ) ) {
      hasError( "Confirmation code is invalid", response );
      return;
    }
    response.setContentType( mimetype );
    response.setHeader( "Content-Disposition", "attachment; filename=\"" + X509Download.NAME_SHORT + "-" + userName + "-x509.zip\"" );
    LOG.info( "pushing out the X509 certificate for user " + userName );
    
    try {
      byte[] x509zip = getX509Zip( userName, keyName );
      ServletOutputStream op = response.getOutputStream( );
      
      response.setContentLength( x509zip.length );
      
      op.write( x509zip );
      op.flush( );
      
    } catch ( Throwable e ) {
      LOG.error( e, e );
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
  
  public static byte[] getX509Zip( String userName, String newKeyName ) throws Exception {
    X509Certificate cloudCert = null;
    final X509Certificate x509;
    User u = Users.lookupUser( userName );
    String userAccessKey = u.getQueryId( );
    String userSecretKey = u.getSecretKey( );
    KeyPair keyPair = null;
    try {
      keyPair = Certs.generateKeyPair( );
      x509 = Certs.generateCertificate( keyPair, userName );
      x509.checkValidity( );
      cloudCert = SystemCredentialProvider.getCredentialProvider( Component.eucalyptus ).getCertificate( );
      u.revokeX509Certificate( );
      u.setX509Certificate( x509 );
      //      Transactions.one( new UserEntity( userName ), new Tx<UserEntity>() {
      //        public void fire( UserEntity user ) throws Throwable {
      //          user.revokeX509Certificate( );
      //          user.setCertificate( B64.url.encString( PEMFiles.getBytes( x509 ) ) );
      //        }});
      User now = Users.lookupUser( userName );
      LOG.info( "Current user certificate: " + now.getX509Certificate( ) != null ? now.getX509Certificate( ).getSerialNumber( ) : null );
    } catch ( Exception e ) {
      LOG.fatal( e, e );
      throw e;
    }
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream( );
    ZipOutputStream zipOut = new ZipOutputStream( byteOut );
    String fingerPrint = Certs.getFingerPrint( keyPair.getPublic( ) );
    if ( fingerPrint != null ) {
      String baseName = X509Download.NAME_SHORT + "-" + userName + "-" + fingerPrint.replaceAll( ":", "" ).toLowerCase( ).substring( 0, 8 );
      
      zipOut.setComment( "To setup the environment run: source /path/to/eucarc" );
      StringBuffer sb = new StringBuffer( );
      
      BigInteger number = Users.lookupUser( userName ).getNumber( );
      String userNumber = null;
      if ( number != null ) {
	    userNumber = number.toString( );
      }
      sb.append( "EUCA_KEY_DIR=$(dirname $(readlink -f ${BASH_SOURCE}))" );
      
      try {
        sb.append( "\nexport S3_URL=" + SystemConfiguration.getWalrusUrl( ) );
      } catch ( Exception e ) {
        sb.append( "\necho WARN:  Walrus URL is not configured." );
      }
      sb.append( "\nexport EC2_URL=" + SystemConfiguration.getCloudUrl( ) );
      sb.append( "\nexport EC2_PRIVATE_KEY=${EUCA_KEY_DIR}/" + baseName + "-pk.pem" );
      sb.append( "\nexport EC2_CERT=${EUCA_KEY_DIR}/" + baseName + "-cert.pem" );
      sb.append( "\nexport EC2_JVM_ARGS=-Djavax.net.ssl.trustStore=${EUCA_KEY_DIR}/jssecacerts" );
      sb.append( "\nexport EUCALYPTUS_CERT=${EUCA_KEY_DIR}/cloud-cert.pem" );
      sb.append( "\nexport EC2_ACCESS_KEY='" + userAccessKey + "'" );
      sb.append( "\nexport EC2_SECRET_KEY='" + userSecretKey + "'" );
      if ( userNumber != null ) {
        sb.append( "\n# This is a bogus value; Eucalyptus does not need this but client tools do.\nexport EC2_USER_ID='" + userNumber + "'" );
        sb.append( "\nalias ec2-bundle-image=\"ec2-bundle-image --cert ${EC2_CERT} --privatekey ${EC2_PRIVATE_KEY} --user " + userNumber
                 + " --ec2cert ${EUCALYPTUS_CERT}\"" );
      }
      sb.append( "\nalias ec2-upload-bundle=\"ec2-upload-bundle -a ${EC2_ACCESS_KEY} -s ${EC2_SECRET_KEY} --url ${S3_URL} --ec2cert ${EUCALYPTUS_CERT}\"" );
      sb.append( "\n" );
      zipOut.putNextEntry( new ZipEntry( "eucarc" ) );
      zipOut.write( sb.toString( ).getBytes( ) );
      zipOut.closeEntry( );
      
      /** write the private key to the zip stream **/
      zipOut.putNextEntry( new ZipEntry( "cloud-cert.pem" ) );
      zipOut.write( PEMFiles.getBytes( cloudCert ) );
      zipOut.closeEntry( );
      
      zipOut.putNextEntry( new ZipEntry( "jssecacerts" ) );
      KeyStore tempKs = KeyStore.getInstance( "jks" );
      tempKs.load( null );
      tempKs.setCertificateEntry( "eucalyptus", cloudCert );
      ByteArrayOutputStream bos = new ByteArrayOutputStream( );
      tempKs.store( bos, "changeit".toCharArray( ) );
      zipOut.write( bos.toByteArray( ) );
      zipOut.closeEntry( );
      
      /** write the private key to the zip stream **/
      zipOut.putNextEntry( new ZipEntry( baseName + "-pk.pem" ) );
      zipOut.write( PEMFiles.getBytes( keyPair.getPrivate( ) ) );
      zipOut.closeEntry( );
      
      /** write the X509 certificate to the zip stream **/
      zipOut.putNextEntry( new ZipEntry( baseName + "-cert.pem" ) );
      zipOut.write( PEMFiles.getBytes( x509 ) );
      zipOut.closeEntry( );
    }
    /** close the zip output stream and return the bytes **/
    zipOut.close( );
    return byteOut.toByteArray( );
  }
  
}
