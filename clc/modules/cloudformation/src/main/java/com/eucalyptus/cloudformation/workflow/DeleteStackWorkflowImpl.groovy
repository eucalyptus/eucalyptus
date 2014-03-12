package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.eucalyptus.cloudformation.entity.StackEntityHelper
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.template.JsonHelper
import com.eucalyptus.cloudformation.template.dependencies.DependencyManager
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.netflix.glisten.WorkflowOperations
import com.netflix.glisten.impl.swf.SwfWorkflowOperations
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * Created by ethomas on 3/4/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class DeleteStackWorkflowImpl implements DeleteStackWorkflow {

  @Delegate
  WorkflowOperations<StackActivity> workflowOperations = SwfWorkflowOperations.of(StackActivity);

  private static final String EMPTY_JSON_NODE = JsonHelper.getStringFromJsonNode(new ObjectMapper().createObjectNode());

  private enum ResourceStatus {
    NOT_STARTED,
    IN_PROCESS,
    COMPLETE
  };

  @Override
  public void deleteStack(String stackId, String accountId, String resourceDependencyManagerJson, String effectiveUserId) {
    try {
      doTry {

        Map<String, ResourceInfo> resourceInfoMap = Maps.newConcurrentMap();

        Map<String, Settable<String>> deletedResourcePromiseMap = Maps.newConcurrentMap();
        Map<String, ResourceStatus> deletedResourceStatusMap = Maps.newConcurrentMap();
        DependencyManager resourceDependencyManager = StackEntityHelper.jsonToResourceDependencyManager(
          resourceDependencyManagerJson
        );

        for (String resourceName: resourceDependencyManager.getNodes()) {
          deletedResourceStatusMap.put(resourceName, ResourceStatus.NOT_STARTED);
          deletedResourcePromiseMap.put(resourceName, new Settable<String>());
        }

        // First create the Stack Event for the DELETE_STACK itself
        waitFor(promiseFor(activities.createGlobalStackEvent(
          stackId,
          accountId,
          StackResourceEntity.Status.DELETE_IN_PROGRESS.toString(),
          "User Initiated"))) {
          return doTry { // This is in case any part of setting up the stack fails
            // Now for each resource, set up the promises and the dependencies they have for each other

            for (String resourceId: deletedResourcePromiseMap.keySet()) {
              Collection<Promise<String>> deletePromisesDependedOn = Lists.newArrayList();

              for (String dependingResourceId: resourceDependencyManager.getDependentNodes(resourceId)) {
                // We have the opposite direction in delete than create,
                deletePromisesDependedOn.add(deletedResourcePromiseMap.get(dependingResourceId));
              }
              // Since we will use an "AndPromise" to depend on all of the dependent nodes and the AndPromise doesn't
              // have a return value (Promise<Vold>) and the dependent promises don't indicate which node we depend on,
              // we add one final promise to the and promise, that returns the node we are dealing with here.
              ObjectNode objectNode = new ObjectMapper().createObjectNode();
              objectNode.put("thisResourceId", resourceId);
              deletePromisesDependedOn.add(promiseFor(JsonHelper.getStringFromJsonNode(objectNode)));
              AndPromise jointPromise = new AndPromise(deletePromisesDependedOn);
              waitFor(jointPromise) {
                // at this point, all promises need to be complete
                String thisResourceId = null;
                for (Promise promise: jointPromise.getValues()) {
                  JsonNode jsonNode = JsonHelper.getJsonNodeFromString((String) promise.get());
                  if (jsonNode.has("thisResourceId")) {
                    thisResourceId = jsonNode.get("thisResourceId").textValue();
                  }
                } // the assumption here is that the global resourceInfo map is up to date...
                deletedResourceStatusMap.put(thisResourceId, ResourceStatus.IN_PROCESS);
                waitFor(promiseFor(activities.deleteResource(resourceId, stackId, accountId,
                  effectiveUserId))) { String result->
                  JsonNode jsonNodeResult = JsonHelper.getJsonNodeFromString(result);
                  String returnResourceId = jsonNodeResult.get("resourceId").textValue();
                  String returnResourceStatus = jsonNodeResult.get("status").textValue();
                  if (returnResourceStatus == "success") {
                    deletedResourceStatusMap.put(returnResourceId, ResourceStatus.COMPLETE);
                  }
                  deletedResourcePromiseMap.get(returnResourceId).chain(promiseFor(DeleteStackWorkflowImpl.EMPTY_JSON_NODE));
                }
              }
            }
            // Need to return a promise?  finally should kick off correctly (I think) if everything above is done,
            // let's make it an and promise...
            List<Promise<String>> allDeletedResources = Lists.newArrayList();
            allDeletedResources.addAll(deletedResourcePromiseMap.values());
            waitFor(allDeletedResources) {
              // Was everything done correctly?
              Collection<String> failedDeletedResources = Lists.newArrayList();
              for (String resourceName: deletedResourcePromiseMap.keySet()) {
                if (deletedResourceStatusMap.get(resourceName) == ResourceStatus.IN_PROCESS) {
                  failedDeletedResources.add(resourceName);
                }
              }
              if (!failedDeletedResources.isEmpty()) {
                return promiseFor(activities.createGlobalStackEvent(
                  stackId,
                  accountId,
                  StackResourceEntity.Status.DELETE_FAILED.toString(),
                  "The following resource(s) failed to delete: " + failedDeletedResources + "."));
              } else {
                return waitFor(promiseFor(activities.createGlobalStackEvent(
                  stackId,
                  accountId,
                  StackResourceEntity.Status.DELETE_COMPLETE.toString(),
                  ""))) {
                  promiseFor(activities.deleteAllStackRecords(stackId, accountId));
                }
              }
            }
          }.withCatch { Throwable t->
            String errorMessage = ((t != null) &&  (t.getMessage() != null) ? t.getMessage() : null);
            promiseFor(activities.createGlobalStackEvent(
              stackId,
              accountId,
              StackResourceEntity.Status.DELETE_FAILED.toString(),
              errorMessage));
          }.getResult()
        }
      } withCatch {
        Throwable t ->
          promiseFor(activities.logException(t));
      }
    } catch (Exception ex) {
      activities.logException(ex);
    }
  }
}
