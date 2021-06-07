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
package com.eucalyptus.auth.euare.common.identity.msgs;

import java.util.ArrayList;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class SecurityToken extends EucalyptusData {

  private String originatingAccessKeyId;
  private String originatingUserId;
  private String originatingRoleId;
  private String nonce;
  private Long created;
  private Long expires;
  private ArrayList<SecurityTokenAttribute> attributes = Lists.newArrayList( );

  public String getOriginatingAccessKeyId( ) {
    return originatingAccessKeyId;
  }

  public void setOriginatingAccessKeyId( String originatingAccessKeyId ) {
    this.originatingAccessKeyId = originatingAccessKeyId;
  }

  public String getOriginatingUserId( ) {
    return originatingUserId;
  }

  public void setOriginatingUserId( String originatingUserId ) {
    this.originatingUserId = originatingUserId;
  }

  public String getOriginatingRoleId( ) {
    return originatingRoleId;
  }

  public void setOriginatingRoleId( String originatingRoleId ) {
    this.originatingRoleId = originatingRoleId;
  }

  public String getNonce( ) {
    return nonce;
  }

  public void setNonce( String nonce ) {
    this.nonce = nonce;
  }

  public Long getCreated( ) {
    return created;
  }

  public void setCreated( Long created ) {
    this.created = created;
  }

  public Long getExpires( ) {
    return expires;
  }

  public void setExpires( Long expires ) {
    this.expires = expires;
  }

  public ArrayList<SecurityTokenAttribute> getAttributes( ) {
    return attributes;
  }

  public void setAttributes( ArrayList<SecurityTokenAttribute> attributes ) {
    this.attributes = attributes;
  }
}
