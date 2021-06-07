/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service.persist.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.route53.common.Route53Metadata.HostedZoneMetadata;
import com.eucalyptus.route53.service.persist.HostedZones;

/**
 *
 */
@ComponentNamed
public class PersistenceHostedZones extends Route53PersistenceSupport<HostedZoneMetadata, HostedZone> implements HostedZones {

  public PersistenceHostedZones() {
    super("hostedzone");
  }

  @Override
  protected HostedZone exampleWithOwner(final OwnerFullName ownerFullName) {
    return HostedZone.exampleWithOwner(ownerFullName);
  }

  @Override
  protected HostedZone exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return HostedZone.exampleWithName(ownerFullName, name);
  }
}
