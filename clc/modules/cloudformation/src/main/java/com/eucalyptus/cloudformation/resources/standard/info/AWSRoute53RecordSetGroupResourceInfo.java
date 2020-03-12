/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.info;

import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.annotations.AttributeJson;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class AWSRoute53RecordSetGroupResourceInfo extends ResourceInfo {

  @AttributeJson
  private String hostedZoneId;

  public AWSRoute53RecordSetGroupResourceInfo( ) {
    setType( "AWS::Route53::RecordSetGroup" );
  }

  public String getHostedZoneId() {
    return hostedZoneId;
  }

  public void setHostedZoneId(final String hostedZoneId) {
    this.hostedZoneId = hostedZoneId;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("hostedZoneId", hostedZoneId)
        .toString();
  }

}
