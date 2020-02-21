/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common.msgs;

import javax.annotation.Nonnull;
import com.eucalyptus.binding.HttpRequestMapping;
import com.eucalyptus.binding.HttpUriMapping;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRange;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegex;
import com.eucalyptus.route53.common.Route53MessageValidation.FieldRegexValue;


@HttpRequestMapping(method = "POST", uri = "/2013-04-01/healthcheck/{HealthCheckId}")
public class UpdateHealthCheckType extends Route53Message {

  private AlarmIdentifier alarmIdentifier;

  @FieldRange(max = 256)
  private ChildHealthCheckList childHealthChecks;

  private Boolean disabled;

  private Boolean enableSNI;

  @FieldRange(min = 1, max = 10)
  private Integer failureThreshold;

  @FieldRange(max = 255)
  private String fullyQualifiedDomainName;

  @Nonnull
  @HttpUriMapping(uri = "HealthCheckId")
  @FieldRange(max = 64)
  private String healthCheckId;

  @FieldRange(min = 1)
  private Long healthCheckVersion;

  @FieldRange(max = 256)
  private Integer healthThreshold;

  @FieldRange(max = 45)
  private String iPAddress;

  @FieldRegex(FieldRegexValue.ENUM_INSUFFICIENTDATAHEALTHSTATUS)
  private String insufficientDataHealthStatus;

  private Boolean inverted;

  @FieldRange(min = 1, max = 65535)
  private Integer port;

  @FieldRange(min = 3, max = 64)
  private HealthCheckRegionList regions;

  @FieldRange(max = 64)
  private ResettableElementNameList resetElements;

  @FieldRange(max = 255)
  private String resourcePath;

  @FieldRange(max = 255)
  private String searchString;

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

  public String getHealthCheckId() {
    return healthCheckId;
  }

  public void setHealthCheckId(final String healthCheckId) {
    this.healthCheckId = healthCheckId;
  }

  public Long getHealthCheckVersion() {
    return healthCheckVersion;
  }

  public void setHealthCheckVersion(final Long healthCheckVersion) {
    this.healthCheckVersion = healthCheckVersion;
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

  public ResettableElementNameList getResetElements() {
    return resetElements;
  }

  public void setResetElements(final ResettableElementNameList resetElements) {
    this.resetElements = resetElements;
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

}
