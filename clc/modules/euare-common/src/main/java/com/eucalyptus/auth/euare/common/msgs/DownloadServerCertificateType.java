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
package com.eucalyptus.auth.euare.common.msgs;

import java.util.Date;
import com.eucalyptus.auth.euare.common.policy.IamPolicySpec;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.policy.annotation.PolicyAction;

@PolicyAction( vendor = PolicySpec.VENDOR_IAM, action = IamPolicySpec.IAM_DOWNLOADSERVERCERTIFICATE )
public class DownloadServerCertificateType extends EuareMessage {

  private String certificateArn;
  private String delegationCertificate;
  private String authSignature;
  private Date timestamp;
  private String signature;

  public String getCertificateArn( ) {
    return certificateArn;
  }

  public void setCertificateArn( String certificateArn ) {
    this.certificateArn = certificateArn;
  }

  public String getDelegationCertificate( ) {
    return delegationCertificate;
  }

  public void setDelegationCertificate( String delegationCertificate ) {
    this.delegationCertificate = delegationCertificate;
  }

  public String getAuthSignature( ) {
    return authSignature;
  }

  public void setAuthSignature( String authSignature ) {
    this.authSignature = authSignature;
  }

  public Date getTimestamp( ) {
    return timestamp;
  }

  public void setTimestamp( Date timestamp ) {
    this.timestamp = timestamp;
  }

  public String getSignature( ) {
    return signature;
  }

  public void setSignature( String signature ) {
    this.signature = signature;
  }
}
