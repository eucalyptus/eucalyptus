/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.rds.common.msgs;

import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegex;
import com.eucalyptus.rds.common.RdsMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class UserAuthConfigInfo extends EucalyptusData {

  @FieldRegex(FieldRegexValue.ENUM_AUTHSCHEME)
  private String authScheme;

  private String description;

  @FieldRegex(FieldRegexValue.ENUM_IAMAUTHMODE)
  private String iAMAuth;

  private String secretArn;

  private String userName;

  public String getAuthScheme() {
    return authScheme;
  }

  public void setAuthScheme(final String authScheme) {
    this.authScheme = authScheme;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getIAMAuth() {
    return iAMAuth;
  }

  public void setIAMAuth(final String iAMAuth) {
    this.iAMAuth = iAMAuth;
  }

  public String getSecretArn() {
    return secretArn;
  }

  public void setSecretArn(final String secretArn) {
    this.secretArn = secretArn;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(final String userName) {
    this.userName = userName;
  }

}
