package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.template.JsonHelper
import com.eucalyptus.cloudformation.template.Template
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import com.netflix.glisten.WorkflowOperations;
import com.netflix.glisten.impl.swf.SwfWorkflowOperations;
import groovy.lang.Delegate
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode;

/**
 * Created by ethomas on 2/18/14.
 */
@CompileStatic(TypeCheckingMode.SKIP)
public class CreateStackWorkflowImpl implements CreateStackWorkflow {
  @Delegate
  WorkflowOperations<StackActivity> workflowOperations = SwfWorkflowOperations.of(StackActivity);

  private enum ResourceStatus {
    NOT_STARTED,
    IN_PROCESS,
    COMPLETE
  };

  private static final String EMPTY_JSON_NODE = JsonHelper.getStringFromJsonNode(new ObjectMapper().createObjectNode());

  @Override
  public void createStack(String templateJson, String onFailure) {
    doTry {
      try {
        Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));

        Map<String, ResourceInfo> resourceInfoMap = Maps.newConcurrentMap();
        Map<String, Settable<String>> createdResourcePromiseMap = Maps.newConcurrentMap();
        Map<String, ResourceStatus> resourceStatusMap = Maps.newConcurrentMap();

        for (String resourceId: template.getResourceDependencyManager().getNodes()) {
          resourceStatusMap.put(resourceId, ResourceStatus.NOT_STARTED);
          createdResourcePromiseMap.put(resourceId, new Settable<String>());
        }
        // First create the Stack Event for the CREATE_STACK itself
        waitFor(promiseFor(activities.createGlobalStackEvent(templateJson,
          StackResourceEntity.Status.CREATE_IN_PROGRESS.toString(),
          "User Initiated"))) {
          doTry { // This is in case any part of setting up the stack fails
            // Now for each resource, set up the promises and the dependencies they have for each other
            for (String resourceId: template.getResourceDependencyManager().getNodes()) {
              Collection<Promise<String>> promisesDependedOn = Lists.newArrayList();
              for (String dependingResourceId: template.getResourceDependencyManager().getInEdges().get(resourceId)) {
                promisesDependedOn.add(createdResourcePromiseMap.get(dependingResourceId));
              }
              // Since we will use an "AndPromise" to depend on all of the dependent nodes and the AndPromise doesn't
              // have a return value (Promise<Vold>) and the dependent promises don't indicate which node we depend on,
              // we add one final promise to the and promise, that returns the node we are dealing with here.
              ObjectNode objectNode = new ObjectMapper().createObjectNode();
              objectNode.put("thisResourceId", resourceId);
              promisesDependedOn.add(promiseFor(JsonHelper.getStringFromJsonNode(objectNode)));
              AndPromise jointPromise = new AndPromise(promisesDependedOn);
              waitFor(jointPromise) {
                // at this point, all promises need to be complete
                String thisResourceId = null;
                for (Promise promise: jointPromise.getValues()) {
                  JsonNode jsonNode = JsonHelper.getJsonNodeFromString((String) promise.get());
                  if (jsonNode.has("thisResourceId")) {
                    thisResourceId = jsonNode.get("thisResourceId").textValue();
                  }
                } // the assumption here is that the global resourceInfo map is up to date...
                resourceStatusMap.put(thisResourceId, ResourceStatus.IN_PROCESS);
                String resourceInfoMapJson =  Template.resourceMapToJsonNode(resourceInfoMap);
                Promise<String> resourcePromise = promiseFor(
                  activities.createResource(thisResourceId, templateJson, resourceInfoMapJson)
                );
                activities.logInfo("CreateResourceResourceInfoMapJson="+resourceInfoMapJson)
                waitFor(resourcePromise) { String result->
                  activities.logInfo("result="+result);
                  JsonNode jsonNodeResult = JsonHelper.getJsonNodeFromString(result);
                  activities.logInfo("jsonNodeResult="+jsonNodeResult);
                  String returnResourceId = jsonNodeResult.get("resourceId").textValue();
                  activities.logInfo("returnResourceId="+returnResourceId);
                  activities.logInfo("jsonNodeResult.get(\"resourceInfo\")="+jsonNodeResult.get("resourceInfo"));
                  ResourceInfo resourceInfo = Template.jsonNodeToResourceInfo(jsonNodeResult.get("resourceInfo"));
                  resourceInfoMap.put(returnResourceId, resourceInfo);
                  resourceStatusMap.put(resourceId, ResourceStatus.COMPLETE);
                  // let any dependents know this is done.
                  createdResourcePromiseMap.get(returnResourceId).chain(promiseFor(CreateStackWorkflowImpl.EMPTY_JSON_NODE));
                }
              }
            }
            // Need to return a promise?  finally should kick off correctly (I think) if everything above is done,
            // let's make it an and promise...
            List<Promise<String>> allResources = Lists.newArrayList();
            allResources.addAll(createdResourcePromiseMap.values());
            waitFor(allResources) {
              String resourceInfoMapJson =  JsonHelper.getStringFromJsonNode(Template.resourceMapToJsonNode(resourceInfoMap));
              waitFor(promiseFor(activities.finalizeCreateStack(templateJson, resourceInfoMapJson))) {
                promiseFor(activities.createGlobalStackEvent(templateJson,
                  StackResourceEntity.Status.CREATE_COMPLETE.toString(),
                  "Complete!"));
              }
            }
          } withCatch { Throwable t->
            activities.logException(t);
            Collection<String> failedResources = Lists.newArrayList();
            for (String resourceName: resourceStatusMap.keySet()) {
              if (resourceStatusMap.get(resourceName) == ResourceStatus.IN_PROCESS) {
                failedResources.add(resourceName);
              }
            }
            String errorMessage = ((t != null) &&  (t.getMessage() != null) ? t.getMessage() : null);
            if (!failedResources.isEmpty()) {
              errorMessage = "The following resource(s) failed to create: " + failedResources + ".";
            }
            waitFor(promiseFor(activities.updateTemplate(templateJson,
              JsonHelper.getStringFromJsonNode(Template.resourceMapToJsonNode(resourceInfoMap))))) {
              if (onFailure == "DO_NOTHING") {
                promiseFor(activities.createGlobalStackEvent(templateJson,
                  StackResourceEntity.Status.CREATE_FAILED.toString(),
                  errorMessage));
              } else {
                doTry {
                  waitFor(promiseFor(activities.createGlobalStackEvent(templateJson,
                    onFailure == "ROLLBACK" ?
                      StackResourceEntity.Status.ROLLBACK_IN_PROGRESS.toString() :
                      StackResourceEntity.Status.DELETE_IN_PROGRESS.toString(),
                    errorMessage + (onFailure == "ROLLBACK" ? "Rollback" : "Delete") + " requested by user."
                  ))) {
                    // Determine correct order for rollback/delete
                    Map<String, Settable<String>> deletedResourcePromiseMap = Maps.newConcurrentMap();
                    Map<String, ResourceStatus> deletedResourceStatusMap = Maps.newConcurrentMap();

                    for (String resourceName: resourceStatusMap.keySet()) {
                      if (resourceStatusMap.get(resourceName) != ResourceStatus.NOT_STARTED) {
                        deletedResourceStatusMap.put(resourceName, ResourceStatus.NOT_STARTED);
                        deletedResourcePromiseMap.put(resourceName, new Settable<String>());
                      }
                    }
                    // Now for each resource, set up the promises and the dependencies they have for each other

                    for (String resourceId: deletedResourcePromiseMap.keySet()) {
                      Collection<Promise<String>> deletePromisesDependedOn = Lists.newArrayList();

                      for (String dependingResourceId:
                      // We have the opposite direction in delete than create, but we don't need to depend on
                      // resources we never created, so take the intersection of out edges and created resources
                      Sets.intersection(
                        Sets.newHashSet(template.getResourceDependencyManager().getOutEdges().get(resourceId)),
                        deletedResourcePromiseMap.keySet())) {
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
                        String resourceInfoMapJson =  Template.resourceMapToJsonNode(resourceInfoMap);
                        activities.logInfo("DeleteResourceResourceInfoMapJson="+resourceInfoMapJson)
                        waitFor(promiseFor(activities.deleteResource(thisResourceId, templateJson,
                          resourceInfoMapJson))) { String result->
                          JsonNode jsonNodeResult = JsonHelper.getJsonNodeFromString(result);
                          String returnResourceId = jsonNodeResult.get("resourceId").textValue();
                          String returnResourceStatus = jsonNodeResult.get("status").textValue();
                          if (returnResourceStatus == "success") {
                            deletedResourceStatusMap.put(returnResourceId, ResourceStatus.COMPLETE);
                          }
                          deletedResourcePromiseMap.get(returnResourceId).chain(promiseFor(CreateStackWorkflowImpl.EMPTY_JSON_NODE));
                          Promise.Void()
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
                      for (String resourceName: deletedResourceStatusMap.keySet()) {
                        if (deletedResourceStatusMap.get(resourceName) == ResourceStatus.IN_PROCESS) {
                          failedDeletedResources.add(resourceName);
                        }
                      }
                      if (!failedDeletedResources.isEmpty()) {
                        promiseFor(activities.createGlobalStackEvent(templateJson,
                          onFailure == "ROLLBACK" ?
                            StackResourceEntity.Status.ROLLBACK_FAILED.toString() :
                            StackResourceEntity.Status.DELETE_FAILED.toString(),
                          "The following resource(s) failed to delete: " + failedDeletedResources + "."));
                      } else if (onFailure == "ROLLBACK") {
                        promiseFor(activities.createGlobalStackEvent(templateJson,
                          StackResourceEntity.Status.ROLLBACK_COMPLETE.toString(),
                          ""));
                      } else {
                        waitFor(promiseFor(activities.createGlobalStackEvent(templateJson,
                          StackResourceEntity.Status.DELETE_COMPLETE.toString(),
                          ""))) {
                          promiseFor(activities.deleteAllStackRecords(templateJson));
                        }
                      }
                      Promise.Void() // wait for must return a promise
                    }
                  }
                } withCatch { Throwable t1 ->
                  String errorMessage1 = ((t1 != null) &&  (t1.getMessage() != null) ? t1.getMessage() : null);
                  promiseFor(activities.createGlobalStackEvent(templateJson,
                    onFailure == "ROLLBACK" ?
                      StackResourceEntity.Status.ROLLBACK_FAILED.toString() :
                      StackResourceEntity.Status.DELETE_FAILED.toString(),
                    errorMessage1));
                }
              }
              Promise.Void(); // waitFor() needs to return a promise
            }
          }
          Promise.Void(); // waitFor() needs to return a promise
        }
      } catch (Exception ex) {
        activities.logException(ex);
      }
      Promise.Void(); // need to return a promise from a doTry()
    } withCatch {
      Throwable t ->
        activities.logException(t);
        Promise.Void();
    }
    Promise.Void();
  }
}
