/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.TagHelper;
import com.eucalyptus.cloudformation.resources.standard.info.AWSRoute53HostedZoneResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSRoute53HostedZoneProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.CloudFormationResourceTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.Route53HostedZoneTag;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.Route53Vpc;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.route53.common.Route53Api;
import com.eucalyptus.route53.common.msgs.AssociateVPCWithHostedZoneType;
import com.eucalyptus.route53.common.msgs.ChangeTagsForResourceType;
import com.eucalyptus.route53.common.msgs.CreateHostedZoneResponseType;
import com.eucalyptus.route53.common.msgs.CreateHostedZoneType;
import com.eucalyptus.route53.common.msgs.DeleteHostedZoneType;
import com.eucalyptus.route53.common.msgs.DisassociateVPCFromHostedZoneType;
import com.eucalyptus.route53.common.msgs.HostedZoneConfig;
import com.eucalyptus.route53.common.msgs.ListResourceRecordSetsResponseType;
import com.eucalyptus.route53.common.msgs.ListResourceRecordSetsType;
import com.eucalyptus.route53.common.msgs.ResourceRecord;
import com.eucalyptus.route53.common.msgs.ResourceRecordSet;
import com.eucalyptus.route53.common.msgs.Tag;
import com.eucalyptus.route53.common.msgs.TagKeyList;
import com.eucalyptus.route53.common.msgs.TagList;
import com.eucalyptus.route53.common.msgs.UpdateHostedZoneCommentType;
import com.eucalyptus.route53.common.msgs.VPC;
import com.eucalyptus.util.NonNullFunction;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 */
public class AWSRoute53HostedZoneResourceAction extends StepBasedResourceAction {

  private AWSRoute53HostedZoneProperties properties = new AWSRoute53HostedZoneProperties();
  private AWSRoute53HostedZoneResourceInfo info = new AWSRoute53HostedZoneResourceInfo();

  public AWSRoute53HostedZoneResourceAction() {
    super(fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        fromUpdateEnum(UpdateNoInterruptionSteps.class),
        null);
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    final AWSRoute53HostedZoneResourceAction otherAction = (AWSRoute53HostedZoneResourceAction) resourceAction;
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    if (!Objects.equals(properties.getHostedZoneConfig(), otherAction.properties.getHostedZoneConfig())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getHostedZoneTags(), otherAction.properties.getHostedZoneTags())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getName(), otherAction.properties.getName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getVpcs(), otherAction.properties.getVpcs())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private static void populateChangeTags(
      final ChangeTagsForResourceType changeTags,
      final String hostedZoneId,
      final List<Tag> addTags) {
    changeTags.setResourceId(hostedZoneId);
    changeTags.setResourceType("hostedzone");
    if (!addTags.isEmpty()) {
      final TagList tagList = new TagList();
      tagList.getMember().addAll(addTags);
      changeTags.setAddTags(tagList);
    }
  }

  private static Set<String> hostedZoneTagKeys(@Nullable final List<Route53HostedZoneTag> hostedZoneTags) {
    return hostedZoneTags == null || hostedZoneTags.isEmpty() ?
        Collections.emptySet() :
        hostedZoneTags.stream().map(Route53HostedZoneTag::getKey).collect(Collectors.toSet());
  }

  private static Collection<Tag> hostedZoneTags(@Nullable final List<Route53HostedZoneTag> hostedZoneTags) {
    return hostedZoneTags == null || hostedZoneTags.isEmpty() ?
        Collections.emptySet() :
        hostedZoneTags.stream().map(PropertiesTagTransform.INSTANCE).collect(Collectors.toSet());
  }

