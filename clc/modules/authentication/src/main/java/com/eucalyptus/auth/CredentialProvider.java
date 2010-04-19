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
/**
 * 
 */
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth;

import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.Adler32;
import org.bouncycastle.util.encoders.UrlBase64;
import org.hibernate.Session;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import com.eucalyptus.auth.util.Hashes;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Depends;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.Resource;
import com.eucalyptus.entities.DatabaseUtil;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;

@Provides( resource = Resource.UserCredentials )
@Depends( resources = { Resource.Database } )
public class CredentialProvider extends Bootstrapper {
  public static boolean hasCertificate( final String alias ) {
    X509Cert certInfo = null;
    EntityWrapper<X509Cert> db = Credentials.getEntityWrapper( );
    try {
      certInfo = db.getUnique( new X509Cert( alias ) );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
    }
    return certInfo != null;
  }
  
  public static X509Certificate getCertificate( final String alias ) throws GeneralSecurityException {
    EntityWrapper<X509Cert> db = Credentials.getEntityWrapper( );
    try {
      X509Cert certInfo = db.getUnique( new X509Cert( alias ) );
      String certString = certInfo.getPemCertificate();
      if(certString != null) {
          byte[] certBytes = UrlBase64.decode(certString.getBytes( ) );
          X509Certificate x509 = Hashes.getPemCert( certBytes );
          db.commit( );
          return x509;
      }
      return null;
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new GeneralSecurityException( e );
    }
  }
  
  public static String getDName( final String name ) {
    return String.format( "CN=localhost, OU=Eucalyptus, O=%s, L=Santa Barbara, ST=CA, C=US", name );
  }

  
  public static String getCertificateAlias( final String certPem ) throws GeneralSecurityException {
    String certAlias = null;
    EntityWrapper<X509Cert> db = Credentials.getEntityWrapper( );
    X509Cert certInfo = new X509Cert( );
    certInfo.setPemCertificate( new String( UrlBase64.encode( certPem.getBytes( ) ) ) );
    try {
      certAlias = db.getUnique( certInfo ).getAlias( );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new GeneralSecurityException( e );
    }
    return certAlias;
  }
  
  public static String getQueryId( String userName ) throws GeneralSecurityException {
    String queryId = null;
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    User searchUser = new User( userName, true );
    try {
      User user = db.getUnique( searchUser );
      queryId = user.getQueryId( );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new GeneralSecurityException( e );
    }
    return queryId;
  }
  
  public static String getSecretKey( String queryId ) throws GeneralSecurityException {
    String secretKey = null;
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    User searchUser = new User( );
    searchUser.setQueryId( queryId );
    searchUser.setIsEnabled( true );
    try {
      User user = db.getUnique( searchUser );
      secretKey = user.getSecretKey( );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new GeneralSecurityException( e );
    }
    return secretKey;
  }
  
  public static String getUserName( String queryId ) throws GeneralSecurityException {
    String userName = null;
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    User searchUser = new User( );
    searchUser.setQueryId( queryId );
    try {
      User user = db.getUnique( searchUser );
      userName = user.getUserName( );
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
      throw new GeneralSecurityException( e );
    }
    return userName;
  }
  
  public static String getUserName( X509Certificate cert ) throws GeneralSecurityException {
    return getUser( cert ).getUserName( );
  }

  @SuppressWarnings( "unchecked" )
  public static User getUser( X509Certificate cert ) throws GeneralSecurityException {
    String certPem = new String( UrlBase64.encode( Hashes.getPemBytes( cert ) ) );
    User searchUser = new User( );
    X509Cert searchCert = new X509Cert( );
    searchCert.setPemCertificate( certPem );
    searchUser.setIsEnabled( true );
    Session session = DatabaseUtil.getEntityManagerFactory( Credentials.DB_NAME ).getSessionFactory( ).openSession( );
    try {
      Example qbeUser = Example.create( searchUser ).enableLike( MatchMode.EXACT );
      Example qbeCert = Example.create( searchCert ).enableLike( MatchMode.EXACT );
      List<User> users = ( List<User> ) session.createCriteria( User.class ).setCacheable( true ).add( qbeUser )
                                               .createCriteria( "certificates" ).setCacheable( true ).add( qbeCert )
                                               .list( );
      User ret = users.size()==1?users.get(0):null;
      int size = users.size();
      if( ret != null ) {
        return ret;
      } else {
        throw new GeneralSecurityException( (size == 0)?"No user with the specified certificate.":"Multiple users with the same certificate." );
      }
    } catch ( Throwable t ) {
      throw new GeneralSecurityException( t );
    } finally {
      try {
        session.close( );
      } catch ( Throwable t ) {
      }
    }
  }
  
  public static String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException {
    return getCertificateAlias( new String( Hashes.getPemBytes( cert ) ) );
  }
  
