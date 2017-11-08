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
package com.eucalyptus.auth;

import java.util.Date;

import org.apache.log4j.Logger;

/**
 * @author Sang-Min Park
 *
 */
public class ServerCertificate {
  private static Logger LOG = Logger.getLogger(ServerCertificate.class);

  private String owningAccountNumber = null;
  private String certName = null;
  private String certId = null;
  private String certPath =  null;
  private String certBody = null;
  private String certChain = null;
  private String privateKey = null;
  private Date createdTime = null;
  private Date expiration = null;

  public ServerCertificate(final String owningAccountNumber, final String certName, final Date createdTime, final Date expiration){
    this.owningAccountNumber= owningAccountNumber;
    this.certName = certName;
    this.createdTime = createdTime;
    this.expiration = expiration;
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
    
    return String.format("arn:aws:iam::%s:server-certificate%s%s", this.owningAccountNumber, path, this.certName);
  }
  
  public Date getCreatedTime(){
    return this.createdTime;
  }

  public Date getExpiration(){
    return this.expiration;
  }
}
