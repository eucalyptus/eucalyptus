/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.auth.euare;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.ServerCertificate;
import com.eucalyptus.auth.euare.persist.entities.ServerCertificateEntity;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.google.common.base.Charsets;
import com.google.common.base.Function;

/**
 * @author Sang-Min Park
 * 
 */
public class ServerCertificates {
  private static Logger LOG = Logger.getLogger( ServerCertificates.class );

  public static final class VerifiedCertInfo {
    private final Date expiration;


    public VerifiedCertInfo( final Date expiration ) {
      this.expiration = expiration;
    }

    @Nullable
    public Date getExpiration( ) {
      return expiration;
    }
  }

  @Nonnull
  public static VerifiedCertInfo verifyCertificate(final String certBody, final String certChain)
      throws AuthException{
    final Date expiration;
    try{
      final X509Certificate cert = PEMFiles.getCert(certBody.getBytes( Charsets.UTF_8 ));
      if(cert==null)
        throw new Exception("Malformed certificate");
      expiration = cert.getNotAfter( );
    }catch(final Exception ex) {
      throw new AuthException(
          String.format("%s (%s)", AuthException.SERVER_CERT_INVALID_FORMAT,
              "Certificate body is invalid - is the cert in PEM format?"));
    }
    return new VerifiedCertInfo( expiration );
  }


  @Nonnull
  public static VerifiedCertInfo verifyCertificate(final String certBody, final String pk, final String certChain)
      throws AuthException{
    final VerifiedCertInfo certInfo = verifyCertificate( certBody, certChain );
    try{
      final KeyPair kp = PEMFiles.getKeyPair(pk.getBytes( Charsets.UTF_8 ));
      if(kp == null)
        throw new Exception("Malformed pk");
    }catch(final Exception ex){
      LOG.error("Invalid private key is given", ex);
      throw new AuthException(
          String.format("%s (%s)", AuthException.SERVER_CERT_INVALID_FORMAT,
          "Private key is invalid - is the key in PEM format?"));
    }
    return certInfo;
  }

  @Resolver( ServerCertificateEntity.class )
  public enum Lookup implements Function<String, ServerCertificateEntity> {
    INSTANCE;
    @Override
    public ServerCertificateEntity apply( final String arn ) {
      try{
        //String.format("arn:aws:iam::%s:server-certificate%s%s", this.owningAccount.getAccountNumber(), path, this.certName);
        // extract account id from arn and find account
        if(!arn.startsWith("arn:aws:iam::"))
          throw new EucalyptusCloudException("malformed arn");
        
        // get admin name of the account
        String token = arn.substring( "arn:aws:iam::".length() );
        String acctId = token.substring( 0, token.indexOf( ":server-certificate" ) );

        // get certname of the arn
        final String prefix = 
            String.format("arn:aws:iam::%s:server-certificate", acctId);
        if(!arn.startsWith(prefix))
          throw new EucalyptusCloudException("malformed arn");
        String pathAndName = arn.replace(prefix, "");
        String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
        
        ServerCertificateEntity found = null;
        try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
          found = Entities.criteriaQuery(
              ServerCertificateEntity.named( AccountFullName.getInstance( acctId ), certName )
          ).uniqueResult( );
          db.rollback();
        } catch(final NoSuchElementException ex){
          ;
        } catch(final Exception ex){
          throw ex;
        }
        return found;
      }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
      }
    }
  }
  
  public enum ToServerCertificate implements
      Function<ServerCertificateEntity, ServerCertificate> {
    INSTANCE;
    @Override
    public ServerCertificate apply(ServerCertificateEntity entity) {
      return toServerCertificate( entity, false );
    }
  }

  public enum ToServerCertificateWithSecrets implements
      Function<ServerCertificateEntity, ServerCertificate> {
    INSTANCE;
    @Override
    public ServerCertificate apply(ServerCertificateEntity entity) {
      return toServerCertificate( entity, true );
    }
  }

  /// TODO: should think about impact to services using the certificate
  public static void updateServerCertificate(final OwnerFullName user,
      final String certName, final String newCertName, final String newCertPath)
      throws NoSuchElementException, AuthException {
    try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
      final ServerCertificateEntity found = Entities.criteriaQuery(
          ServerCertificateEntity.named( user, certName )
      ).uniqueResult( );
      try {
        if (newCertName != null && newCertName.length() > 0
            && !certName.equals(newCertName))
          found.setCertName(newCertName);
      } catch (final Exception ex) {
        throw new AuthException(AuthException.INVALID_SERVER_CERT_NAME);
      }
      try {
        if (newCertPath != null && newCertPath.length() > 0)
          found.setCertPath(newCertPath);
      } catch (final Exception ex) {
        throw new AuthException(AuthException.INVALID_SERVER_CERT_PATH);
      }
      Entities.persist(found);
      db.commit();
    } catch (final Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }  
  
  private static ServerCertificate toServerCertificate( 
      final ServerCertificateEntity entity, 
      final boolean includeSecrets
  ) {
    try {
      final ServerCertificate cert = new ServerCertificate(
          entity.getOwnerAccountNumber( ),
          entity.getCertName( ), 
          entity.getCreationTimestamp( ),
          entity.getExpiration( )
      );
      cert.setCertificatePath(entity.getCertPath());
      cert.setCertificateBody(entity.getCertBody());
      cert.setCertificateChain(entity.getCertChain());
      cert.setCertificateId(entity.getCertId());

      if ( includeSecrets ) {
        byte[] encText = Base64.decode( entity.getPrivateKey( ) );
        byte[] iv = Arrays.copyOfRange( encText, 0, 32 );
        byte[] encPk = Arrays.copyOfRange( encText, 32, encText.length );

        byte[] symKeyWrapped = Base64.decode( entity.getSessionKey( ) );
        // get session key
        final PrivateKey euarePk = SystemCredentials.lookup( Euare.class )
            .getPrivateKey( );
        Cipher cipher = Ciphers.RSA_PKCS1.get( );
        cipher.init( Cipher.UNWRAP_MODE, euarePk, Crypto.getSecureRandomSupplier( ).get( ) );
        SecretKey sessionKey = (SecretKey) cipher.unwrap( symKeyWrapped,
            "AES/GCM/NoPadding", Cipher.SECRET_KEY );
        cipher = Ciphers.AES_GCM.get( );
        cipher.init( Cipher.DECRYPT_MODE, sessionKey, new IvParameterSpec( iv ), Crypto.getSecureRandomSupplier( ).get( ) );
        cert.setPrivateKey( new String( cipher.doFinal( encPk ) ) );
      }
      return cert;
    } catch (final Exception ex) {
      throw Exceptions.toUndeclared(ex);
    }
  }
}
