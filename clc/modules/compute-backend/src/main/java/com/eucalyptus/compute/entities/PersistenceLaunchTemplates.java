/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.compute.entities;

import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.compute.common.CloudMetadata.LaunchTemplateMetadata;
import com.eucalyptus.compute.common.internal.ComputeMetadataException;
import com.eucalyptus.compute.common.internal.ComputeMetadataNotFoundException;
import com.eucalyptus.compute.common.internal.vm.LaunchTemplate;
import com.eucalyptus.compute.common.internal.vm.LaunchTemplates;
import com.eucalyptus.entities.AbstractPersistentSupport;

/**
 *
 */
@ComponentNamed
public class PersistenceLaunchTemplates extends AbstractPersistentSupport<LaunchTemplateMetadata, LaunchTemplate, ComputeMetadataException> implements LaunchTemplates {

  public PersistenceLaunchTemplates() {
    super("launch-template");
  }

  @Override
  protected ComputeMetadataException notFoundException(final String message, final Throwable cause) {
    return new ComputeMetadataNotFoundException(message, cause);
  }

  @Override
  protected ComputeMetadataException metadataException(final String message, final Throwable cause) {
    return new ComputeMetadataException(message, cause);
  }

  @Override
  protected LaunchTemplate exampleWithOwner(final OwnerFullName ownerFullName) {
    return LaunchTemplate.exampleWithOwner(ownerFullName);
  }

  @Override
  protected LaunchTemplate exampleWithName(final OwnerFullName ownerFullName, final String name) {
    return LaunchTemplate.exampleWithName(ownerFullName, name);
  }
}
