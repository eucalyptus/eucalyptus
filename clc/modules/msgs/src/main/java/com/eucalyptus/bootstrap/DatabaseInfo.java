/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.bootstrap;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.crypto.Cipher;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PersistenceContext;
import javax.persistence.Table;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;
import org.hibernate.annotations.Type;

import com.eucalyptus.component.auth.SystemCredentials;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableFieldType;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.ConfigurablePropertyException;
import com.eucalyptus.configurable.PropertyChangeListener;
import com.eucalyptus.crypto.Ciphers;
import com.eucalyptus.crypto.Crypto;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.scripting.Groovyness;

@Entity
@PersistenceContext(name="eucalyptus_config")
@Table( name = "config_database" )
@ConfigurableClass(root = "services.database", description = "Parameters controlling database information.", singleton = true)
public class DatabaseInfo extends AbstractPersistent {
  private static final Logger  LOG   = Logger.getLogger( DatabaseInfo.class );
  
  @ConfigurableField( displayName = "append_only_host", 
      description = "host address of the backend database for append-only data",
      initial = "localhost" )
  @Column(name = "append_only_host")
  private String appendOnlyHost = null;

  @ConfigurableField( displayName = "append_only_port", 
      description = "port number of the backend database for append-only data",
      changeListener = AppendOnlyPortChangeListener.class)
  @Column(name = "append_only_port")
  private String appendOnlyPort = null;
  
  @ConfigurableField( displayName = "append_only_user", 
      description = "user name of the backend database for append-only data")
  @Column(name = "append_only_user")
  private String appendOnlyUser = null;

  @ConfigurableField( displayName = "append_only_password", 
      description = "password of the backend database for append-only data",
      type = ConfigurableFieldType.KEYVALUEHIDDEN)
  @Column(name = "append_only_password")
  @Type(type="org.hibernate.type.StringClobType")
  @Lob
  private String appendOnlyPassword = null; 
  
  @ConfigurableField( displayName = "append_only_ssl", 
      description = "ssl certificate to use when connecting to the backend database for append-only data",
      type = ConfigurableFieldType.KEYVALUEHIDDEN)
  @Column(name = "append_only_ssl_certificate")
  @Type(type="org.hibernate.type.StringClobType")
  @Lob
  private String appendOnlySslCert = null;

  
  public DatabaseInfo() { }
  
  private static DatabaseInfo newDefault() {
    final DatabaseInfo newInfo = new DatabaseInfo();
    newInfo.appendOnlyHost = "localhost";
    newInfo.appendOnlyPort = "";
    newInfo.appendOnlyUser = "";
    newInfo.appendOnlyPassword = "";
    newInfo.appendOnlySslCert = "";
    return newInfo;
  }
  
  public void setAppendOnlyHost(final String host){
    this.appendOnlyHost = host;
  }
  
  public String getAppendOnlyHost(){
    return this.appendOnlyHost;
  }
  
  public void setAppendOnlyPort(final String port){
    this.appendOnlyPort = port;
  }
  
  public String getAppendOnlyPort(){ 
    return this.appendOnlyPort;
  }
  
  public void setAppendOnlyUser(final String user){
    this.appendOnlyUser = user;
  }
  
  public String getAppendOnlyUser(){
    return this.appendOnlyUser;
  }
  
  public void setAppendOnlyPassword(final String password){
    try{
      final X509Certificate cloudCert = SystemCredentials.lookup(Eucalyptus.class).getCertificate();
      final Cipher cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.ENCRYPT_MODE, cloudCert.getPublicKey(), Crypto.getSecureRandomSupplier( ).get( ));
      byte[] bencPassword = cipher.doFinal(password.getBytes());
      final String encryptedPassword = new String(Base64.encode(bencPassword));
      this.appendOnlyPassword = encryptedPassword;
    }catch(final Exception ex){
      LOG.error("Failed to encrypt the database password");
    }
  }
  
  public String getAppendOnlyPassword(){
    try{
      final PrivateKey cloudPk = SystemCredentials.lookup(Eucalyptus.class).getPrivateKey();
      final Cipher cipher = Ciphers.RSA_PKCS1.get();
      cipher.init(Cipher.DECRYPT_MODE, cloudPk, Crypto.getSecureRandomSupplier().get( ));
      byte[] decoded = Base64.decode(this.appendOnlyPassword.getBytes());
      byte[] bdecPassword = cipher.doFinal(decoded);
      final String password= new String(bdecPassword);
      return password;
    }catch(final Exception ex){
      return null;
    }
 }
  
  public void setAppendOnlySslCert(final String cert){
    this.appendOnlySslCert = cert;
  }
  
  public String getAppendOnlySslCert() {
    return this.appendOnlySslCert;
  }

  private void resetDatabase() {
    if (this.appendOnlyHost == null ||  this.appendOnlyHost.length()<=0)
      return;
    else if (this.appendOnlyPort == null || this.appendOnlyPort.length()<=0)
      return;
    else if (this.appendOnlyUser == null || this.appendOnlyUser.length()<=0)
      return;
    else if (this.appendOnlyPassword == null || this.appendOnlyPassword.length()<=0)
      return;
    
    try{
      Groovyness.run("setup_dbpool_remote.groovy");
    }catch(final Exception ex) {
      LOG.error("Failed to reset remote db pool", ex);
    }
    
    try{
      Groovyness.run("setup_persistence_remote.groovy");
    }catch(final Exception ex) {
      LOG.error("Failed to reset persistence contexts", ex);
    }
    
    LOG.info(String.format("Remote databases are reset [%s:%s]", this.appendOnlyHost, this.appendOnlyPort));
  }

  public static DatabaseInfo getDatabaseInfo() {
    DatabaseInfo conf = null;
    try {
      conf = Transactions.find(new DatabaseInfo());
    } catch (Exception e) {
      LOG.warn("Database information is not found. Loading defaults.");
      try {
        conf = Transactions.saveDirect(newDefault());
      } catch (Exception e1) {
        try {
          conf = Transactions.find(new DatabaseInfo());
        } catch (Exception e2) {
          LOG.warn("Failed to persist and retrieve DatabaseInfo entity");
        }
      }
    }
    if (conf == null) {
      conf = newDefault();
    }
    return conf;
  }
 
  public static final class AppendOnlyPortChangeListener implements PropertyChangeListener {
    @Override
    public void fireChange(ConfigurableProperty t, Object newValue)
        throws ConfigurablePropertyException {
      try{
        int portNum = Integer.parseInt( (String) newValue);
        if(portNum < 1 || portNum > 65535)
          throw new Exception();
      }catch(final Exception ex){
        throw new ConfigurablePropertyException("Invalid port number");
      }
    }
  }
}
