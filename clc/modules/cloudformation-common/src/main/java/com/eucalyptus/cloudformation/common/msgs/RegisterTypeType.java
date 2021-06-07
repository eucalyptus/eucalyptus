/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;


public class RegisterTypeType extends CloudFormationMessage {

  @FieldRange(min = 1, max = 128)
  private String clientRequestToken;

  @FieldRange(min = 1, max = 256)
  private String executionRoleArn;

  private LoggingConfig loggingConfig;

  @Nonnull
  @FieldRange(min = 1, max = 4096)
  private String schemaHandlerPackage;

  @FieldRegex(FieldRegexValue.ENUM_REGISTRYTYPE)
  private String type;

  @Nonnull
  @FieldRange(min = 10, max = 196)
  private String typeName;

  public String getClientRequestToken() {
    return clientRequestToken;
  }

  public void setClientRequestToken(final String clientRequestToken) {
    this.clientRequestToken = clientRequestToken;
  }

  public String getExecutionRoleArn() {
    return executionRoleArn;
  }

  public void setExecutionRoleArn(final String executionRoleArn) {
    this.executionRoleArn = executionRoleArn;
  }

  public LoggingConfig getLoggingConfig() {
    return loggingConfig;
  }

  public void setLoggingConfig(final LoggingConfig loggingConfig) {
    this.loggingConfig = loggingConfig;
  }

  public String getSchemaHandlerPackage() {
    return schemaHandlerPackage;
  }

  public void setSchemaHandlerPackage(final String schemaHandlerPackage) {
    this.schemaHandlerPackage = schemaHandlerPackage;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

  public String getTypeName() {
    return typeName;
  }

  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

}
