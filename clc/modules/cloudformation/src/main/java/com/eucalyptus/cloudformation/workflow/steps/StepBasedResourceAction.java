/*************************************************************************
 * Copyright 2015-2016 Ent. Services Development Corporation LP
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

package com.eucalyptus.cloudformation.workflow.steps;

import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by ethomas on 12/4/15.
 */
public abstract class StepBasedResourceAction extends ResourceAction {

  public Integer getMultiUpdateStepTimeoutPollMaximumInterval() {
    return (int) TimeUnit.SECONDS.toSeconds(30);
  }

  public Integer getMultiStepTimeoutPollMaximumInterval() {
    return (int) TimeUnit.SECONDS.toSeconds(30);
  }

  protected Map<String, Step> createSteps = Maps.newLinkedHashMap();
  public final Step getCreateStep(String stepId) {
    return createSteps.get(stepId);
  }

  protected Map<String, Step> deleteSteps = Maps.newLinkedHashMap();

  public final Step getDeleteStep(String stepId) {
    return deleteSteps.get(stepId);
  }

  protected EnumMap<UpdateTypeAndDirection, Map<String, UpdateStep>> updateStepEnumMap = Maps.newEnumMap(UpdateTypeAndDirection.class);
  public final UpdateStep getUpdateStep(UpdateTypeAndDirection updateTypeAndDirection, String stepId) {
    return updateStepEnumMap.get(updateTypeAndDirection).get(stepId);
  }


  public StepBasedResourceAction(Map<String, Step> addedCreateSteps, Map<String, Step> addedDeleteSteps,
                                 Map<String, UpdateStep> addedUpdateNoInterruptionSteps,
                                 Map<String, UpdateStep> addedUpdateSomeInterruptionSteps) {
    for (UpdateTypeAndDirection updateTypeAndDirection : UpdateTypeAndDirection.values()) {
      updateStepEnumMap.put(updateTypeAndDirection, Maps.<String, UpdateStep>newLinkedHashMap());
    }
    clearAndPutIfNotNull(createSteps, addedCreateSteps);
    clearAndPutIfNotNull(deleteSteps, addedDeleteSteps);
    clearAndPutIfNotNull(updateStepEnumMap.get(UpdateTypeAndDirection.UPDATE_NO_INTERRUPTION), addedUpdateNoInterruptionSteps);
    clearAndPutIfNotNull(updateStepEnumMap.get(UpdateTypeAndDirection.UPDATE_SOME_INTERRUPTION), addedUpdateSomeInterruptionSteps);
    // defaults for the rest
    clearAndPutIfNotNull(updateStepEnumMap.get(UpdateTypeAndDirection.UPDATE_WITH_REPLACEMENT), createStepsToUpdateWithReplacementSteps(addedCreateSteps));

    clearAndPutIfNotNull(updateStepEnumMap.get(UpdateTypeAndDirection.UPDATE_ROLLBACK_NO_INTERRUPTION), addedUpdateNoInterruptionSteps);
    clearAndPutIfNotNull(updateStepEnumMap.get(UpdateTypeAndDirection.UPDATE_ROLLBACK_SOME_INTERRUPTION), addedUpdateSomeInterruptionSteps);
    clearAndPutIfNotNull(updateStepEnumMap.get(UpdateTypeAndDirection.UPDATE_ROLLBACK_WITH_REPLACEMENT), Maps.<String, UpdateStep>newHashMap()); // by default, nothing to do.  (Cleanup will take care of it)
  }

  public void setCreateSteps(Map<String, Step> addedCreateSteps) {
    clearAndPutIfNotNull(createSteps, addedCreateSteps);
  }

  public void setDeleteSteps(Map<String, Step> addedDeleteSteps) {
    clearAndPutIfNotNull(deleteSteps, addedDeleteSteps);
  }

  public void setUpdateSteps(UpdateTypeAndDirection updateTypeAndDirection, Map<String, UpdateStep> addedUpdateSteps) {
    clearAndPutIfNotNull(updateStepEnumMap.get(updateTypeAndDirection), addedUpdateSteps);
  }

  protected static <T> void clearAndPutIfNotNull(Map<String, T> map, Map<String, T> addedMap){
    map.clear();
    if (addedMap != null) map.putAll(addedMap);
  }

  public static final Map<String, Step> fromEnum(Class<? extends Step> stepClass) {
    Map<String, Step> stepMap = Maps.newLinkedHashMap();
    Step[] steps = stepClass.getEnumConstants();
    if (steps != null) {
      for (Step step: steps) {
        stepMap.put(step.name(), step);
      }
    }
    return stepMap;
  }

  public static final Map<String, UpdateStep> fromUpdateEnum(Class<? extends UpdateStep> stepClass) {
    Map<String, UpdateStep> stepMap = Maps.newLinkedHashMap();
    UpdateStep[] steps = stepClass.getEnumConstants();
    if (steps != null) {
      for (UpdateStep step: steps) {
        stepMap.put(step.name(), step);
      }
    }
    return stepMap;
  }

  public static final Map<String, UpdateStep> createStepsToUpdateWithReplacementSteps(Map<String, Step> createStepsMap) {
    return Maps.transformValues(createStepsMap, new Function<Step, UpdateStep>() {

      @Nullable
      @Override
      public UpdateStep apply(final @Nullable Step step) {
        return new UpdateStep() {

          @Override
          public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
            return step.perform(newResourceAction);
          }

          @Nullable
          @Override
          public Integer getTimeout() {
            return step.getTimeout();
          }

          @Override
          public String name() {
            return step.name();
          }
        };
      }
    });
  }

  @Override
  public List<String> getCreateStepIds( ) {
    return Lists.newArrayList(createSteps.keySet());
  }

  @Override
  public List<String> getDeleteStepIds( ) {
    return Lists.newArrayList(deleteSteps.keySet());
  }

  @Override
  public List<String> getUpdateStepIds( final UpdateTypeAndDirection updateTypeAndDirection ) {
    return Lists.newArrayList(updateStepEnumMap.get(updateTypeAndDirection).keySet());
  }
}
