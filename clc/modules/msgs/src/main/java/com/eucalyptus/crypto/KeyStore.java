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
 ************************************************************************/

package com.eucalyptus.crypto;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public interface KeyStore {
  
  public abstract KeyPair getKeyPair( final String alias, final String password ) throws GeneralSecurityException;
  
  public abstract boolean check( ) throws GeneralSecurityException;
  
  public abstract boolean containsEntry( final String alias );
  
  public abstract X509Certificate getCertificate( final String alias ) throws GeneralSecurityException;
  
  public abstract Key getKey( final String alias, final String password ) throws GeneralSecurityException;
  
  public abstract String getCertificateAlias( final String certPem ) throws GeneralSecurityException;
  
  public abstract String getCertificateAlias( final X509Certificate cert ) throws GeneralSecurityException;
  
  public abstract void addCertificate( final String alias, final X509Certificate cert ) throws IOException, GeneralSecurityException;
  
  public abstract void addKeyPair( final String alias, final X509Certificate cert, final PrivateKey privateKey, final String keyPassword ) throws IOException, GeneralSecurityException;
  
  public abstract void store( ) throws IOException, GeneralSecurityException;
  
  public abstract List<String> getAliases( ) throws KeyStoreException;
  
  public abstract String getFileName( );
  
  public abstract void remove( final String alias );
  
  public abstract void remove( );
  
  public abstract InputStream getAsInputStream( ) throws FileNotFoundException;
  
  public abstract List<X509Certificate> getAllCertificates( ) throws KeyStoreException;
  
}
