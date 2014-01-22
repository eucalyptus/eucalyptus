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

import java.util.Date;

import org.apache.log4j.Logger;

import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.component.annotation.PolicyVendor;
import com.eucalyptus.util.RestrictedType;

/**
 * @author Sang-Min Park
 *
 */
public class ServerCertificate {
  private static Logger LOG = Logger.getLogger(ServerCertificate.class);
  private Account owningAccount = null;
  private String certName = null;
  private String certId = null;
  private String certPath =  null;
  private String certBody = null;
  private String certChain = null;
  private String privateKey = null;
  private Date createdTime = null;
  
  public ServerCertificate(final Account owningAcct, final String certName, final Date createdTime){
    this.owningAccount= owningAcct;
    this.certName = certName;
    this.createdTime = createdTime;
  }
  
  public void setCertificateName(final String name){
    this.certName = name;
  }
  
  public String getCertificateName(){
    return this.certName;
  }
  
  public void setCertificatePath(final String path){
    this.certPath = path;
  }
  
  public String getCertificatePath(){
    return this.certPath;
  }
  
  public void setCertificateBody(final String body){
    this.certBody = body;
  }
  
  public String getCertificateBody(){
    return this.certBody;
  }
  
  public void setCertificateChain(final String chain){
    this.certChain = chain;
  }
  
  public String getCertificateChain(){
    return this.certChain;
  }
  
  public void setCertificateId(final String id){
    this.certId = id;
  }
  
  public String getCertificateId(){
    return this.certId;
  }
  
  public void setPrivateKey(final String pk){
    this.privateKey = pk;
  }
  
  public String getPrivateKey(){
    return this.privateKey;
  }
  
  public String getArn(){
    String path = this.certPath;
    if(!path.endsWith("/"))
      path = path+"/";
    
    return String.format("arn:aws:iam::%s:server-certificate%s%s", this.owningAccount.getAccountNumber(), path, this.certName);
  }
  
  public Date getCreatedTime(){
    return this.createdTime;
  }
}
