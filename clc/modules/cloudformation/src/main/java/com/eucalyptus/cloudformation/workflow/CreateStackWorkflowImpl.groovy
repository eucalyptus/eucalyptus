package com.eucalyptus.cloudformation.workflow

import com.amazonaws.services.simpleworkflow.flow.core.AndPromise
import com.amazonaws.services.simpleworkflow.flow.core.Promise
import com.amazonaws.services.simpleworkflow.flow.core.Settable
import com.eucalyptus.cloudformation.StackEvent;
import com.eucalyptus.cloudformation.TestSWF.TestTemplateActivity
import com.eucalyptus.cloudformation.entity.StackEventEntityManager
import com.eucalyptus.cloudformation.entity.StackResourceEntity
import com.eucalyptus.cloudformation.resources.ResourceInfo
import com.eucalyptus.cloudformation.template.JsonHelper
import com.eucalyptus.cloudformation.template.Template
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.google.common.collect.Multimap
import com.google.common.collect.Sets
import com.google.common.collect.TreeMultimap;
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

  @Override
  public void createStack(String templateJson) {

    Template template = Template.fromJsonNode(JsonHelper.getJsonNodeFromString(templateJson));
    Multimap<String, String> outEdges = template.getResourceDependencyManager().getOutEdges();
    Multimap<String, String> inEdges = TreeMultimap.create();
    for (String key: outEdges.keySet()) {
      for (String value: outEdges.get(key)) {
        inEdges.put(value, key);
      }
    }
    Set<String> createdResources = Sets.newHashSet();
    Map<String, Settable<String>> createResourcePromiseMap = Maps.newConcurrentMap();
    Map<String, Settable<String>> postCreateResourcePromiseMap = Maps.newConcurrentMap(); // in case we need to do a little more after the promise is done
    Map<String, ResourceInfo> resourceInfoMap = Maps.newConcurrentMap();
    String emptyResourceInfoMap = JsonHelper.getStringFromJsonNode(Template.resourceMapToJsonNode(new HashMap<String, ResourceInfo>()));
    String emptyObjectNodeJson = JsonHelper.getStringFromJsonNode(new ObjectMapper().createObjectNode());
    waitFor(activities.createInitialCreateStackEvent(templateJson)) {
      doTry {
        for (String resourceId: template.getResourceDependencyManager().getNodes()) {
          finishedCreateResourcePromiseMap.put(resourceId, new Settable<String>()); // placeholder
        }
        for (String resourceId: template.getResourceDependencyManager().getNodes()) {
          if (inEdges.get(resourceId) == null || inEdges.get(resourceId).isEmpty()) { // no dependencies
            Promise<String> resourcePromise = promiseFor(
              activities.createResource(resourceId, templateJson, emptyResourceInfoMap)
            );
            waitFor(resourcePromise) {
              String result->
              JsonNode jsonNodeResult = JsonHelper.getJsonNodeFromString(result);
              String returnResourceId = jsonNodeResult.get("resourceId").textValue();
              ResourceInfo resourceInfo = Template.jsonNodeToResourceMap(jsonNodeResult.get("resourceInfo"));
              resourceInfoMap.put(returnResourceId, resourceInfo);
              finishedCreateResourcePromiseMap.get(returnResourceId).chain(promiseFor(emptyObjectNodeJson)); // just lets other waitFors know this is done...
            }
          } else {
            Collection<Promise<String>> promisesDependedOn = Lists.newArrayList();
            for (String dependingResourceId: inEdges.get(resourceId)) {
              promisesDependedOn.add(finisedCreateResourcePromiseMap.get(dependingResourceId));
            }
            // Add one that indicates which node we are dealing with here (I don't trust external variables in closures)
            ObjectNode objectNode = new ObjectMapper().createObjectNode();
            objectNode.put("thisResourceId", resourceId);
            promisesDependedOn.add(promiseFor(JsonHelper.getStringFromJsonNode(objectNode)));
            AndPromise jointPromise = new AndPromise(promisesDependedOn);
            waitFor(jointPromise) {
              // at this point, all promises need to be complete
              String localResourceId = null;
              for (Promise promise: jointPromise.getValues()) {
                JsonNode jsonNode = JsonHelper.getJsonNodeFromString((String) promise.get());
                if (jsonNode.has("thisResourceId")) {
                  localResourceId = jsonNode.get("thisResourceId").textValue();
                }
              } // the assumption here is that the global resourceInfo map is up to date...
              String resourceInfoMapJson =  Template.resourceMapToJsonNode(resourceInfoMap);
              Promise<String> resourcePromise = promiseFor(
                activities.createResource(localResourceId, templateJson, resourceInfoMapJson)
              );
              createResourcePromiseMap.get(resourceId).chain(resourcePromise);
              waitFor(resourcePromise) {
                String result->
                JsonNode jsonNodeResult = JsonHelper.getJsonNodeFromString(result);
                String returnResourceId = jsonNodeResult.get("resourceId").textValue();
                ResourceInfo resourceInfo = Template.jsonNodeToResourceMap(jsonNodeResult.get("resourceInfo"));
                resourceInfoMap.put(returnResourceId, resourceInfo);
                createdResources.add(returnResourceId);
                postCreateResourcePromiseMap.get(returnResourceId).chain(promiseFor(emptyObjectNodeJson)); // just lets other waitFors know this is done...
              }
            }
          }
        }
        // Need to return a promise?  finally should kick off correctly (I think) if everything above is done,
        // let's make it an and promise...
        List<Promise<String>> allResources = Lists.newArrayList();
        allResources.addAll(postCreateResourcePromiseMap.values());
        return new AndPromise(allResources); // Not sure if necessary...
      } withCatch {
//        Throwable t->
//        activities.
//        // update template here regardless
//        activities.updateTemplate();
//        // how do we know something good happened?
//
//        // set status, maybe delete, maybe rollback, depends on template

      } withFinally {
//        // update template here regardless
//        activities.updateTemplate();
//        // how do we know something good happened?
      }
    }
  }
}
