/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 ************************************************************************/
package com.eucalyptus.auth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.NoSuchElementException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.entities.ServerCertificateEntity;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.RestrictedTypes.Resolver;
import com.google.common.base.Charsets;
import com.google.common.base.Function;

/**
 * @author Sang-Min Park
 * 
 */
public class ServerCertificates {
  
  public static boolean isCertValid(final String certBody, final String pk, final String certChain){
    try{
      final X509Certificate cert = PEMFiles.getCert(certBody.getBytes( Charsets.UTF_8 ));
      if(cert==null)
        throw new EucalyptusCloudException("Malformed cert");
      
      final KeyPair kp = PEMFiles.getKeyPair(pk.getBytes( Charsets.UTF_8 ));
      if(kp == null)
        throw new EucalyptusCloudException("Malformed pk");
    }catch(final Exception ex){
      return false;
    }
    return true;
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
        String token = arn.substring("arn:aws:iam::".length());
        String acctId = token.substring(0, token.indexOf(":server-certificate"));
        final Account acct = Accounts.lookupAccountById(acctId);
        final User adminUser = acct.lookupAdmin();
        
        // get certname of the arn
        final String prefix = 
            String.format("arn:aws:iam::%s:server-certificate", acctId);
        if(!arn.startsWith(prefix))
          throw new EucalyptusCloudException("malformed arn");
        String pathAndName = arn.replace(prefix, "");
        String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
        
        ServerCertificateEntity found = null;
        try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
          found = 
              Entities.uniqueResult(ServerCertificateEntity.named(UserFullName.getInstance(adminUser), certName));
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
      try {
        final ServerCertificate cert = new ServerCertificate(
            Accounts.lookupAccountById(entity.getOwnerAccountNumber()),
            entity.getCertName(), entity.getCreationTimestamp());
        cert.setCertificatePath(entity.getCertPath());
        cert.setCertificateBody(entity.getCertBody());
        cert.setCertificateChain(entity.getCertChain());
        cert.setCertificateId(entity.getCertId());

        byte[] encText = Base64.decode(entity.getPrivateKey());
        byte[] iv = Arrays.copyOfRange(encText, 0, 32);
        byte[] encPk = Arrays.copyOfRange(encText, 32, encText.length);

        byte[] symKeyWrapped = Base64.decode(entity.getSessionKey());
        // get session key
        final PrivateKey euarePk = SystemCredentials.lookup(Euare.class)
            .getPrivateKey();
        Cipher cipher = Ciphers.RSA_PKCS1.get();
        cipher.init(Cipher.UNWRAP_MODE, euarePk);
        SecretKey sessionKey = (SecretKey) cipher.unwrap(symKeyWrapped,
            "AES/GCM/NoPadding", Cipher.SECRET_KEY);
        cipher = Ciphers.AES_GCM.get();
        cipher.init(Cipher.DECRYPT_MODE, sessionKey, new IvParameterSpec(iv));
        cert.setPrivateKey(new String(cipher.doFinal(encPk)));
        return cert;
      } catch (final Exception ex) {
        throw Exceptions.toUndeclared(ex);
      }
    }
  }

  /// TODO: should think about impact to services using the certificate
  public static void updateServerCertificate(final OwnerFullName user,
      final String certName, final String newCertName, final String newCertPath)
      throws NoSuchElementException, AuthException {
    try ( final TransactionResource db = Entities.transactionFor( ServerCertificateEntity.class ) ) {
      final ServerCertificateEntity found = Entities.uniqueResult(ServerCertificateEntity.named(user, certName));
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
}
