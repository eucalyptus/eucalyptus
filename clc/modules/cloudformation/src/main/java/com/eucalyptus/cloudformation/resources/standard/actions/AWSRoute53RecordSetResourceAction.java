/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import java.util.Objects;
import java.util.function.Function;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSRoute53RecordSetResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSRoute53RecordSetProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.ResourceFailureException;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.steps.UpdateStep;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.route53.common.Route53Api;
import com.eucalyptus.route53.common.msgs.AliasTarget;
import com.eucalyptus.route53.common.msgs.Change;
import com.eucalyptus.route53.common.msgs.ChangeBatch;
import com.eucalyptus.route53.common.msgs.ChangeResourceRecordSetsType;
import com.eucalyptus.route53.common.msgs.Changes;
import com.eucalyptus.route53.common.msgs.ListHostedZonesByNameResponseType;
import com.eucalyptus.route53.common.msgs.ListHostedZonesByNameType;
import com.eucalyptus.route53.common.msgs.ResourceRecord;
import com.eucalyptus.route53.common.msgs.ResourceRecordSet;
import com.eucalyptus.route53.common.msgs.ResourceRecords;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;

/**
 *
 */
public class AWSRoute53RecordSetResourceAction extends StepBasedResourceAction {

  private AWSRoute53RecordSetProperties properties = new AWSRoute53RecordSetProperties();
  private AWSRoute53RecordSetResourceInfo info = new AWSRoute53RecordSetResourceInfo();

