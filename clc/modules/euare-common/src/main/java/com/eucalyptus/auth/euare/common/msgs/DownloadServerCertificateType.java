/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