  public static void addCertificate( final String userName, final String alias, final X509Certificate cert ) throws GeneralSecurityException {
    String certPem = new String( UrlBase64.encode( Hashes.getPemBytes( cert ) ) );
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    User u = null;
    try {
      u = db.getUnique( new User( userName ) );
      X509Cert x509cert = new X509Cert( alias );
      x509cert.setPemCertificate( certPem );
      u.getCertificates( ).add( x509cert );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      Credentials.LOG.error( e, e );
      Credentials.LOG.error( "username=" + userName + " \nalias=" + alias + " \ncert=" + cert );
      db.rollback( );
      throw new GeneralSecurityException( e );
    }
  }
  
  public static List<String> getAliases( ) {
    EntityWrapper<X509Cert> db = Credentials.getEntityWrapper( );
    List<String> certAliases = Lists.newArrayList( );
    try {
      List<X509Cert> certList = db.query( new X509Cert( ) );
      for ( X509Cert cert : certList ) {
        certAliases.add( cert.getAlias( ) );
      }
      db.commit( );
    } catch ( Throwable e ) {
      db.rollback( );
    }
    return certAliases;
  }
  
  @Override
  public boolean load( Resource current ) throws Exception {
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    return Credentials.checkAdmin( );
  }
  
  public static String getUserNumber( final String userName ) {
    Adler32 hash = new Adler32( );
    hash.reset( );
    hash.update( userName.getBytes( ) );
    String userNumber = String.format( "%012d", hash.getValue( ) );
    return userNumber;
  }
  
  public static User getUser( String userName ) throws NoSuchUserException {
    User user = null;
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    User searchUser = new User( userName, true );
    try {
      user = db.getUnique( searchUser );
      db.commit( );
    } catch ( EucalyptusCloudException e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
    return user;
  }
  
  public static User getUserFromQueryId( String queryId ) throws NoSuchUserException {
	User user = null;
	EntityWrapper<User> db = Credentials.getEntityWrapper( );
	User searchUser = new User( );
	searchUser.setQueryId( queryId );
	try {
	  user = db.getUnique( searchUser );
	  db.commit( );
	} catch ( EucalyptusCloudException e ) {
	  db.rollback( );
	  throw new NoSuchUserException( e );
	}
	return user;
   }

  public static List<User> getEnabledUsers( ) {
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    try {
      return db.query( new User( null, true ) );
    } finally {
      db.commit( );
    }
  }

  public static List<User> getAllUsers( ) {
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    try {
      return db.query( new User( null ) );
    } finally {
      db.commit( );
    }
  }

  
  public static User addUser( String userName, Boolean isAdmin, String queryId, String secretKey ) throws UserExistsException {
    return CredentialProvider.addUser( userName, isAdmin, queryId, secretKey, true );
  }
  public static User addUser( String userName, Boolean isAdmin, String queryId, String secretKey, Boolean isEnabled ) throws UserExistsException {
    User newUser = new User( userName);
    newUser.setQueryId( queryId );
    newUser.setSecretKey( secretKey );
    newUser.setIsAdministrator( isAdmin );
    newUser.setIsEnabled( isEnabled );
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    try {
      db.add( newUser );
      db.commit( );
    } catch ( Throwable t ) {
      db.rollback( );
      throw new UserExistsException( t );
    }
    return newUser;
  }

  public static User addDisabledUser( String userName, Boolean isAdmin ) throws UserExistsException {
    String queryId = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, false ).replaceAll( "\\p{Punct}", "" );
    String secretKey = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, true ).replaceAll( "\\p{Punct}", "" );
    return CredentialProvider.addUser( userName, isAdmin, queryId, secretKey, false );
  }
  public static User addUser( String userName, Boolean isAdmin ) throws UserExistsException {
    String queryId = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, false ).replaceAll( "\\p{Punct}", "" );
    String secretKey = Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, true ).replaceAll( "\\p{Punct}", "" );
    return CredentialProvider.addUser( userName, isAdmin, queryId, secretKey );
  }
  
  public static User addUser( String userName ) throws UserExistsException {
    return CredentialProvider.addUser( userName, false );
  }
  
  public static void deleteUser( String userName ) throws NoSuchUserException {
    User user = new User( userName );
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    try {
      User foundUser = db.getUnique( user );
      db.delete( foundUser );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
  }
  
  public static void updateUser( String userName, Boolean isEnabled ) throws NoSuchUserException {
    User user = new User( userName );
    EntityWrapper<User> db = Credentials.getEntityWrapper( );
    try {
      User foundUser = db.getUnique( user );
      foundUser.setIsEnabled( isEnabled );
      db.commit( );
    } catch ( Exception e ) {
      db.rollback( );
      throw new NoSuchUserException( e );
    }
  }
  
  public static String generateConfirmationCode( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.SHA512, true ).replaceAll("\\p{Punct}", "" );
  }

  public static String generateCertificateCode( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.SHA512, true ).replaceAll("\\p{Punct}", "" );
  }

  public static String generateSecretKey( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.SHA224, true ).replaceAll("\\p{Punct}", "" );
  }

  public static String generateQueryId( String userName ) {
    return Hashes.getDigestBase64( userName, Hashes.Digest.MD5, false ).replaceAll("\\p{Punct}", "" );
  }

}
