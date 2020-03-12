/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.propertytypes;

import java.util.Objects;
import com.eucalyptus.cloudformation.resources.annotations.Property;
import com.eucalyptus.cloudformation.resources.annotations.Required;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class Route53QueryLoggingConfig {

  @Property
  @Required
  private String cloudWatchLogsLogGroupArn;

  public String getCloudWatchLogsLogGroupArn() {
    return cloudWatchLogsLogGroupArn;
  }

  public void setCloudWatchLogsLogGroupArn(final String cloudWatchLogsLogGroupArn) {
    this.cloudWatchLogsLogGroupArn = cloudWatchLogsLogGroupArn;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Route53QueryLoggingConfig that = (Route53QueryLoggingConfig) o;
    return Objects.equals(getCloudWatchLogsLogGroupArn(), that.getCloudWatchLogsLogGroupArn());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCloudWatchLogsLogGroupArn());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("cloudWatchLogsLogGroupArn", cloudWatchLogsLogGroupArn)
        .toString();
  }
}
