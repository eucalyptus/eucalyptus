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


public class ListQueryLoggingConfigsResponseType extends Route53Message {


  @FieldRange(max = 256)
  private String nextToken;

  @Nonnull
  private QueryLoggingConfigs queryLoggingConfigs;

  public String getNextToken() {
    return nextToken;
  }

  public void setNextToken(final String nextToken) {
    this.nextToken = nextToken;
  }

  public QueryLoggingConfigs getQueryLoggingConfigs() {
    return queryLoggingConfigs;
  }

  public void setQueryLoggingConfigs(final QueryLoggingConfigs queryLoggingConfigs) {
    this.queryLoggingConfigs = queryLoggingConfigs;
  }

}