  public AWSRoute53RecordSetResourceAction() {
    super(fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        fromUpdateEnum(UpdateNoInterruptionSteps.class),
        null );
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    final AWSRoute53RecordSetResourceAction otherAction = (AWSRoute53RecordSetResourceAction) resourceAction;
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    if (!Objects.equals(properties.getAliasTarget(), otherAction.properties.getAliasTarget())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getComment(), otherAction.properties.getComment())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getHostedZoneId(), otherAction.properties.getHostedZoneId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getHostedZoneName(), otherAction.properties.getHostedZoneName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getName(), otherAction.properties.getName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getResourceRecords(), otherAction.properties.getResourceRecords())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getTtl(), otherAction.properties.getTtl())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getType(), otherAction.properties.getType())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private static ChangeBatch toChangeBatch(final String action, final AWSRoute53RecordSetProperties properties) {
    final ChangeBatch changeBatch = new ChangeBatch();
    final Changes changes = new Changes();
    final Change change = toChange(action, properties);
    changes.getMember().add(change);
    changeBatch.setChanges(changes);
    changeBatch.setComment(properties.getComment());
    return changeBatch;
  }

  private static Change toChange(final String action, final AWSRoute53RecordSetProperties properties) {
    final Change change = new Change();
    change.setAction(action);
    final ResourceRecordSet resourceRecordSet = new ResourceRecordSet();
    resourceRecordSet.setName(properties.getName());
    resourceRecordSet.setType(properties.getType());
    if ( properties.getAliasTarget() != null ) {
      final AliasTarget aliasTarget = new AliasTarget();
      aliasTarget.setDNSName( properties.getAliasTarget().getDnsName() );
      aliasTarget.setEvaluateTargetHealth( MoreObjects.firstNonNull(
          properties.getAliasTarget().getEvaluateTargetHealth(), Boolean.FALSE) );
      aliasTarget.setHostedZoneId( properties.getAliasTarget().getHostedZoneId() );
      resourceRecordSet.setAliasTarget( aliasTarget );
    }
    if ( properties.getResourceRecords()!=null && !properties.getResourceRecords().isEmpty() ) {
      final ResourceRecords resourceRecords = new ResourceRecords();
      for (final String value : properties.getResourceRecords() ) {
        final ResourceRecord resourceRecord = new ResourceRecord();
        resourceRecord.setValue(value);
        resourceRecords.getMember().add(resourceRecord);
      }
      resourceRecordSet.setResourceRecords(resourceRecords);

    }
    if (properties.getTtl() !=null) {
      resourceRecordSet.setTTL(Long.valueOf(properties.getTtl()));
    }
    change.setResourceRecordSet(resourceRecordSet);
    return change;
  }

  private enum CreateSteps implements Step {
    RESOLVE_HOSTEDZONE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53RecordSetResourceAction action = (AWSRoute53RecordSetResourceAction) resourceAction;
        final String hostedZoneName = action.properties.getHostedZoneName();
        final String hostedZoneId;
        if (action.properties.getHostedZoneId() != null) {
          hostedZoneId = action.properties.getHostedZoneId();
        } else if (hostedZoneName != null) {
          final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
          final ListHostedZonesByNameType listHostedZonesByName =
              MessageHelper.createMessage(ListHostedZonesByNameType.class, action.info.getEffectiveUserId());
          listHostedZonesByName.setDNSName(hostedZoneName);
          final ListHostedZonesByNameResponseType zoneListing =
              route53.listHostedZonesByName(listHostedZonesByName);
          if (zoneListing.getHostedZones()!=null && !zoneListing.getHostedZones().getMember().isEmpty()) {
            hostedZoneId = Strings.substringAfter("/hostedzone/",
                zoneListing.getHostedZones().getMember().get(0).getId());
          } else {
            throw new ResourceFailureException("Unique hosted zone not found for " + hostedZoneName);
          }
        } else {
          throw new ResourceFailureException("Hosted zone id or name required");
        }
        action.info.setHostedZoneId(hostedZoneId);
        return action;
      }
    },
    CREATE_RECORDSET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53RecordSetResourceAction action = (AWSRoute53RecordSetResourceAction) resourceAction;
        final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
        final ChangeResourceRecordSetsType changeRrSets =
            MessageHelper.createMessage(ChangeResourceRecordSetsType.class, action.info.getEffectiveUserId());
        final String hostedZoneId = action.info.getHostedZoneId();
        changeRrSets.setHostedZoneId(hostedZoneId);
        changeRrSets.setChangeBatch(toChangeBatch("UPSERT", action.properties));
        route53.changeResourceRecordSets(changeRrSets);
        action.info.setPhysicalResourceId(action.properties.getName());
        action.info.setHostedZoneId(hostedZoneId);
        action.info.setCreatedEnoughToDelete(true);
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_RECORDSET {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53RecordSetResourceAction action = (AWSRoute53RecordSetResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
        final ChangeResourceRecordSetsType changeRrSets =
            MessageHelper.createMessage(ChangeResourceRecordSetsType.class, action.info.getEffectiveUserId());
        final String hostedZoneId = action.info.getHostedZoneId();
        changeRrSets.setHostedZoneId(hostedZoneId);
        changeRrSets.setChangeBatch(toChangeBatch("DELETE", action.properties));
        route53.changeResourceRecordSets(changeRrSets);
        return action;
      }
    }
  }

  private enum UpdateNoInterruptionSteps implements UpdateStep {
    UPDATE_RECORDSET {
      @Override
      public ResourceAction perform(ResourceAction oldResourceAction, ResourceAction newResourceAction) throws Exception {
        final AWSRoute53RecordSetResourceAction oldAction = (AWSRoute53RecordSetResourceAction) oldResourceAction;
        final AWSRoute53RecordSetResourceAction newAction = (AWSRoute53RecordSetResourceAction) newResourceAction;
        final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
        final ChangeResourceRecordSetsType changeRrSets =
            MessageHelper.createMessage(ChangeResourceRecordSetsType.class, oldAction.info.getEffectiveUserId());
        final String hostedZoneId = oldAction.info.getHostedZoneId();
        changeRrSets.setHostedZoneId(hostedZoneId);
        if (!Objects.equals(oldAction.properties.getType(), newAction.properties.getType())) {
          // if type changes then the identity is altered so delete/upsert is required
          final ChangeBatch changeBatch = new ChangeBatch();
          final Changes changes = new Changes();
          changes.getMember().add(toChange("DELETE", oldAction.properties));
          changes.getMember().add(toChange("UPSERT", newAction.properties));
          changeBatch.setChanges(changes);
          changeBatch.setComment(newAction.properties.getComment());
          changeRrSets.setChangeBatch(changeBatch);
        } else {
          changeRrSets.setChangeBatch(toChangeBatch("UPSERT", newAction.properties));
        }
        route53.changeResourceRecordSets(changeRrSets);
        return newAction;
      }
    }
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSRoute53RecordSetProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSRoute53RecordSetResourceInfo) resourceInfo;
  }
}
