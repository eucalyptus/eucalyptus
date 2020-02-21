/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class HealthCheckConfig extends EucalyptusData {

  private AlarmIdentifier alarmIdentifier;

  @FieldRange(max = 256)
  private ChildHealthCheckList childHealthChecks;

  private Boolean disabled;

  private Boolean enableSNI;

  @FieldRange(min = 1, max = 10)
  private Integer failureThreshold;

  @FieldRange(max = 255)
  private String fullyQualifiedDomainName;

  @FieldRange(max = 256)
  private Integer healthThreshold;

  @FieldRange(max = 45)
  private String iPAddress;

  @FieldRegex(FieldRegexValue.ENUM_INSUFFICIENTDATAHEALTHSTATUS)
  private String insufficientDataHealthStatus;

  private Boolean inverted;

  private Boolean measureLatency;

  @FieldRange(min = 1, max = 65535)
  private Integer port;

  @FieldRange(min = 3, max = 64)
  private HealthCheckRegionList regions;

  @FieldRange(min = 10, max = 30)
  private Integer requestInterval;

  @FieldRange(max = 255)
  private String resourcePath;

  @FieldRange(max = 255)
  private String searchString;

  @Nonnull
  @FieldRegex(FieldRegexValue.ENUM_HEALTHCHECKTYPE)
  private String type;

  public AlarmIdentifier getAlarmIdentifier() {
    return alarmIdentifier;
  }

  public void setAlarmIdentifier(final AlarmIdentifier alarmIdentifier) {
    this.alarmIdentifier = alarmIdentifier;
  }

  public ChildHealthCheckList getChildHealthChecks() {
    return childHealthChecks;
  }

  public void setChildHealthChecks(final ChildHealthCheckList childHealthChecks) {
    this.childHealthChecks = childHealthChecks;
  }

  public Boolean getDisabled() {
    return disabled;
  }

  public void setDisabled(final Boolean disabled) {
    this.disabled = disabled;
  }

  public Boolean getEnableSNI() {
    return enableSNI;
  }

  public void setEnableSNI(final Boolean enableSNI) {
    this.enableSNI = enableSNI;
  }

  public Integer getFailureThreshold() {
    return failureThreshold;
  }

  public void setFailureThreshold(final Integer failureThreshold) {
    this.failureThreshold = failureThreshold;
  }

  public String getFullyQualifiedDomainName() {
    return fullyQualifiedDomainName;
  }

  public void setFullyQualifiedDomainName(final String fullyQualifiedDomainName) {
    this.fullyQualifiedDomainName = fullyQualifiedDomainName;
  }

  public Integer getHealthThreshold() {
    return healthThreshold;
  }

  public void setHealthThreshold(final Integer healthThreshold) {
    this.healthThreshold = healthThreshold;
  }

  public String getIPAddress() {
    return iPAddress;
  }

  public void setIPAddress(final String iPAddress) {
    this.iPAddress = iPAddress;
  }

  public String getInsufficientDataHealthStatus() {
    return insufficientDataHealthStatus;
  }

  public void setInsufficientDataHealthStatus(final String insufficientDataHealthStatus) {
    this.insufficientDataHealthStatus = insufficientDataHealthStatus;
  }

  public Boolean getInverted() {
    return inverted;
  }

  public void setInverted(final Boolean inverted) {
    this.inverted = inverted;
  }

  public Boolean getMeasureLatency() {
    return measureLatency;
  }

  public void setMeasureLatency(final Boolean measureLatency) {
    this.measureLatency = measureLatency;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(final Integer port) {
    this.port = port;
  }

  public HealthCheckRegionList getRegions() {
    return regions;
  }

  public void setRegions(final HealthCheckRegionList regions) {
    this.regions = regions;
  }

  public Integer getRequestInterval() {
    return requestInterval;
  }

  public void setRequestInterval(final Integer requestInterval) {
    this.requestInterval = requestInterval;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  public void setResourcePath(final String resourcePath) {
    this.resourcePath = resourcePath;
  }

  public String getSearchString() {
    return searchString;
  }

  public void setSearchString(final String searchString) {
    this.searchString = searchString;
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = type;
  }

}
