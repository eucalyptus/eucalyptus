/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.cloudformation.common.CloudFormation;
import com.eucalyptus.cloudformation.common.CloudFormationApi;
import com.eucalyptus.cloudformation.common.msgs.Capabilities;
import com.eucalyptus.cloudformation.common.msgs.CreateStackType;
import com.eucalyptus.cloudformation.common.msgs.DeleteStackType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksResponseType;
import com.eucalyptus.cloudformation.common.msgs.DescribeStacksType;
import com.eucalyptus.cloudformation.common.msgs.Parameter;
import com.eucalyptus.cloudformation.common.msgs.Parameters;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.AttachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.AttachNetworkInterfaceType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.CreateNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.CreateNetworkInterfaceType;
import com.eucalyptus.compute.common.CreateTagsResponseType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DeleteNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DeleteNetworkInterfaceType;
import com.eucalyptus.compute.common.DeleteTagsResponseType;
import com.eucalyptus.compute.common.DeleteTagsType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesResponseType;
import com.eucalyptus.compute.common.DescribeNetworkInterfacesType;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceResponseType;
import com.eucalyptus.compute.common.DetachNetworkInterfaceType;
import com.eucalyptus.compute.common.Filter;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttachmentType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeResponseType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttributeType;
import com.eucalyptus.compute.common.ReservationInfoType;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.RunningInstancesItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetItemType;
import com.eucalyptus.compute.common.SecurityGroupIdSetType;
import com.eucalyptus.entities.PersistenceExceptions;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.EventListener;
import com.eucalyptus.event.Listeners;
import com.eucalyptus.event.SystemClock;
import com.eucalyptus.loadbalancing.LoadBalancingSystemVpcs;
import com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingWorkflows;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataException;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancingv2.service.persist.entities.PersistenceLoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.views.ImmutableLoadBalancerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerView;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.ThrowingFunction;
import com.eucalyptus.util.async.AsyncProxy;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;

public class Loadbalancingv2Workflow {

  private static final Logger logger = Logger.getLogger(Loadbalancingv2Workflow.class);

  private final LoadBalancers loadBalancers;

  private final List<WorkflowTask> workflowTasks = ImmutableList.<WorkflowTask>builder()
      .add(new WorkflowTask( 10, "ELB.Provision")      {@Override void doWork() { loadBalancersProvision(); }})
      .add(new WorkflowTask( 10, "ELB.Track")          {@Override void doWork() { loadBalancersTrack(); }})
      .add(new WorkflowTask( 10, "ELB.Delete")         {@Override void doWork() { loadBalancersDelete(); }})
      .add(new WorkflowTask(300, "ELB.Timeout")        {@Override void doWork() { loadBalancersTimeout(); }})
      .build();

  private final CompatFunction<String,String> loadbalancingAccountLookup =
      FUtils.memoizeLast( ThrowingFunction.undeclared( Accounts::lookupAccountIdByAlias ) );

  private final Function<BaseMessage,BaseMessage> loadBalancingAuthTransform = ThrowingFunction.undeclared(request -> {
    request.setUserId(loadbalancingAccountLookup.apply(AccountIdentifiers.ELB_SYSTEM_ACCOUNT));
    request.markPrivileged();
    return request;
  });

  public Loadbalancingv2Workflow(
      final LoadBalancers loadBalancers
  ) {
    this.loadBalancers = loadBalancers;
  }

  private void doWorkflow() {
    for (final WorkflowTask workflowTask : workflowTasks) {
      try {
        workflowTask.perhapsWork();
      } catch (Exception e) {
        logger.error(e, e);
      }
    }
  }

  private List<String> listLoadBalancerIds(final LoadBalancer.State state) {
    List<String> loadBalancerIds = Collections.emptyList();
    try {
      loadBalancerIds = loadBalancers.listByExample(
          LoadBalancer.exampleWithState(state),
          Predicates.alwaysTrue(),
          LoadBalancer::getNaturalId);
    } catch (final Loadbalancingv2MetadataException e) {
      logger.error("Error listing load balancer instances", e);
    }
    return loadBalancerIds;
  }

  private LoadBalancerView lookupLoadBalancerById(final String id) {
    try {
     return loadBalancers.lookupByExample(
          LoadBalancer.exampleWithId(id),
          null,
          id,
          Predicates.alwaysTrue(),
          ImmutableLoadBalancerView::copyOf);
    } catch (final Loadbalancingv2MetadataException e) {
      logger.error("Error looking up load balancer", e);
      throw Exceptions.toUndeclared(e);
    }
  }

