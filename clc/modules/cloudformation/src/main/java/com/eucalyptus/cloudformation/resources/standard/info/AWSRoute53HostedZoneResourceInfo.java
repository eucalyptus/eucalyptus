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
public class AWSRoute53HostedZoneResourceInfo extends ResourceInfo {

  @AttributeJson
  private String nameServers;

  public AWSRoute53HostedZoneResourceInfo( ) {
    setType( "AWS::Route53::HostedZone" );
  }

  @Override
  public boolean supportsTags( ) {
    return true;
  }

  public String getNameServers() {
    return nameServers;
  }

  public void setNameServers(final String nameServers) {
    this.nameServers = nameServers;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("nameServers", nameServers)
        .toString();
  }
}
