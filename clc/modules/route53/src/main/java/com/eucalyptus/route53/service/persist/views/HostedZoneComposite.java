/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.views;

import java.util.List;
import org.immutables.value.Value.Immutable;

/**
 *
 */
@Immutable
public interface HostedZoneComposite extends Comparable<HostedZoneComposite> {

  HostedZoneView getHostedZone();

  List<ResourceRecordSetView> getResourceRecordSets();

  @Override
  default int compareTo(final HostedZoneComposite other) {
    int compare = getHostedZone().getZoneName().compareTo(other.getHostedZone().getZoneName());
    if(compare==0){
      compare = getHostedZone().getOwnerAccountNumber().compareTo(other.getHostedZone().getOwnerAccountNumber());
    }
    return compare;
  }
}