  private enum CreateSteps implements Step {
    CREATE_HOSTEDZONE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction action = (AWSRoute53HostedZoneResourceAction) resourceAction;
        final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
        boolean admin;
        try {
          admin = Accounts.lookupPrincipalByAccountNumber(action.info.getAccountId( )).isSystemUser();
        } catch (Exception e) {
          admin = false;
        }
        final CreateHostedZoneType createHostedZone = admin ?
            MessageHelper.createPrivilegedMessage(CreateHostedZoneType.class, action.info.getAccountId( )) :
            MessageHelper.createMessage(CreateHostedZoneType.class, action.info.getEffectiveUserId());
        createHostedZone.setCallerReference(UUID.randomUUID().toString()); //TODO wwawsd
        createHostedZone.setName(action.properties.getName());
        if (action.properties.getHostedZoneConfig() != null &&
            action.properties.getHostedZoneConfig().getComment() != null) {
          final HostedZoneConfig hostedZoneConfig = new HostedZoneConfig();
          hostedZoneConfig.setComment(action.properties.getHostedZoneConfig().getComment());
          createHostedZone.setHostedZoneConfig(hostedZoneConfig);
        }
        if (action.properties.getVpcs() != null &&
            !action.properties.getVpcs().isEmpty()) {
          final VPC vpc = new VPC();
          vpc.setVPCId(action.properties.getVpcs().get(0).getVpcId());
          vpc.setVPCRegion(action.properties.getVpcs().get(0).getVpcRegion());
          createHostedZone.setVPC(vpc);
        }
        final CreateHostedZoneResponseType response = route53.createHostedZone(createHostedZone);
        final ArrayNode nameServers = JsonHelper.createArrayNode();
        action.info.setPhysicalResourceId(Strings.trimPrefix("/hostedzone/", response.getHostedZone().getId()));
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        action.info.setNameServers(JsonHelper.getStringFromJsonNode(nameServers));
        return action;
      }
    },
    CREATE_VPCS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction action = (AWSRoute53HostedZoneResourceAction) resourceAction;
        if (action.properties.getVpcs() != null &&
            action.properties.getVpcs().size() > 1) {
          final Route53Api route53 = AsyncProxy.client(Route53Api.class, Function.identity());
          for (final Route53Vpc propertiesVpc : action.properties.getVpcs().subList(1,action.properties.getVpcs().size())) {
            final AssociateVPCWithHostedZoneType associateVpc =
                MessageHelper.createMessage(AssociateVPCWithHostedZoneType.class, action.info.getEffectiveUserId());
            associateVpc.setHostedZoneId(action.info.getPhysicalResourceId());
            associateVpc.setVPC(PropertiesVpcTransform.INSTANCE.apply(propertiesVpc));
            route53.associateVPCWithHostedZone(associateVpc);
          }
        }
        return action;
      }
    },
    CREATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction action = (AWSRoute53HostedZoneResourceAction) resourceAction;
        final Route53Api route53 = AsyncProxy.client(Route53Api.class, Function.identity());

        // Create 'system' tags as admin user
        {
          final String effectiveAdminUserId = action.info.getAccountId();
          final ChangeTagsForResourceType changeTagsSystem =
              MessageHelper.createPrivilegedMessage(ChangeTagsForResourceType.class, effectiveAdminUserId);
          populateChangeTags(
              changeTagsSystem,
              action.info.getPhysicalResourceId(),
              TagHelper.getCloudFormationResourceSystemTags(
                  action.info, action.getStackEntity(), TagTransform.INSTANCE));
          route53.changeTagsForResource(changeTagsSystem);
        }
        final List<Tag> tags = TagHelper.getCloudFormationResourceStackTags(
            action.getStackEntity(), TagTransform.INSTANCE);
        if (action.properties.getHostedZoneTags() != null) {
          TagHelper.checkReservedTemplateTags(hostedZoneTagKeys(action.properties.getHostedZoneTags()));
          tags.addAll(hostedZoneTags(action.properties.getHostedZoneTags()));
        }
        if (!tags.isEmpty()) {
          final ChangeTagsForResourceType changeTags =
              MessageHelper.createMessage(ChangeTagsForResourceType.class, action.info.getEffectiveUserId());
          populateChangeTags(changeTags, action.info.getPhysicalResourceId(), tags);
          route53.changeTagsForResource(changeTags);
        }

        return action;
      }
    },
    GET_ATTRIBUTES {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction action = (AWSRoute53HostedZoneResourceAction) resourceAction;
        final ArrayNode nameServersArrayNode = JsonHelper.createArrayNode();
        final boolean privateZone = action.properties.getVpcs() != null && !action.properties.getVpcs().isEmpty();
        if (!privateZone) {
          final Route53Api route53 = AsyncProxy.client(Route53Api.class, Function.identity());
          final ListResourceRecordSetsType listRRSets =
              MessageHelper.createMessage(ListResourceRecordSetsType.class, action.info.getEffectiveUserId());
          listRRSets.setHostedZoneId(action.info.getPhysicalResourceId());
          final ListResourceRecordSetsResponseType rrSetListing = route53.listResourceRecordSets(listRRSets);
          if (rrSetListing.getResourceRecordSets()!=null) {
            for (final ResourceRecordSet rrSet : rrSetListing.getResourceRecordSets().getMember()) {
              if ("NS".equals(rrSet.getType()) && rrSet.getResourceRecords() != null) {
                for (final ResourceRecord rr : rrSet.getResourceRecords().getMember()) {
                  if (rr.getValue()!=null) {
                    nameServersArrayNode.add(Strings.trimSuffix(".", rr.getValue()));
                  }
                }
              }
            }
          }
        }
        action.info.setNameServers(JsonHelper.getStringFromJsonNode(nameServersArrayNode));
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_HOSTEDZONE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction action = (AWSRoute53HostedZoneResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
        final DeleteHostedZoneType deleteHostedZone =
            MessageHelper.createMessage(DeleteHostedZoneType.class, action.info.getEffectiveUserId());
        deleteHostedZone.setId(action.info.getPhysicalResourceId());
        try {
          route53.deleteHostedZone(deleteHostedZone);
        } catch ( final RuntimeException ex ) {
          if (!AsyncExceptions.isWebServiceErrorCode(ex, "NoSuchHostedZone")) {
            throw ex;
          }
        }
        return action;
      }
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_HOSTEDZONE {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction oldAction = (AWSRoute53HostedZoneResourceAction) oldResourceAction;
        final AWSRoute53HostedZoneResourceAction newAction = (AWSRoute53HostedZoneResourceAction) newResourceAction;
        if (!Objects.equals(oldAction.properties.getHostedZoneConfig(), newAction.properties.getHostedZoneConfig())) {
          final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
          final UpdateHostedZoneCommentType updateHostedZoneComment =
              MessageHelper.createMessage(UpdateHostedZoneCommentType.class, newAction.info.getEffectiveUserId());
          updateHostedZoneComment.setId(newAction.info.getPhysicalResourceId());
          if (newAction.properties.getHostedZoneConfig() != null &&
              newAction.properties.getHostedZoneConfig().getComment() != null) {
            updateHostedZoneComment.setComment(newAction.properties.getHostedZoneConfig().getComment());
          }
          route53.updateHostedZoneComment(updateHostedZoneComment);
        }
        return newAction;
      }
    },
    UPDATE_VPCS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction oldAction = (AWSRoute53HostedZoneResourceAction) oldResourceAction;
        final AWSRoute53HostedZoneResourceAction newAction = (AWSRoute53HostedZoneResourceAction) newResourceAction;
        if (!Objects.equals(oldAction.properties.getVpcs(), newAction.properties.getVpcs())) {
          final List<Route53Vpc> oldVpcs = MoreObjects.firstNonNull(oldAction.properties.getVpcs(), Collections.emptyList());
          final List<Route53Vpc> newVpcs = MoreObjects.firstNonNull(newAction.properties.getVpcs(), Collections.emptyList());

          final Map<String, Route53Vpc> toRemove = Maps.newHashMap(oldVpcs.stream().collect(Collectors.toMap(Route53Vpc::getVpcId, Function.identity())));
          final Map<String, Route53Vpc> toAdd = Maps.newHashMap(newVpcs.stream().collect(Collectors.toMap(Route53Vpc::getVpcId, Function.identity())));
          final Set<String> unchanged = Sets.newHashSet();
          unchanged.addAll(toRemove.keySet());
          unchanged.removeAll(toAdd.keySet());
          toRemove.keySet().removeAll(unchanged);
          toAdd.keySet().removeAll(unchanged);

          final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
          while (!toRemove.isEmpty() || !toAdd.isEmpty()) {
            if (!toRemove.isEmpty()) {
              final String removeId = toRemove.keySet().iterator().next();
              try {
                final DisassociateVPCFromHostedZoneType disassociateVpc =
                    MessageHelper.createMessage(DisassociateVPCFromHostedZoneType.class, newAction.info.getEffectiveUserId());
                disassociateVpc.setHostedZoneId(newAction.info.getPhysicalResourceId());
                disassociateVpc.setVPC(PropertiesVpcTransform.INSTANCE.apply(toRemove.get(removeId)));
                route53.disassociateVPCFromHostedZone(disassociateVpc);
                toRemove.remove(removeId);
              } catch ( final RuntimeException ex ) {
                if (AsyncExceptions.isWebServiceErrorCode(ex, "VPCAssociationNotFound")) {
                  toRemove.remove(removeId);
                } else if (!AsyncExceptions.isWebServiceErrorCode(ex, "LastVPCAssociation")) {
                  throw ex;
                }
              }
            }
            if (!toAdd.isEmpty()) {
              final String addId = toAdd.keySet().iterator().next();
              final AssociateVPCWithHostedZoneType associateVpc =
                  MessageHelper.createMessage(AssociateVPCWithHostedZoneType.class, newAction.info.getEffectiveUserId());
              associateVpc.setHostedZoneId(newAction.info.getPhysicalResourceId());
              associateVpc.setVPC(PropertiesVpcTransform.INSTANCE.apply(toAdd.get(addId)));
              route53.associateVPCWithHostedZone(associateVpc);
              toAdd.remove(addId);
            }
          }
        }
        return newAction;
      }
    },
    UPDATE_TAGS {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSRoute53HostedZoneResourceAction oldAction = (AWSRoute53HostedZoneResourceAction) oldResourceAction;
        final AWSRoute53HostedZoneResourceAction newAction = (AWSRoute53HostedZoneResourceAction) newResourceAction;

        if (!Objects.equals(oldAction.properties.getHostedZoneTags(), newAction.properties.getHostedZoneTags())) {
          final Set<String> oldKeys = hostedZoneTagKeys(oldAction.properties.getHostedZoneTags());
          final Set<String> newKeys = hostedZoneTagKeys(newAction.properties.getHostedZoneTags());
          TagHelper.checkReservedTemplateTags(newKeys);
          final List<Tag> tags = Lists.newArrayList();
          tags.addAll(hostedZoneTags(newAction.properties.getHostedZoneTags()));
          final Set<String> removedKeys = Sets.newHashSet();
          removedKeys.addAll(oldKeys);
          removedKeys.removeAll(newKeys);

          if (!removedKeys.isEmpty() || !tags.isEmpty()) {
            final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
            final ChangeTagsForResourceType changeTags =
                MessageHelper.createMessage(ChangeTagsForResourceType.class, newAction.info.getEffectiveUserId());
            populateChangeTags(changeTags, newAction.info.getPhysicalResourceId(), tags);
            if (!removedKeys.isEmpty()) {
              final TagKeyList removeTags = new TagKeyList();
              removeTags.getMember().addAll(removedKeys);
              changeTags.setRemoveTagKeys(removeTags);
            }
            route53.changeTagsForResource(changeTags);
          }
        }

        return newAction;
      }
    },
  }

  private enum TagTransform implements NonNullFunction<CloudFormationResourceTag, Tag> {
    INSTANCE;

    @Nonnull
    @Override
    public Tag apply(final CloudFormationResourceTag resourceTag) {
      final Tag tag = new Tag();
      tag.setKey(resourceTag.getKey());
      tag.setValue(resourceTag.getValue());
      return tag;    }
  }

  private enum PropertiesTagTransform implements NonNullFunction<Route53HostedZoneTag, Tag> {
    INSTANCE;

    @Nonnull
    @Override
    public Tag apply(final Route53HostedZoneTag zoneTag) {
      final Tag tag = new Tag();
      tag.setKey(zoneTag.getKey());
      tag.setValue(zoneTag.getValue());
      return tag;
    }
  }

  private enum PropertiesVpcTransform implements NonNullFunction<Route53Vpc, VPC> {
    INSTANCE;

    @Nonnull
    @Override
    public VPC apply(final Route53Vpc route53Vpc) {
      final VPC vpc = new VPC();
      vpc.setVPCId(route53Vpc.getVpcId());
      vpc.setVPCRegion(route53Vpc.getVpcRegion());
      return vpc;
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSRoute53HostedZoneProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSRoute53HostedZoneResourceInfo) resourceInfo;
  }
}
