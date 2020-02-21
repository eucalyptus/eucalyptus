/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSRoute53RecordSetGroupResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSRoute53RecordSetGroupProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.Route53RecordSet;
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
import com.google.common.collect.Lists;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 *
 */
public class AWSRoute53RecordSetGroupResourceAction extends StepBasedResourceAction {

  private AWSRoute53RecordSetGroupProperties properties = new AWSRoute53RecordSetGroupProperties();
  private AWSRoute53RecordSetGroupResourceInfo info = new AWSRoute53RecordSetGroupResourceInfo();

  public AWSRoute53RecordSetGroupResourceAction() {
    super(fromEnum(CreateSteps.class),
        fromEnum(DeleteSteps.class),
        fromUpdateEnum(UpdateNoInterruptionSteps.class),
        null );
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    final AWSRoute53RecordSetGroupResourceAction otherAction = (AWSRoute53RecordSetGroupResourceAction) resourceAction;
    UpdateType updateType = info.supportsTags() && stackTagsChanged ? UpdateType.NO_INTERRUPTION : UpdateType.NONE;
    if (!Objects.equals(properties.getComment(), otherAction.properties.getComment())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    if (!Objects.equals(properties.getHostedZoneId(), otherAction.properties.getHostedZoneId())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getHostedZoneName(), otherAction.properties.getHostedZoneName())) {
      updateType = UpdateType.max(updateType, UpdateType.NEEDS_REPLACEMENT);
    }
    if (!Objects.equals(properties.getRecordSets(), otherAction.properties.getRecordSets())) {
      updateType = UpdateType.max(updateType, UpdateType.NO_INTERRUPTION);
    }
    return updateType;
  }

  private static ChangeBatch toChangeBatch(final String action, final AWSRoute53RecordSetGroupProperties properties) {
    final ChangeBatch changeBatch = new ChangeBatch();
    final Changes changes = toChanges(action, properties);
    changeBatch.setChanges(changes);
    changeBatch.setComment(properties.getComment());
    return changeBatch;
  }

  private static Changes toChanges(final String action, final AWSRoute53RecordSetGroupProperties properties) {
    final Changes changes = new Changes();
    if (properties.getRecordSets() != null) {
      for (final Route53RecordSet recordSet : properties.getRecordSets()) {
        changes.getMember().add(toChange(action, recordSet));
      }
    }
    return changes;
  }

  private static void populateDeletes(final Changes changes, final AWSRoute53RecordSetGroupProperties properties ) {
    final Set<Tuple2<String,String>> changeNameTypes = changes.getMember().stream().map( change -> Tuple.of(
        change.getResourceRecordSet().getName(),
        change.getResourceRecordSet().getType())).collect(Collectors.toSet());

    final ArrayList<Change> deletes = Lists.newArrayList();
    if (properties.getRecordSets() != null) {
      for (final Route53RecordSet recordSet : properties.getRecordSets()) {
        if (!changeNameTypes.contains(Tuple.of(recordSet.getName(),recordSet.getType()))) {
          deletes.add(toChange("DELETE", recordSet));
        }
      }
    }

    if (!deletes.isEmpty()) {
      deletes.addAll(changes.getMember());
      changes.setMember(deletes);
    }
  }

  private static Change toChange(final String action, final Route53RecordSet recordSet) {
    final Change change = new Change();
    change.setAction(action);
    final ResourceRecordSet resourceRecordSet = new ResourceRecordSet();
    resourceRecordSet.setName(recordSet.getName());
    resourceRecordSet.setType(recordSet.getType());
    if ( recordSet.getAliasTarget() != null ) {
      final AliasTarget aliasTarget = new AliasTarget();
      aliasTarget.setDNSName( recordSet.getAliasTarget().getDnsName() );
      aliasTarget.setEvaluateTargetHealth( MoreObjects.firstNonNull(
          recordSet.getAliasTarget().getEvaluateTargetHealth(), Boolean.FALSE) );
      aliasTarget.setHostedZoneId( recordSet.getAliasTarget().getHostedZoneId() );
      resourceRecordSet.setAliasTarget( aliasTarget );
    }
    if ( recordSet.getResourceRecords()!=null && !recordSet.getResourceRecords().isEmpty() ) {
      final ResourceRecords resourceRecords = new ResourceRecords();
      for (final String value : recordSet.getResourceRecords() ) {
        final ResourceRecord resourceRecord = new ResourceRecord();
        resourceRecord.setValue(value);
        resourceRecords.getMember().add(resourceRecord);
      }
      resourceRecordSet.setResourceRecords(resourceRecords);

    }
    if (recordSet.getTtl() !=null) {
      resourceRecordSet.setTTL(Long.valueOf(recordSet.getTtl()));
    }
    change.setResourceRecordSet(resourceRecordSet);
    return change;
  }

  private enum CreateSteps implements Step {
    RESOLVE_HOSTEDZONE {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53RecordSetGroupResourceAction action = (AWSRoute53RecordSetGroupResourceAction) resourceAction;
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
          if (zoneListing.getHostedZones()!=null && zoneListing.getHostedZones().getMember().size()==1) {
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
    CREATE_RECORDSETGROUP {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSRoute53RecordSetGroupResourceAction action = (AWSRoute53RecordSetGroupResourceAction) resourceAction;
        final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
        final ChangeResourceRecordSetsType changeRrSets =
            MessageHelper.createMessage(ChangeResourceRecordSetsType.class, action.info.getEffectiveUserId());
        final String hostedZoneId = action.info.getHostedZoneId();
        changeRrSets.setHostedZoneId(hostedZoneId);
        changeRrSets.setChangeBatch(toChangeBatch("UPSERT", action.properties));
        route53.changeResourceRecordSets(changeRrSets);
        action.info.setPhysicalResourceId(action.info.getLogicalResourceId());
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
        final AWSRoute53RecordSetGroupResourceAction action = (AWSRoute53RecordSetGroupResourceAction) resourceAction;
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
        final AWSRoute53RecordSetGroupResourceAction oldAction = (AWSRoute53RecordSetGroupResourceAction) oldResourceAction;
        final AWSRoute53RecordSetGroupResourceAction newAction = (AWSRoute53RecordSetGroupResourceAction) newResourceAction;
        final Route53Api route53 = AsyncProxy.client( Route53Api.class, Function.identity( ) );
        final ChangeResourceRecordSetsType changeRrSets =
            MessageHelper.createMessage(ChangeResourceRecordSetsType.class, oldAction.info.getEffectiveUserId());
        final String hostedZoneId = oldAction.info.getHostedZoneId();
        changeRrSets.setHostedZoneId(hostedZoneId);
        final ChangeBatch changeBatch = new ChangeBatch();
        final Changes changes = toChanges("UPSERT", newAction.properties);
        populateDeletes(changes, oldAction.properties);
        changeBatch.setChanges(changes);
        changeBatch.setComment(newAction.properties.getComment());
        changeRrSets.setChangeBatch(changeBatch);
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
    properties = (AWSRoute53RecordSetGroupProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSRoute53RecordSetGroupResourceInfo) resourceInfo;
  }
}
