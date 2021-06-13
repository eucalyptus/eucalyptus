/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.loadbalancingv2.service;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.AccountFullName;
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
import com.eucalyptus.cloudformation.common.msgs.UpdateStackType;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.compute.common.AddressInfoType;
import com.eucalyptus.compute.common.AllocateAddressResponseType;
import com.eucalyptus.compute.common.AssociateAddressType;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.ComputeApi;
import com.eucalyptus.compute.common.CreateNetworkInterfaceType;
import com.eucalyptus.compute.common.CreateTagsType;
import com.eucalyptus.compute.common.DescribeAddressesResponseType;
import com.eucalyptus.compute.common.DescribeInstancesResponseType;
import com.eucalyptus.compute.common.InstanceNetworkInterfaceSetItemType;
import com.eucalyptus.compute.common.ModifyNetworkInterfaceAttachmentType;
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
import com.eucalyptus.loadbalancing.LoadBalancerHelper;
import com.eucalyptus.loadbalancing.LoadBalancingSystemVpcs;
import com.eucalyptus.loadbalancing.LoadBalancingWorkerProperties;
import com.eucalyptus.loadbalancing.workflow.LoadBalancingWorkflows;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.LoadBalancingAttribute.Attribute;
import com.eucalyptus.loadbalancingv2.service.persist.Loadbalancingv2MetadataException;
import com.eucalyptus.loadbalancingv2.service.persist.entities.LoadBalancer;
import com.eucalyptus.loadbalancingv2.service.persist.entities.PersistenceLoadBalancers;
import com.eucalyptus.loadbalancingv2.service.persist.views.ImmutableLoadBalancerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.ListenerView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerListenersView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerSubnetView;
import com.eucalyptus.loadbalancingv2.service.persist.views.LoadBalancerView;
import com.eucalyptus.loadbalancingv2.service.servo.ServoCache;
import com.eucalyptus.loadbalancingv2.service.servo.SwitchedServoMetadataSource;
import com.eucalyptus.util.CompatFunction;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.FUtils;
import com.eucalyptus.util.Internets;
import com.eucalyptus.util.ThrowingFunction;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.eucalyptus.ws.StackConfiguration;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.common.primitives.Longs;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Restrictions;

@SuppressWarnings({"Guava", "UnstableApiUsage", "unused"})
public class Loadbalancingv2Workflow {

  private static final Logger logger = Logger.getLogger(Loadbalancingv2Workflow.class);

  private static final long DEFAULT_UPDATE_RETRY_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
  private static final long UPDATE_RETRY_TIMEOUT = MoreObjects.firstNonNull(
      Longs.tryParse(System.getProperty(
          "com.eucalyptus.loadbalancingv2.workflowUpdateRetryTimeoutMillis",
          String.valueOf(DEFAULT_UPDATE_RETRY_TIMEOUT))),
      DEFAULT_UPDATE_RETRY_TIMEOUT);

  private final LoadBalancers loadBalancers;
  private final AtomicBoolean loaded = new AtomicBoolean(false);

  private final List<WorkflowTask> workflowTasks = ImmutableList.<WorkflowTask>builder()
      .add(new WorkflowTask( 10, "ELB.Provision")      {@Override void doWork() { loadBalancersProvision(); }})
      .add(new WorkflowTask( 10, "ELB.Update")         {@Override void doWork() { loadBalancersUpdate(); }})
      .add(new WorkflowTask( 10, "ELB.Load")           {@Override void doWork() { loadBalancersLoad(); }})
      .add(new WorkflowTask( 10, "ELB.Track")          {@Override void doWork() { loadBalancersTrack(); }})
      .add(new WorkflowTask( 10, "ELB.Delete")         {@Override void doWork() { loadBalancersDelete(); }})
      .add(new WorkflowTask( 30, "ELB.ReleaseAddress") {@Override void doWork() { addressesRelease(); }})
      .add(new WorkflowTask( 60, "ELB.Workflow")       {@Override void doWork() { loadBalancersWorkflow(); }})
      .add(new WorkflowTask(300, "ELB.Timeout")        {@Override void doWork() { loadBalancersTimeout(); }})
      .build();

  private final CompatFunction<String,String> loadbalancingAccountLookup =
      FUtils.memoizeLast( ThrowingFunction.undeclared( Accounts::lookupAccountIdByAlias ) );

