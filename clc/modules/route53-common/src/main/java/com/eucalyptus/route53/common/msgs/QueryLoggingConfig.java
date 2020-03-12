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
import edu.ucsb.eucalyptus.msgs.EucalyptusData;


public class QueryLoggingConfig extends EucalyptusData {

  @Nonnull
  private String cloudWatchLogsLogGroupArn;

  @Nonnull
  @FieldRange(max = 32)
  private String hostedZoneId;

  @Nonnull
  @FieldRange(min = 1, max = 36)
  private String id;

  public String getCloudWatchLogsLogGroupArn() {
    return cloudWatchLogsLogGroupArn;
  }

  public void setCloudWatchLogsLogGroupArn(final String cloudWatchLogsLogGroupArn) {
    this.cloudWatchLogsLogGroupArn = cloudWatchLogsLogGroupArn;
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

}