  private void loadBalancersProvision() {
    for (final String loadBalancerId : listLoadBalancerIds(LoadBalancer.State.provisioning)) {
      final LoadBalancerView loadBalancer;
      try {
        loadBalancer = lookupLoadBalancerById(loadBalancerId);
      } catch (final Exception e) {
        logger.error("Error provisioning load balancer " + loadBalancerId, e);
        continue;
      }

      final List<String> subnetIds = loadBalancer.getSubnetIds();
      if (subnetIds.isEmpty()) {
        loadBalancerSetupFailure(loadBalancer, "No subnets");
        continue;
      }

      final String userSubnetId = subnetIds.get(0);
      final String subnetId = LoadBalancingSystemVpcs.getSystemVpcSubnetId(userSubnetId);
      final String securityGroupId = LoadBalancingSystemVpcs.getSecurityGroupId(subnetId);
      final String imageId = LoadBalancingWorkerProperties.IMAGE;
      final String instanceType = LoadBalancingWorkerProperties.INSTANCE_TYPE;
      final String keyName =
          Strings.emptyToNull(Strings.nullToEmpty(LoadBalancingWorkerProperties.KEYNAME).trim());
      final String servoEucalyptusHost = Internets.localHostAddress();
      final String servoEucalyptusPort = Objects.toString(StackConfiguration.PORT, "8773");
      final String servoNtpHost = LoadBalancingWorkerProperties.NTP_SERVER;
      final String servoAccountNumber = loadBalancer.getOwnerAccountNumber();
      
      final CloudFormationApi cf = AsyncProxy.client(CloudFormationApi.class,
          loadBalancingAuthTransform, () -> Topology.lookup(CloudFormation.class));

      final String stackName = getStackName(loadBalancer, null);
      final Option<String> stackStatusOption =
          stackStatus(cf.describeStacks(describeStacksMessage(stackName)));
      if (stackStatusOption.isEmpty()) {
        final String template = getTemplate(loadBalancer);
        final Map<String, String> parameters = Maps.newTreeMap();
        parameters.put("ImageId", imageId);
        parameters.put("InstanceType", instanceType);
        parameters.put("SecurityGroupId", securityGroupId);
        parameters.put("SubnetId", subnetId);
        parameters.put("KeyName", keyName);
        parameters.put("ServoEucalyptusHost", servoEucalyptusHost);
        parameters.put("ServoEucalyptusPort", servoEucalyptusPort);
        parameters.put("ServoNtpHost", servoNtpHost);
        parameters.put("ServoAccountNumber", servoAccountNumber);
        parameters.put("ServoLoadBalancerId", loadBalancerId);
        cf.createStack(createStackMessage(stackName, template, parameters));
        continue; // check for stack complete next time
      } else if (stackStatusOption.get().endsWith("_FAILED")) {
        loadBalancerSetupFailure(loadBalancer, "Stack failed");
        continue;
      } else if (!stackStatusOption.get().equals("CREATE_COMPLETE")) {
        continue; // check for stack complete next time
      }

      LoadBalancingWorkflows.updateLoadBalancer(
          loadBalancer.getOwnerAccountNumber(),
          loadBalancer.getDisplayName());

      try {
        loadBalancers.updateByView(loadBalancer, Predicates.alwaysTrue(), balancer -> {
          balancer.setState(LoadBalancer.State.active);
          return null;
        });
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict provisioning load balancer " + loadBalancerId + " (will retry)");
        } else {
          logger.error("Error provisioning load balancer " + loadBalancerId, e);
        }
      }
    }
  }

  /**
   * Track servo instances and configure them
   */
  private void loadBalancersTrack() {
    final LoadBalancingComputeApi ec2;
    final DescribeInstancesResponseType describeInstancesResponse;
    try {
      ec2 = AsyncProxy.client(LoadBalancingComputeApi.class,
          loadBalancingAuthTransform, () -> Topology.lookup(Compute.class));
      final Map<String, String> filters = Maps.newHashMap();
      filters.put("instance-state-name", "running");
      filters.put("tag-key", "loadbalancer-id");
      filters.put("tag:user-interface-status", "pending");
      describeInstancesResponse = ec2.describeInstances(describeInstancesMessage(filters));
    } catch (final Exception e) {
      logger.warn("Error describing instances to setup load balancer servos: " + e.getMessage());
      return;
    }

    for (final ReservationInfoType reservationInfo : describeInstancesResponse.getReservationSet()) {
      for (final RunningInstancesItemType runningInstance : reservationInfo.getInstancesSet()) {
        final String instanceId = runningInstance.getInstanceId();
        String loadBalancerId = "<none>";
        try {
          // check for user interface
          String networkInterfaceId = null;
          String attachmentId = null;
          boolean hasUserNetworkInterface = false;
          for (final InstanceNetworkInterfaceSetItemType networkInterface : runningInstance.getNetworkInterfaceSet().getItem()) {
            if (1 == networkInterface.getAttachment().getDeviceIndex()) {
              hasUserNetworkInterface = true;
              if (!networkInterface.getAttachment().getDeleteOnTermination()) {
                networkInterfaceId = networkInterface.getNetworkInterfaceId();
                attachmentId = networkInterface.getAttachment().getAttachmentId();
              }
              break;
            }
          }

          if (!hasUserNetworkInterface) {
            logger.info("Creating user network interface for servo instance: " + instanceId);

            for (final ResourceTag tag : runningInstance.getTagSet()) {
              if ("loadbalancer-id".equals(tag.getKey())) {
                loadBalancerId = Objects.toString(tag.getValue(), loadBalancerId);
              }
            }
            final LoadBalancerView loadBalancer = lookupLoadBalancerById(loadBalancerId);
            final List<String> subnetId = loadBalancer.getSubnetIds();
            final List<String> securityGroupIds = loadBalancer.getSecurityGroupIds();

            networkInterfaceId =
                ec2.createNetworkInterface(createNetworkInterfaceMessage(subnetId.get(0), securityGroupIds))
                    .getNetworkInterface().getNetworkInterfaceId();

            attachmentId = ec2.attachNetworkInterface(
                attachNetworkInterfaceMessage(instanceId, 0, networkInterfaceId)).getAttachmentId();
          }

          if (attachmentId != null) {
            logger.info("Modifying user network interface for servo instance: " + instanceId);

            ec2.modifyNetworkInterfaceAttribute(
                modifyNetworkInterfaceAttributeMessage(networkInterfaceId, attachmentId, true));
          }

          // update tag to record attachment complete
          final Map<String,String> tags = Maps.newHashMap();
          tags.put("user-interface-status", "attached");
          ec2.createTags(createTagsMessage(instanceId, tags));
        } catch (final Exception e) {
          logger.warn("Error in interface setup for load balancer " + loadBalancerId, e);
        }
      }
    }
  }

  private Option<String> stackStatus(final DescribeStacksResponseType response) {
    Option<String> status = Option.none();
    if (response.getDescribeStacksResult().getStacks() != null &&
        response.getDescribeStacksResult().getStacks().getMember().size()==1) {
      status = Option.of(response.getDescribeStacksResult().getStacks().getMember().get(0).getStackStatus());
    }
    return status;
  }

  private DescribeStacksType describeStacksMessage(final String name) {
    final DescribeStacksType describeStacks = new DescribeStacksType();
    describeStacks.setStackName(name);
    return describeStacks;
  }

  private CreateStackType createStackMessage(
      final String name,
      final String body,
      final Map<String,String> parameterMap
  ) {
    final CreateStackType createStack = new CreateStackType();
    createStack.setStackName(name);
    createStack.setTemplateBody(body);
    final Parameters parameters = new Parameters();
    Stream.ofAll(parameterMap.entrySet())
        .map(entry -> new Parameter(entry.getKey(), entry.getValue()))
        .forEach(parameters.getMember()::add);
    createStack.setParameters(parameters);
    final Capabilities capabilities = new Capabilities();
    capabilities.getMember().add("CAPABILITY_IAM");
    createStack.setCapabilities(capabilities);
    createStack.setDisableRollback(true);
    return createStack;
  }

  private DeleteStackType deleteStackMessage(final String name) {
    final DeleteStackType deleteStack = new DeleteStackType();
    deleteStack.setStackName(name);
    return deleteStack;
  }

  private DescribeInstancesType describeInstancesMessage(final Map<String,String> filters) {
    final DescribeInstancesType describeInstancesType = new DescribeInstancesType();
    Stream.ofAll(filters.entrySet())
        .map(entry -> {
          final Filter filter = new Filter();
          filter.setName(entry.getKey());
          filter.setValueSet(Lists.newArrayList(entry.getValue()));
          return filter;
        })
        .forEach(describeInstancesType.getFilterSet()::add);
    return describeInstancesType;
  }

  private CreateNetworkInterfaceType createNetworkInterfaceMessage(
      final String subnetId,
      final List<String> securityGroupIds
  ) {
    final CreateNetworkInterfaceType createNetworkInterface = new CreateNetworkInterfaceType();
    createNetworkInterface.setSubnetId(subnetId);
    final SecurityGroupIdSetType groupSet = new SecurityGroupIdSetType();
    Stream.ofAll(securityGroupIds)
        .map(SecurityGroupIdSetItemType.forGroupId())
        .forEach(groupSet.getItem()::add);
    createNetworkInterface.setGroupSet(groupSet);
    return createNetworkInterface;
  }

  private AttachNetworkInterfaceType attachNetworkInterfaceMessage(
      final String instanceId,
      final int deviceIndex,
      final String networkInterfaceId
  ) {
    final AttachNetworkInterfaceType attachNetworkInterface = new AttachNetworkInterfaceType();
    attachNetworkInterface.setInstanceId(instanceId);
    attachNetworkInterface.setDeviceIndex(deviceIndex);
    attachNetworkInterface.setNetworkInterfaceId(networkInterfaceId);
    return attachNetworkInterface;
  }

  private ModifyNetworkInterfaceAttributeType modifyNetworkInterfaceAttributeMessage(
      final String networkInterfaceId,
      final String attachmentId,
      final boolean deleteOnTerminate
  ) {
    final ModifyNetworkInterfaceAttributeType modifyNetworkInterfaceAttribute =
        new ModifyNetworkInterfaceAttributeType();
    modifyNetworkInterfaceAttribute.setNetworkInterfaceId(networkInterfaceId);
    final ModifyNetworkInterfaceAttachmentType modifyNetworkInterfaceAttachment =
        new ModifyNetworkInterfaceAttachmentType();
    modifyNetworkInterfaceAttachment.setAttachmentId(attachmentId);
    modifyNetworkInterfaceAttachment.setDeleteOnTermination(deleteOnTerminate);
    modifyNetworkInterfaceAttribute.setAttachment(modifyNetworkInterfaceAttachment);
    return modifyNetworkInterfaceAttribute;
  }

  private CreateTagsType createTagsMessage(final String resourceId, final Map<String,String> tags) {
    final CreateTagsType createTags = new CreateTagsType();
    createTags.getResourcesSet().add(resourceId);
    Stream.ofAll(tags.entrySet())
        .map(entry -> new ResourceTag(entry.getKey(), entry.getValue()))
        .forEach(createTags.getTagSet()::add);
    return createTags;
  }

  private String getStackName(final LoadBalancerView loadBalancer, final String zone) {  //TODO:STEVE: zone support
    final String userAccount = loadBalancer.getOwnerAccountNumber();
    final String name = loadBalancer.getDisplayName();
    return "loadbalancer-" + loadBalancer.getType() + "-" + userAccount + "-" + name;
  }

  private String getTemplate(final LoadBalancerView loadBalancer) {
    try {
      return Resources.toString(
          Resources.getResource(Loadbalancingv2Workflow.class,
              "loadbalancer-" + loadBalancer.getType() + "-zone.yaml"),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw Exceptions.toUndeclared(e);
    }
  }

  private void loadBalancersDelete() {
    for (final String loadBalancerId : listLoadBalancerIds(LoadBalancer.State.deleted)) {
      final LoadBalancerView loadBalancer;
      try {
        loadBalancer = lookupLoadBalancerById(loadBalancerId);
      } catch (final Exception e) {
        logger.error("Error processing load balancer delete " + loadBalancerId, e);
        continue;
      }

      if (!loadBalancerReleaseResources(loadBalancer)) {
        continue;
      }

      try {
        loadBalancers.delete(LoadBalancer.exampleWithId(loadBalancerId));
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict deleting load balancer " + loadBalancerId + " (will retry)");
        } else {
          logger.error("Error processing load balancer delete " + loadBalancerId, e);
        }
      }
    }
  }

  private void loadBalancerSetupFailure(
      final LoadBalancerView loadBalancer,
      final String reason
  ) {
    try {
      logger.info(
          "Marking load balancer " + loadBalancer.getDisplayName() + " failed due to " + reason);
      loadBalancers.updateByView(loadBalancer, Predicates.alwaysTrue(), instance -> {
        instance.setState(LoadBalancer.State.failed);
        return null;
      });
    } catch (final Exception e) {
      logger.error("Error marking load balancer failed", e);
    }
  }

  private boolean loadBalancerReleaseResources(
      final LoadBalancerView loadBalancer
  ) {
    boolean releasedAll = true; //TODO:STEVE: track resource cleanup

    logger.debug("Attempting resources release for load balancer " + loadBalancer.getDisplayName());

    LoadBalancingWorkflows.cancelUpdateLoadBalancer(loadBalancer.getOwnerAccountNumber(), loadBalancer.getDisplayName());

    final CloudFormationApi cf = AsyncProxy.client(CloudFormationApi.class,
        loadBalancingAuthTransform, () -> Topology.lookup(CloudFormation.class));
    cf.deleteStack(deleteStackMessage(getStackName(loadBalancer, null)));

    return releasedAll;
  }

  private void loadBalancersTimeout() {
    List<LoadBalancerView> timedOutLoadBalancers = Collections.emptyList();
    try {
      timedOutLoadBalancers = loadBalancers.list(
          null,
          Restrictions.and(
              Restrictions.or(
                  Example.create(LoadBalancer.exampleWithState(LoadBalancer.State.provisioning)),
                  Example.create(LoadBalancer.exampleWithState(LoadBalancer.State.failed))
              ),
              Restrictions.lt("lastUpdateTimestamp",
                  new Date(System.currentTimeMillis() - LoadBalancers.EXPIRY_AGE))
          ),
          Predicates.alwaysTrue(),
          ImmutableLoadBalancerView::copyOf
      );
    } catch (final Exception e) {
      logger.error("Error listing timed out load balancers", e);
    }

    for (final LoadBalancerView loadBalancer : timedOutLoadBalancers) {
      try {
        if (loadBalancer.getState() == LoadBalancer.State.provisioning) {
          logger.info("Marking load balancer "
              + loadBalancer.getDisplayName()
              + " failed from state "
              + loadBalancer.getState());
          loadBalancers.updateByView(loadBalancer, Predicates.alwaysTrue(), balancer -> {
            balancer.setState(LoadBalancer.State.failed);
            return null;
          });
          loadBalancerReleaseResources(loadBalancer);
        }
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict handling timeout for load balancer "
              + loadBalancer.getDisplayName()
              + " (will retry)");
        } else {
          logger.error("Error processing timeout for load balancer " + loadBalancer.getDisplayName(),
              e);
        }
      }
    }
  }

  private interface LoadBalancingComputeApi {
    DescribeInstancesResponseType describeInstances(DescribeInstancesType request);

    CreateNetworkInterfaceResponseType createNetworkInterface(CreateNetworkInterfaceType request);
    AttachNetworkInterfaceResponseType attachNetworkInterface(AttachNetworkInterfaceType request);
    DetachNetworkInterfaceResponseType detachNetworkInterface(DetachNetworkInterfaceType request);
    DeleteNetworkInterfaceResponseType deleteNetworkInterface(DeleteNetworkInterfaceType request);
    DescribeNetworkInterfacesResponseType describeNetworkInterfaces(DescribeNetworkInterfacesType request);
    ModifyNetworkInterfaceAttributeResponseType modifyNetworkInterfaceAttribute(ModifyNetworkInterfaceAttributeType request);

    CreateTagsResponseType createTags(CreateTagsType request);
    DeleteTagsResponseType deleteTags(DeleteTagsType request);
    DescribeTagsResponseType describeTags(DescribeTagsType request);
  }

  private static abstract class WorkflowTask {

    private volatile int count = 0;

    private final int factor;

    private final String task;

    protected WorkflowTask(final int factor, final String task) {
      this.factor = factor;
      this.task = task;
    }

    protected final int calcFactor() {
      return factor / (int) Math.max(1, SystemClock.RATE / 1000);
    }

    protected final void perhapsWork() throws Exception {
      if (++count % calcFactor() == 0) {
        logger.trace("Running ELBv2 workflow task: " + task);
        doWork();
        logger.trace("Completed ELBv2 workflow task: " + task);
      }
    }

    abstract void doWork() throws Exception;
  }

  public static class Loadbalancingv2WorkflowEventListener implements EventListener<ClockTick> {

    private final Loadbalancingv2Workflow loadbalancingv2Workflow = new Loadbalancingv2Workflow(
        new PersistenceLoadBalancers()
    );

    public static void register() {
      Listeners.register(ClockTick.class, new Loadbalancingv2WorkflowEventListener());
    }

    @Override
    public void fireEvent(final ClockTick event) {
      if (Bootstrap.isOperational() &&
          Topology.isEnabledLocally(Eucalyptus.class) &&
          Topology.isEnabled(Compute.class) &&
          Topology.isEnabled(CloudFormation.class) &&
          Topology.isEnabled(Loadbalancingv2.class)) {
        loadbalancingv2Workflow.doWorkflow();
      }
    }
  }
}