  private final Function<BaseMessage,BaseMessage> loadBalancingAuthTransform = ThrowingFunction.undeclared(request -> {
    //noinspection deprecation
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

  private void halted() {
    loaded.set(false);
  }

  private List<String> listLoadBalancerIds(final LoadBalancer.State state) {
    return listLoadBalancerIds(state, null);
  }

  private List<String> listLoadBalancerIds(final LoadBalancer.State state, final Boolean updateRequired) {
    List<String> loadBalancerIds = Collections.emptyList();
    try {
      loadBalancerIds = loadBalancers.listByExample(
          LoadBalancer.exampleWithState(state, updateRequired),
          Predicates.alwaysTrue(),
          LoadBalancer::getNaturalId);
    } catch (final Loadbalancingv2MetadataException e) {
      logger.error("Error listing load balancer instances", e);
    }
    return loadBalancerIds;
  }

  private LoadBalancerView lookupLoadBalancerById(final String id) {
    return lookupLoadBalancerById(id, ImmutableLoadBalancerView::copyOf);
  }

  private <T> T lookupLoadBalancerById(
      final String id,
      final CompatFunction<LoadBalancer, T> transform) {
    try {
      return loadBalancers.lookupByExample(
          LoadBalancer.exampleWithId(id),
          null,
          id,
          Predicates.alwaysTrue(),
          transform);
    } catch (final Loadbalancingv2MetadataException e) {
      logger.error("Error looking up load balancer", e);
      throw Exceptions.toUndeclared(e);
    }
  }

  private Map<String,String> getStackParameters(
      final LoadBalancerView loadBalancer,
      final Map<String,String> loadBalancerAttributes,
      final LoadBalancerSubnetView subnet,
      final List<ListenerView> listeners
  ) {
    final String userSubnetId = subnet.getSubnetId();
    final String subnetId = LoadBalancingSystemVpcs.getSystemVpcSubnetId(userSubnetId);
    final String securityGroupId = LoadBalancingSystemVpcs.getSecurityGroupId(subnetId);
    final String serverCertifcateArns = Stream.ofAll(listeners)
        .map(ListenerView::getDefaultServerCertificateArn)
        .filter(Objects::nonNull)
        .intersperse(",").fold("", String::concat);
    final String s3BucketArns = buildBucketArnsParameter(loadBalancerAttributes);
    final String imageId = LoadBalancingWorkerProperties.IMAGE;
    final String instanceType = LoadBalancingWorkerProperties.INSTANCE_TYPE;
    final String keyName =
        Strings.emptyToNull(Strings.nullToEmpty(LoadBalancingWorkerProperties.KEYNAME).trim());
    final String servoEucalyptusHost = Internets.localHostAddress();
    final String servoEucalyptusPort = Objects.toString(StackConfiguration.PORT, "8773");
    final String servoNtpHost = LoadBalancingWorkerProperties.NTP_SERVER;
    final String servoAccountNumber = loadBalancer.getOwnerAccountNumber();

    final Map<String, String> parameters = Maps.newTreeMap();
    parameters.put("ImageId", imageId);
    parameters.put("InstanceType", instanceType);
    parameters.put("SecurityGroupId", securityGroupId);
    parameters.put("ServerCertificateArns", serverCertifcateArns);
    parameters.put("SubnetId", subnetId);
    parameters.put("S3BucketArns", s3BucketArns);
    parameters.put("KeyName", keyName);
    parameters.put("ServoEucalyptusHost", servoEucalyptusHost);
    parameters.put("ServoEucalyptusPort", servoEucalyptusPort);
    parameters.put("ServoNtpHost", servoNtpHost);
    parameters.put("ServoAccountNumber", servoAccountNumber);
    parameters.put("ServoLoadBalancerId", loadBalancer.getNaturalId());
    return parameters;
  }

  private String buildBucketArnsParameter(final Map<String,String> attributes) {
    String parameter = "";
    if (Attribute.LoadBalancerAccessLogsS3Enabled.booleanValue(attributes)) {
      final String bucket = Attribute.LoadBalancerAccessLogsS3Bucket.stringValue(attributes);
      final String prefix = Attribute.LoadBalancerAccessLogsS3Prefix.stringValue(attributes);
      if (bucket != null && !bucket.isEmpty()) {
        if (prefix != null && !prefix.isEmpty()) {
          parameter = String.format("arn:aws:s3:::%s/%s/*", bucket, prefix);
        } else {
          parameter = String.format("arn:aws:s3:::%s/*", bucket);
        }
      }
    }
    return parameter;
  }

  private void loadBalancersProvision() {
    for (final String loadBalancerId : listLoadBalancerIds(LoadBalancer.State.provisioning)) {
      final LoadBalancerView loadBalancer;
      final Map<String,String> loadBalancerAttributes;
      final List<ListenerView> listeners;
      try {
        final LoadBalancerListenersView loadBalancerWithListeners =
            lookupLoadBalancerById(loadBalancerId, LoadBalancers.LISTENERS_VIEW);
        loadBalancer = loadBalancerWithListeners.getLoadBalancer();
        loadBalancerAttributes = loadBalancerWithListeners.getLoadBalancerAttributes();
        listeners = loadBalancerWithListeners.getListeners();
      } catch (final Exception e) {
        logger.error("Error provisioning load balancer " + loadBalancerId, e);
        continue;
      }

      final List<LoadBalancerSubnetView> subnets = loadBalancer.getSubnetViews();
      if (subnets.isEmpty()) {
        loadBalancerSetupFailure(loadBalancer, "No subnets");
        continue;
      }

      final CloudFormationApi cf = AsyncProxy.client(CloudFormationApi.class,
          loadBalancingAuthTransform, () -> Topology.lookup(CloudFormation.class));

      final Set<String> allZones = Sets.newHashSet();
      final Set<String> completedZones = Sets.newHashSet();
      for (final LoadBalancerSubnetView subnet : subnets) {
        allZones.add(subnet.getAvailabilityZone());
        final String stackName = getStackName(loadBalancer, subnet.getAvailabilityZone());
        final Option<String> stackStatusOption =
            stackStatus(cf.describeStacks(describeStacksMessage(stackName)));
        if (stackStatusOption.isEmpty()) {
          final String template = getTemplate(loadBalancer);
          final Map<String, String> parameters =
              getStackParameters(loadBalancer, loadBalancerAttributes, subnet, listeners);
          cf.createStack(createStackMessage(stackName, template, parameters));
        } else if (stackStatusOption.get().endsWith("_FAILED")) {
          loadBalancerSetupFailure(loadBalancer, "Stack failed");
        } else if (stackStatusOption.get().equals("CREATE_COMPLETE")) {
          completedZones.add(subnet.getAvailabilityZone());
        }
      }
      if (!completedZones.containsAll(allZones)) continue;

      LoadBalancingWorkflows.runUpdateLoadBalancer(
          loadBalancer.getOwnerAccountNumber(),
          loadBalancer.getDisplayName(),
          loadBalancer.getArn());

      try {
        loadBalancers.updateByView(loadBalancer, Predicates.alwaysTrue(), balancer -> {
          //noinspection ConstantConditions
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

  private void loadBalancersUpdate() {
    for (final String loadBalancerId : listLoadBalancerIds(LoadBalancer.State.active, true)) {
      final LoadBalancerView loadBalancer;
      final Map<String,String> loadBalancerAttributes;
      final List<ListenerView> listeners;
      try {
        final LoadBalancerListenersView loadBalancerWithListeners =
            lookupLoadBalancerById(loadBalancerId, LoadBalancers.LISTENERS_VIEW);
        loadBalancer = loadBalancerWithListeners.getLoadBalancer();
        loadBalancerAttributes = loadBalancerWithListeners.getLoadBalancerAttributes();
        listeners = loadBalancerWithListeners.getListeners();
      } catch (final Exception e) {
        logger.error("Error updating load balancer " + loadBalancerId, e);
        continue;
      }

      final List<LoadBalancerSubnetView> subnets = loadBalancer.getSubnetViews();
      if (subnets.isEmpty()) {
        continue;
      }

      final CloudFormationApi cf = AsyncProxy.client(CloudFormationApi.class,
          loadBalancingAuthTransform, () -> Topology.lookup(CloudFormation.class));

      final Set<String> allZones = Sets.newHashSet();
      final Set<String> updatedZones = Sets.newHashSet();
      for (final LoadBalancerSubnetView subnet : subnets) {
        allZones.add(subnet.getAvailabilityZone());
        final String stackName = getStackName(loadBalancer, subnet.getAvailabilityZone());
        final Map<String, String> parameters =
            getStackParameters(loadBalancer, loadBalancerAttributes, subnet, listeners);
        try {
          cf.updateStack(updateStackMessage(stackName, parameters));
          updatedZones.add(subnet.getAvailabilityZone());
        } catch (final Exception e) {
          final boolean updateExpired =
              (loadBalancer.getLastUpdateTimestamp().getTime() + UPDATE_RETRY_TIMEOUT)
                  < System.currentTimeMillis();
          final boolean isValidationError =
              AsyncExceptions.isWebServiceErrorCode(e, "ValidationError");
          final String message = AsyncExceptions.asWebServiceErrorMessage(e, e.getMessage());
          if (isValidationError && message.toLowerCase().contains("no updates")) {
            logger.debug("No changes for load balancer on update " + loadBalancerId);
            updatedZones.add(subnet.getAvailabilityZone());
          } else if (isValidationError && message.contains("_IN_PROGRESS") && !updateExpired) {
            logger.info("In progress update for load balancer " + loadBalancerId + " (will retry)");
          } else if (!updateExpired) {
            logger.warn("Error in update for load balancer "
                + loadBalancerId
                + " (will retry): "
                + message);
          } else {
            logger.warn("Update expired for load balancer " + loadBalancerId + ": " + message);
            updatedZones.add(subnet.getAvailabilityZone());
          }
        }
      }
      if (!updatedZones.containsAll(allZones)) continue;

      try {
        loadBalancers.updateByView(loadBalancer, Predicates.alwaysTrue(), balancer -> {
          //noinspection ConstantConditions
          if (loadBalancer.getLastUpdateTimestamp().equals(balancer.getLastUpdateTimestamp())) {
            balancer.setUpdateRequired(false);
          }
          return null;
        });
      } catch (Exception e) {
        if (PersistenceExceptions.isStaleUpdate(e)) {
          logger.debug("Conflict updating load balancer " + loadBalancerId + " (will retry)");
        } else {
          logger.error("Error updating load balancer " + loadBalancerId, e);
        }
      }
    }
  }

  private void loadBalancersLoad() {
    if (loaded.get()) return;

    final ComputeApi ec2;
    final DescribeInstancesResponseType describeInstancesResponse;
    try {
      ec2 = AsyncProxy.client(ComputeApi.class,
          loadBalancingAuthTransform, () -> Topology.lookup(Compute.class));
      describeInstancesResponse = ec2.describeInstances(
          ComputeApi.filter("instance-state-name", "running"),
          ComputeApi.filter("tag-key", "loadbalancer-id"),
          ComputeApi.filter("tag:user-interface-status", "attached")
      );
    } catch (final Exception e) {
      logger.warn("Error describing instances to populate load balancer servos: " + e.getMessage());
      return;
    }

    int servoInstanceCount = 0;
    int notifiedCount = 0;
    for (final ReservationInfoType reservationInfo : describeInstancesResponse.getReservationSet()) {
      for (final RunningInstancesItemType runningInstance : reservationInfo.getInstancesSet()) {
        servoInstanceCount++;

        final String instanceId = runningInstance.getInstanceId();
        try {
          String loadBalancerId = null;
          for (final ResourceTag tag : runningInstance.getTagSet()) {
            if ("loadbalancer-id".equals(tag.getKey())) {
              loadBalancerId = Objects.toString(tag.getValue(), loadBalancerId);
              break;
            }
          }

          String loadBalancerServoIp = null;
          for (final InstanceNetworkInterfaceSetItemType networkInterface : runningInstance.getNetworkInterfaceSet().getItem()) {
            if (1 == networkInterface.getAttachment().getDeviceIndex()) {
              if (networkInterface.getAssociation() == null) {
                loadBalancerServoIp = networkInterface.getPrivateIpAddress();
              } else {
                loadBalancerServoIp = networkInterface.getAssociation().getPublicIp();
              }
              break;
            }
          }

          if (loadBalancerId == null || loadBalancerServoIp == null) {
            continue;
          }

          final LoadBalancerView loadBalancer = lookupLoadBalancerById(loadBalancerId);
          ServoCache.notifyServo(
              runningInstance.getInstanceId(),
              loadBalancer.getNaturalId(),
              loadBalancer.getDisplayName(),
              loadBalancer.getOwnerAccountNumber(),
              runningInstance.getPlacement(),
              loadBalancerServoIp);
          notifiedCount++;
        } catch (final Exception e) {
          logger.warn("Error initializing load balancer servo " + instanceId, e);
        }
      }
    }

    logger.info("Initialized servo cache for " + notifiedCount + "/" + servoInstanceCount + " instances");

    loaded.set(true);
  }

  /**
   * Track servo instances and configure them
   */
  private void loadBalancersTrack() {
    final ComputeApi ec2;
    final DescribeInstancesResponseType describeInstancesResponse;
    try {
      ec2 = AsyncProxy.client(ComputeApi.class,
          loadBalancingAuthTransform, () -> Topology.lookup(Compute.class));
      describeInstancesResponse = ec2.describeInstances(
          ComputeApi.filter("instance-state-name", "running"),
          ComputeApi.filter("tag-key", "loadbalancer-id"),
          ComputeApi.filter("tag:user-interface-status", "pending")
      );
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
          String privateIp = null;
          boolean hasUserNetworkInterface = false;
          for (final InstanceNetworkInterfaceSetItemType networkInterface : runningInstance.getNetworkInterfaceSet().getItem()) {
            if (1 == networkInterface.getAttachment().getDeviceIndex()) {
              hasUserNetworkInterface = true;
              if (!networkInterface.getAttachment().getDeleteOnTermination() ||
                  networkInterface.getAssociation() == null) {
                networkInterfaceId = networkInterface.getNetworkInterfaceId();
                attachmentId = networkInterface.getAttachment().getAttachmentId();
                privateIp = networkInterface.getPrivateIpAddress();
              }
              break;
            }
          }

          LoadBalancerView loadBalancer = null;
          if (!hasUserNetworkInterface) {
            logger.info("Creating user network interface for servo instance: " + instanceId);

            for (final ResourceTag tag : runningInstance.getTagSet()) {
              if ("loadbalancer-id".equals(tag.getKey())) {
                loadBalancerId = Objects.toString(tag.getValue(), loadBalancerId);
              }
            }
            loadBalancer = lookupLoadBalancerById(loadBalancerId);
            final Option<LoadBalancerSubnetView> subnet =
                loadBalancer.findSubnetView(runningInstance.getPlacement());
            final List<String> securityGroupIds = loadBalancer.getSecurityGroupIds();

            if (subnet.isDefined()) {
              networkInterfaceId =
                  ec2.createNetworkInterface(
                      createNetworkInterfaceMessage(subnet.get().getSubnetId(), securityGroupIds))
                      .getNetworkInterface().getNetworkInterfaceId();

              attachmentId =
                  ec2.attachNetworkInterface(instanceId, 1, networkInterfaceId).getAttachmentId();
            } else {
              logger.warn("Subnet not found for servo instance: " + instanceId);
            }
          }

          if (attachmentId != null) {
            if (loadBalancer == null) {
              loadBalancer = lookupLoadBalancerById(loadBalancerId);
            }

            String loadBalancerServoIp;
            if (loadBalancer.getScheme() != LoadBalancer.Scheme.internal) {
              logger.info("Allocating public ip for servo instance: " + instanceId);

              final Map<String,String> tags = Maps.newHashMap();
              tags.put("loadbalancer-id", loadBalancerId);
              final AllocateAddressResponseType allocateResponse = ec2.allocateAddress("vpc", tags);
              final String allocationId = allocateResponse.getAllocationId();
              loadBalancerServoIp = allocateResponse.getPublicIp();

              ec2.associateAddress(assocateAddressMessage(allocationId, networkInterfaceId));
            } else {
              loadBalancerServoIp = privateIp;
            }

            logger.info("Modifying user network interface for servo instance: " + instanceId);

            ec2.modifyNetworkInterfaceAttribute(
                modifyNetworkInterfaceAttributeMessage(networkInterfaceId, attachmentId, true));

            // notify cache
            ServoCache.notifyServo(
                runningInstance.getInstanceId(),
                loadBalancer.getNaturalId(),
                loadBalancer.getDisplayName(),
                loadBalancer.getOwnerAccountNumber(),
                runningInstance.getPlacement(),
                loadBalancerServoIp);
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

  private UpdateStackType updateStackMessage(
      final String name,
      final Map<String,String> parameterMap
  ) {
    final UpdateStackType updateStack = new UpdateStackType();
    updateStack.setStackName(name);
    updateStack.setUsePreviousTemplate(true);
    final Parameters parameters = new Parameters();
    Stream.ofAll(parameterMap.entrySet())
        .map(entry -> new Parameter(entry.getKey(), entry.getValue()))
        .forEach(parameters.getMember()::add);
    updateStack.setParameters(parameters);
    final Capabilities capabilities = new Capabilities();
    capabilities.getMember().add("CAPABILITY_IAM");
    updateStack.setCapabilities(capabilities);
    return updateStack;
  }

  private DeleteStackType deleteStackMessage(final String name) {
    final DeleteStackType deleteStack = new DeleteStackType();
    deleteStack.setStackName(name);
    return deleteStack;
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

  @SuppressWarnings("SameParameterValue")
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

  private AssociateAddressType assocateAddressMessage(
      final String allocationId,
      final String networkInterfaceId
  ) {
    final AssociateAddressType associateAddress = new AssociateAddressType();
    associateAddress.setAllocationId(allocationId);
    associateAddress.setNetworkInterfaceId(networkInterfaceId);
    return associateAddress;
  }

  private CreateTagsType createTagsMessage(final String resourceId, final Map<String,String> tags) {
    final CreateTagsType createTags = new CreateTagsType();
    createTags.getResourcesSet().add(resourceId);
    Stream.ofAll(tags.entrySet())
        .map(entry -> new ResourceTag(entry.getKey(), entry.getValue()))
        .forEach(createTags.getTagSet()::add);
    return createTags;
  }

  private String getStackName(final LoadBalancerView loadBalancer, final String zone) {
    final String userAccount = loadBalancer.getOwnerAccountNumber();
    final String name = loadBalancer.getDisplayName();
    return "loadbalancer-" + loadBalancer.getType().getCode() + "-" + userAccount + "-" + zone + "-" + name;
  }

  private String getTemplate(final LoadBalancerView loadBalancer) {
    try {
      return Resources.toString(
          Resources.getResource(Loadbalancingv2Workflow.class,
              "loadbalancer-" + loadBalancer.getType().getCode() + "-zone.yaml"),
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
        loadBalancers.delete(LoadBalancer.named(
            AccountFullName.getInstance(loadBalancer.getOwnerAccountNumber()),
            loadBalancer.getDisplayName()));
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
        //noinspection ConstantConditions
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
    for (final LoadBalancerSubnetView subnet : loadBalancer.getSubnetViews()) {
      cf.deleteStack(deleteStackMessage(getStackName(loadBalancer, subnet.getAvailabilityZone())));
    }

    return releasedAll;
  }

  private void addressesRelease() {
    try {
      final ComputeApi ec2 = AsyncProxy.client(ComputeApi.class,
          loadBalancingAuthTransform, () -> Topology.lookup(Compute.class));

      final DescribeAddressesResponseType describeAddressesResponse = ec2.describeAddresses(
          ComputeApi.filter("tag-key", "loadbalancer-id")
      );

      for (final AddressInfoType addressInfo : describeAddressesResponse.getAddressesSet()) {
        if (addressInfo.getAssociationId() == null) {
          logger.info("Releasing address: " + addressInfo.getAllocationId() + "/" + addressInfo.getPublicIp());
          ec2.releaseAddressByAllocationId(addressInfo.getAllocationId());
        }
      }
    } catch (final Exception e) {
      logger.warn("Error during address release", e);
    }
  }

  private void loadBalancersWorkflow() {
    try {
      final List<LoadBalancerView> loadBalancers =
          Lists.newArrayList(this.loadBalancers.listByExample(
              LoadBalancer.named(null,null),
              Predicates.alwaysTrue(),
              ImmutableLoadBalancerView::copyOf));

      @SuppressWarnings("unchecked")
      final List<Consumer<LoadBalancerView>> workflowRunners = Lists.newArrayList(
          loadBalancer -> LoadBalancingWorkflows.runUpdateLoadBalancer(
              loadBalancer.getOwnerAccountNumber(),
              loadBalancer.getDisplayName(),
              loadBalancer.getArn()),
          loadBalancer -> LoadBalancingWorkflows.runInstanceStatusPolling(
              loadBalancer.getOwnerAccountNumber(),
              loadBalancer.getDisplayName(),
              loadBalancer.getArn()));

      Collections.shuffle(loadBalancers);
      Collections.shuffle(workflowRunners);

      loadBalancers.forEach(loadBalancer ->
          workflowRunners.forEach(runner ->
              runner.accept(loadBalancer)));
    } catch (final Exception e) {
      logger.warn("Error during continuous workflow start", e);
    }
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
            //noinspection ConstantConditions
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
      //noinspection NonAtomicOperationOnVolatileField
      if (++count % calcFactor() == 0) {
        logger.trace("Running ELBv2 workflow task: " + task);
        doWork();
        logger.trace("Completed ELBv2 workflow task: " + task);
      }
    }

    abstract void doWork() throws Exception;
  }

  public static class Loadbalancingv2WorkflowEventListener implements EventListener<ClockTick> {

    static {
      LoadBalancerHelper.setServoMetadataSource(new SwitchedServoMetadataSource());
    }

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
      }  else {
        loadbalancingv2Workflow.halted();
      }
    }
  }
}
