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
 ************************************************************************/
package com.eucalyptus.auth.euare;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Base64;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.ServerCertificate;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Euare;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.crypto.Digest;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.util.Exceptions;

/**
 * @author Sang-Min Park
 *
 */
public class EuareServerCertificateUtil {
  private static Logger    LOG     = Logger.getLogger( EuareServerCertificateUtil.class );

  // return body and chain of server certificate in plain text
  public static String getServerCertificate(final String certArn) throws AuthException {
    final ServerCertificate targetCert = lookupServerCertificate(certArn);
    String serverCert = targetCert.getCertificateBody();
    final String chain = targetCert.getCertificateChain();
    if(chain != null && chain.length()>0)
      serverCert = String.format("%s\n%s", serverCert, chain);
    
    return serverCert; 
  }
  
  public static String getEncryptedKey(final String certArn, final String certPem) throws AuthException {
    final ServerCertificate targetCert = lookupServerCertificate(certArn); 
    // generate symmetric key
    final MessageDigest digest = Digest.SHA256.get();
    final byte[] salt = new byte[32];
    Crypto.getSecureRandomSupplier().get().nextBytes(salt);
    digest.update( salt );
    final SecretKey symmKey = new SecretKeySpec( digest.digest(), "AES" );
    
    try{
      // encrypt the server pk using symm key
      Cipher cipher = Ciphers.AES_CBC.get();
      final byte[] iv = new byte[16];
      Crypto.getSecureRandomSupplier().get().nextBytes(iv);
      cipher.init( Cipher.ENCRYPT_MODE, symmKey, new IvParameterSpec( iv ) );
      final byte[] cipherText = cipher.doFinal(Base64.encode( targetCert.getPrivateKey().getBytes() ));
      final String encPrivKey = new String(Base64.encode(Arrays.concatenate(iv, cipherText)));

      // encrypt the symmetric key using the certPem
      X509Certificate x509Cert = PEMFiles.getCert( B64.standard.dec( certPem ) );
      cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.ENCRYPT_MODE, x509Cert.getPublicKey());
      byte[] symmkey = cipher.doFinal(symmKey.getEncoded());
      final String b64SymKey = new String(Base64.encode(symmkey));
      
      return String.format("%s\n%s", b64SymKey, encPrivKey);
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }
  
  public static String generateSignatureWithEuare(final String msg){
    return generateSignature(SystemCredentials.lookup( Euare.class ).getPrivateKey( ), msg);
  }
  
  public static String generateSignature(final PrivateKey key, final String msg){
    try{
      final Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initSign(key);
      sig.update(msg.getBytes("UTF-8"));
      final byte[] bsig = sig.sign();
      return B64.standard.encString(bsig);
    }catch(final Exception ex){
        throw Exceptions.toUndeclared(ex);
    }
  }
  
  public static boolean verifySignature(final String certPem, final String msg, final String sigB64){
    try{
      final Signature sig = Signature.getInstance("SHA256withRSA");
      final X509Certificate cert = PEMFiles.getCert( B64.standard.dec( certPem ) );
      sig.initVerify( cert );
      sig.update(msg.getBytes("UTF-8"));
      return sig.verify(B64.standard.dec(sigB64.getBytes()));
    }catch(final Exception ex){
      throw Exceptions.toUndeclared(ex);
    }
  }
 
  public static boolean verifySignatureWithEuare(final String msg, final String sigB64){
    final String euareCert = 
        B64.standard.encString( PEMFiles.getBytes( SystemCredentials.lookup( Euare.class ).getCertificate() ) );
    return verifySignature(euareCert, msg, sigB64);
  }
  
  private static ServerCertificate lookupServerCertificate(final String certArn) throws AuthException{
    if(!certArn.startsWith("arn:aws:iam::"))
      Exceptions.toUndeclared(new Exception("ARN is not in valid format"));
    
    String arn = certArn;
    arn = arn.replace("arn:aws:iam::", "");
    final int idx = arn.indexOf(":server-certificate");
    if(idx <=0 ){
      Exceptions.toUndeclared(new Exception("ARN is not in valid format"));
    }
    final String acctId = arn.substring(0, idx);
    
    final Account owner = Accounts.lookupAccountById(acctId);
    final String prefix = String.format("arn:aws:iam::%s:server-certificate", acctId);
    if(!certArn.startsWith(prefix))
      throw new AuthException(AuthException.SERVER_CERT_NO_SUCH_ENTITY);
    
    final String pathAndName = certArn.replace(prefix, "");
    final String certName = pathAndName.substring(pathAndName.lastIndexOf("/")+1);
    
    final ServerCertificate targetCert = owner.lookupServerCertificate(certName);
    if(targetCert==null)
      throw new AuthException(AuthException.SERVER_CERT_NO_SUCH_ENTITY);
    return targetCert;
  }
}
