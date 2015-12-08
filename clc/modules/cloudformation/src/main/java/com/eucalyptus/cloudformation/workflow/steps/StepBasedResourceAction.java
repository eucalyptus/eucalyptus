package com.eucalyptus.cloudformation.workflow.steps;

import com.amazonaws.services.simpleworkflow.flow.core.Promise;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.workflow.StackActivityClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.glisten.WorkflowOperations;

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

  public StepBasedResourceAction(Map<String, Step> addedCreateSteps, Map<String, Step> addedDeleteSteps) {
    createSteps.putAll(addedCreateSteps);
    deleteSteps.putAll(addedCreateSteps);
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


  @Override
  public Promise<String> getCreatePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.newArrayList(createSteps.keySet());
    return new CreateMultiStepPromise(workflowOperations, stepIds, this).getCreatePromise(resourceId, stackId, accountId, effectiveUserId);
  }

  @Override
  public Promise<String> getDeletePromise(WorkflowOperations<StackActivityClient> workflowOperations, String resourceId, String stackId, String accountId, String effectiveUserId) {
    List<String> stepIds = Lists.newArrayList(deleteSteps.keySet());
    return new DeleteMultiStepPromise(workflowOperations, stepIds, this).getDeletePromise(resourceId, stackId, accountId, effectiveUserId);
  }

}
