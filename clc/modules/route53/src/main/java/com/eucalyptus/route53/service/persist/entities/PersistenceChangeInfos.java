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
import com.eucalyptus.route53.common.Route53Metadata.ChangeMetadata;
import com.eucalyptus.route53.service.persist.ChangeInfos;

/**
 *
 */
@ComponentNamed
public class PersistenceChangeInfos extends Route53PersistenceSupport<ChangeMetadata, ChangeInfo> implements ChangeInfos {

  public PersistenceChangeInfos() {
    super("change");
  }

  @Override
  protected ChangeInfo exampleWithOwner(final OwnerFullName ownerFullName) {
    return ChangeInfo.exampleWithOwner(ownerFullName);
  }

  @Override
  protected ChangeInfo exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return ChangeInfo.exampleWithName(ownerFullName, name);
  }
}
