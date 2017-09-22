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
package com.eucalyptus.tokens.common.msgs;

import edu.ucsb.eucalyptus.msgs.EucalyptusData;

public class AssumeRoleWithWebIdentityResultType extends EucalyptusData {
  private CredentialsType credentials;
  private AssumedRoleUserType assumedRoleUser;
  private Integer packedPolicySize;
  private String audience;
  private String provider;
  private String subjectFromWebIdentityToken;

  public AssumeRoleWithWebIdentityResultType() {
  }

  public CredentialsType getCredentials() {
    return credentials;
  }

  public void setCredentials( CredentialsType credentials ) {
    this.credentials = credentials;
  }

  public AssumedRoleUserType getAssumedRoleUser() {
    return assumedRoleUser;
  }

  public void setAssumedRoleUser( AssumedRoleUserType assumedRoleUser ) {
    this.assumedRoleUser = assumedRoleUser;
  }

  public Integer getPackedPolicySize() {
    return packedPolicySize;
  }

  public void setPackedPolicySize( Integer packedPolicySize ) {
    this.packedPolicySize = packedPolicySize;
  }

  public String getAudience() {
    return audience;
  }

  public void setAudience( String audience ) {
    this.audience = audience;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider( String provider ) {
    this.provider = provider;
  }

  public String getSubjectFromWebIdentityToken() {
    return subjectFromWebIdentityToken;
  }

  public void setSubjectFromWebIdentityToken( String subjectFromWebIdentityToken ) {
    this.subjectFromWebIdentityToken = subjectFromWebIdentityToken;
  }
}
