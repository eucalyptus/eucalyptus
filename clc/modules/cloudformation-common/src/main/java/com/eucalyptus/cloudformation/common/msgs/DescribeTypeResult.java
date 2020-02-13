/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.common.msgs;

import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRange;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegex;
import com.eucalyptus.cloudformation.common.CloudFormationMessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class DescribeTypeResult extends EucalyptusData {

  @FieldRange(max = 1024)
  private String arn;

  @FieldRange(min = 1, max = 128)
  private String defaultVersionId;

  @FieldRegex(FieldRegexValue.ENUM_DEPRECATEDSTATUS)
  private String deprecatedStatus;

  @FieldRange(min = 1, max = 1024)
  private String description;

  @FieldRange(max = 4096)
  private String documentationUrl;

  @FieldRange(min = 1, max = 256)
  private String executionRoleArn;

  private java.util.Date lastUpdated;

  private LoggingConfig loggingConfig;

  @FieldRegex(FieldRegexValue.ENUM_PROVISIONINGTYPE)
  private String provisioningType;

  @FieldRange(min = 1, max = 16777216)
  private String schema;

  @FieldRange(max = 4096)
  private String sourceUrl;

  private java.util.Date timeCreated;

  @FieldRegex(FieldRegexValue.ENUM_REGISTRYTYPE)
  private String type;

  @FieldRange(min = 10, max = 196)
  private String typeName;

  @FieldRegex(FieldRegexValue.ENUM_VISIBILITY)
  private String visibility;

  public String getArn() {
    return arn;
  }

  public void setArn(final String arn) {
    this.arn = arn;
  }

  public String getDefaultVersionId() {
    return defaultVersionId;
  }

  public void setDefaultVersionId(final String defaultVersionId) {
    this.defaultVersionId = defaultVersionId;
  }

  public String getDeprecatedStatus() {
    return deprecatedStatus;
  }

  public void setDeprecatedStatus(final String deprecatedStatus) {
    this.deprecatedStatus = deprecatedStatus;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public String getDocumentationUrl() {
    return documentationUrl;
  }

  public void setDocumentationUrl(final String documentationUrl) {
    this.documentationUrl = documentationUrl;
  }

  public String getExecutionRoleArn() {
    return executionRoleArn;
  }

  public void setExecutionRoleArn(final String executionRoleArn) {
    this.executionRoleArn = executionRoleArn;
  }

  public java.util.Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(final java.util.Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public LoggingConfig getLoggingConfig() {
    return loggingConfig;
  }

  public void setLoggingConfig(final LoggingConfig loggingConfig) {
    this.loggingConfig = loggingConfig;
  }

  public String getProvisioningType() {
    return provisioningType;
  }

  public void setProvisioningType(final String provisioningType) {
    this.provisioningType = provisioningType;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(final String schema) {
    this.schema = schema;
  }

  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(final String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public java.util.Date getTimeCreated() {
    return timeCreated;
  }

  public void setTimeCreated(final java.util.Date timeCreated) {
    this.timeCreated = timeCreated;
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

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(final String visibility) {
    this.visibility = visibility;
  }

}
