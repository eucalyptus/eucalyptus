/*************************************************************************
 * Copyright 2013-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.cloudformation.resources;

import java.util.List;
import com.eucalyptus.cloudformation.entity.VersionedStackEntity;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public abstract class ResourceAction {
  public abstract ResourceProperties getResourceProperties();

  public abstract void setResourceProperties(ResourceProperties resourceProperties);

  public abstract ResourceInfo getResourceInfo();

  public abstract void setResourceInfo(ResourceInfo resourceInfo);

  protected VersionedStackEntity stackEntity;

  public VersionedStackEntity getStackEntity() {
    return stackEntity;
  }

  public void setStackEntity(VersionedStackEntity stackEntity) {
    this.stackEntity = stackEntity;
  }

  public final String getDefaultPhysicalResourceId(int maxLength) {
    String stackName = (getStackEntity() != null && getStackEntity().getStackName() != null) ?
      getStackEntity().getStackName() : "UNKNOWN";
    String logicalResourceId = (getResourceInfo() != null && getResourceInfo().getLogicalResourceId() != null) ?
      getResourceInfo().getLogicalResourceId() : "UNKNOWN";
    return ResourceActionHelper.getDefaultPhysicalResourceId(stackName, logicalResourceId, maxLength);
  }

  public final String getDefaultPhysicalResourceId() {
    return getDefaultPhysicalResourceId(Integer.MAX_VALUE);
  }

  public void refreshAttributes() throws Exception {
    // Most resources will not support this action
  }
  public abstract UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) throws Exception;

  public boolean mustCheckUpdateTypeEvenIfNoPropertiesChanged() { // there are a couple of cases, specifically EIP and EIPAssociation if instance has been 'updated'
    return false;
  }

  public abstract List<String> getCreateStepIds( );

  public abstract List<String> getDeleteStepIds( );

  public abstract List<String> getUpdateStepIds( final UpdateTypeAndDirection updateTypeAndDirection );

  protected final <RP> UpdateType defaultUpdateType(
      final boolean stackTagsChanged,
      final RP properties,
      final RP otherProperties,
      final Set<Function<RP,Object>> propRequiresReplace,
      final Set<Function<RP,Object>> propRequiresSomeInterruption,
      final Set<Function<RP,Object>> propRequiresNoInterruption
      ) {
    UpdateType updateType = getResourceInfo().supportsTags() && stackTagsChanged ?
        UpdateType.NO_INTERRUPTION :
        UpdateType.NONE;
    for (final Function<RP,Object> propertyGet : propRequiresReplace) {
      if (!Objects.equals(propertyGet.apply(properties), propertyGet.apply(otherProperties))) {
        updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
      }
    }
    for (final Function<RP,Object> propertyGet : propRequiresSomeInterruption) {
      if (!Objects.equals(propertyGet.apply(properties), propertyGet.apply(otherProperties))) {
        updateType = UpdateType.max(updateType, UpdateType.SOME_INTERRUPTION);
      }
    }
    for (final Function<RP,Object> propertyGet : propRequiresNoInterruption) {
      if (!Objects.equals(propertyGet.apply(properties), propertyGet.apply(otherProperties))) {
        updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
      }
    }
    return updateType;
  }
}

