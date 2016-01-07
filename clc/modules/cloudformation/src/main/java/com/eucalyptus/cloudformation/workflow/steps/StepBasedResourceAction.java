package com.eucalyptus.cloudformation.workflow.steps;

import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.glisten.WorkflowOperations;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Created by ethomas on 12/4/15.
 */
public abstract class StepBasedResourceAction extends ResourceAction {

  protected Map<String, Step> createSteps = Maps.newLinkedHashMap();

  public final Step getCreateStep(String stepId) {
    return createSteps.get(stepId);
  }

  protected Map<String, Step> deleteSteps = Maps.newLinkedHashMap();

  public final Step getDeleteStep(String stepId) {
    return deleteSteps.get(stepId);
  }

  protected Map<String, UpdateStep> updateNoInterruptionSteps = Maps.newLinkedHashMap();

  public final UpdateStep getUpdateNoInterruptionStep(String stepId) {
    return updateNoInterruptionSteps.get(stepId);
  }

  protected Map<String, UpdateStep> updateSomeInterruptionSteps = Maps.newLinkedHashMap();

  public final UpdateStep getUpdateSomeInterruptionStep(String stepId) {
    return updateSomeInterruptionSteps.get(stepId);
  }
  protected Map<String, UpdateStep> updateWithReplacementSteps = Maps.newLinkedHashMap();

  public final UpdateStep getUpdateWithReplacementStep(String stepId) {
    return updateWithReplacementSteps.get(stepId);
  }

  public StepBasedResourceAction(Map<String, Step> addedCreateSteps, Map<String, Step> addedDeleteSteps,
                                 Map<String, UpdateStep> addedUpdateNoInterruptionSteps,
                                 Map<String, UpdateStep> addedUpdateSomeInterruptionSteps,
                                 Map<String, UpdateStep> addedUpdateWithReplacementSteps) {
    putIfNotNull(createSteps, addedCreateSteps);
    putIfNotNull(deleteSteps, addedDeleteSteps);
    putIfNotNull(updateNoInterruptionSteps, addedUpdateNoInterruptionSteps);
    putIfNotNull(updateSomeInterruptionSteps, addedUpdateSomeInterruptionSteps);
    putIfNotNull(updateWithReplacementSteps, addedUpdateWithReplacementSteps);
  }

  private static <T> void putIfNotNull(Map<String, T> map, Map<String, T> addedMap){
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
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion) {
    List<String> stepIds = Lists.newArrayList(createSteps.keySet());
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId, updateVersion);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion) {
    List<String> stepIds = Lists.newArrayList(deleteSteps.keySet());
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId, updateVersion);
  }

  @Override
  public Promise<String> getUpdateCleanupPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion) {
    List<String> stepIds = Lists.newArrayList(deleteSteps.keySet());
    return new UpdateCleanupMultiStepPromise(workflowOperations, stepIds, this).getUpdateCleanupPromise(resourceId, stackId, accountId, effectiveUserId, updateVersion);
  }

  @Override
  public Promise<String> getUpdateNoInterruptionPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion) {
    List<String> stepIds = Lists.newArrayList(updateNoInterruptionSteps.keySet());
    return new UpdateNoInterruptionMultiStepPromise(workflowOperations, stepIds, this).getUpdateNoInterruptionPromise(resourceId, stackId, accountId, effectiveUserId, updateVersion);
  }
  
  @Override
  public Promise<String> getUpdateSomeInterruptionPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion) {
    List<String> stepIds = Lists.newArrayList(updateSomeInterruptionSteps.keySet());
    return new UpdateSomeInterruptionMultiStepPromise(workflowOperations, stepIds, this).getUpdateSomeInterruptionPromise(resourceId, stackId, accountId, effectiveUserId, updateVersion);
  }
  @Override
  public Promise<String> getUpdateWithReplacementPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updateVersion) {
    List<String> stepIds = Lists.newArrayList(updateWithReplacementSteps.keySet());
    return new UpdateWithReplacementMultiStepPromise(workflowOperations, stepIds, this).getUpdateWithReplacementPromise(resourceId, stackId, accountId, effectiveUserId, updateVersion);
  }



}
