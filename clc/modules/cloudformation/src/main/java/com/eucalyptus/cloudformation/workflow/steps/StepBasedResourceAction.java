package com.eucalyptus.cloudformation.workflow.steps;

import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateTypeAndDirection;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.glisten.WorkflowOperations;

import javax.annotation.Nullable;
import java.util.EnumMap;
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
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int createdResourceVersion) {
    List<String> stepIds = Lists.newArrayList(createSteps.keySet());
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId, createdResourceVersion);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    List<String> stepIds = Lists.newArrayList(deleteSteps.keySet());
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
  }

  @Override
  public Promise<String> getUpdateCleanupPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    List<String> stepIds = Lists.newArrayList(deleteSteps.keySet());
    return new CleanupMultiStepPromise(workflowOperations, stepIds, this).getCleanupPromise(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
  }

  @Override
  public Promise<String> getUpdateRollbackCleanupPromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int rolledBackResourceVersion) {
    List<String> stepIds = Lists.newArrayList(deleteSteps.keySet());
    return new CleanupMultiStepPromise(workflowOperations, stepIds, this).getCleanupPromise(resourceId, stackId, accountId, effectiveUserId, rolledBackResourceVersion);
  }

  @Override
  public Promise<String> getUpdatePromise(UpdateTypeAndDirection updateTypeAndDirection, WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId, int updatedResourceVersion) {
    List<String> stepIds = Lists.newArrayList(updateStepEnumMap.get(updateTypeAndDirection).keySet());
    return new UpdateMultiStepPromise(workflowOperations, stepIds, this, updateTypeAndDirection).getUpdatePromise(resourceId, stackId, accountId, effectiveUserId, updatedResourceVersion);
  }

}
