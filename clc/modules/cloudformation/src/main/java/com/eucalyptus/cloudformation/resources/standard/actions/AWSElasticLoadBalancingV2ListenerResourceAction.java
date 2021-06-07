/*
 * Copyright 2021 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.cloudformation.resources.standard.actions;

import com.eucalyptus.cloudformation.ValidationErrorException;
import com.eucalyptus.cloudformation.resources.ResourceAction;
import com.eucalyptus.cloudformation.resources.ResourceInfo;
import com.eucalyptus.cloudformation.resources.ResourceProperties;
import com.eucalyptus.cloudformation.resources.standard.info.AWSElasticLoadBalancingV2ListenerResourceInfo;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.AWSElasticLoadBalancingV2ListenerProperties;
import com.eucalyptus.cloudformation.resources.standard.propertytypes.ElasticLoadBalancingV2CertificateProperties;
import com.eucalyptus.cloudformation.template.JsonHelper;
import com.eucalyptus.cloudformation.util.MessageHelper;
import com.eucalyptus.cloudformation.workflow.steps.Step;
import com.eucalyptus.cloudformation.workflow.steps.StepBasedResourceAction;
import com.eucalyptus.cloudformation.workflow.updateinfo.UpdateType;
import com.eucalyptus.loadbalancingv2.common.Loadbalancingv2Api;
import com.eucalyptus.loadbalancingv2.common.msgs.Action;
import com.eucalyptus.loadbalancingv2.common.msgs.Actions;
import com.eucalyptus.loadbalancingv2.common.msgs.Certificate;
import com.eucalyptus.loadbalancingv2.common.msgs.CertificateList;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateListenerResponseType;
import com.eucalyptus.loadbalancingv2.common.msgs.CreateListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.DeleteListenerType;
import com.eucalyptus.loadbalancingv2.common.msgs.Listener;
import com.eucalyptus.util.async.AsyncExceptions;
import com.eucalyptus.util.async.AsyncProxy;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableSet;
import io.vavr.collection.Stream;
import java.util.Set;
import java.util.function.Function;
import org.apache.log4j.Logger;

public class AWSElasticLoadBalancingV2ListenerResourceAction extends StepBasedResourceAction {

  public static final Logger LOG = Logger.getLogger(AWSElasticLoadBalancingV2ListenerResourceAction.class);

  private static final Set<Function<AWSElasticLoadBalancingV2ListenerProperties,Object>>
      PROP_REPLACE =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2ListenerProperties,Object>>builder()
          .add(AWSElasticLoadBalancingV2ListenerProperties::getAlpnPolicy)
          .add(AWSElasticLoadBalancingV2ListenerProperties::getDefaultActions)
          .add(AWSElasticLoadBalancingV2ListenerProperties::getLoadBalancerArn)
          .add(AWSElasticLoadBalancingV2ListenerProperties::getPort)
          .add(AWSElasticLoadBalancingV2ListenerProperties::getProtocol)
          .add(AWSElasticLoadBalancingV2ListenerProperties::getSslPolicy)
          .build();

  private static final Set<Function<AWSElasticLoadBalancingV2ListenerProperties,Object>> PROP_SOMEINTER =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2ListenerProperties,Object>>builder()
          .build();

  private static final Set<Function<AWSElasticLoadBalancingV2ListenerProperties,Object>> PROP_NOINTER =
      ImmutableSet.<Function<AWSElasticLoadBalancingV2ListenerProperties,Object>>builder()
          .build();

  private AWSElasticLoadBalancingV2ListenerProperties properties = new AWSElasticLoadBalancingV2ListenerProperties();
  private AWSElasticLoadBalancingV2ListenerResourceInfo info = new AWSElasticLoadBalancingV2ListenerResourceInfo();

  public AWSElasticLoadBalancingV2ListenerResourceAction() {
    super(fromEnum(CreateSteps.class), fromEnum(DeleteSteps.class), null, null );
  }

  @Override
  public ResourceProperties getResourceProperties() {
    return properties;
  }

  @Override
  public void setResourceProperties(ResourceProperties resourceProperties) {
    properties = (AWSElasticLoadBalancingV2ListenerProperties) resourceProperties;
  }

  @Override
  public ResourceInfo getResourceInfo() {
    return info;
  }

  @Override
  public void setResourceInfo(ResourceInfo resourceInfo) {
    info = (AWSElasticLoadBalancingV2ListenerResourceInfo) resourceInfo;
  }

  @Override
  public UpdateType getUpdateType(ResourceAction resourceAction, boolean stackTagsChanged) {
    final AWSElasticLoadBalancingV2ListenerResourceAction otherAction =
        (AWSElasticLoadBalancingV2ListenerResourceAction) resourceAction;
    return defaultUpdateType(
        stackTagsChanged,
        properties,
        otherAction.properties,
        PROP_REPLACE,
        PROP_SOMEINTER,
        PROP_NOINTER);
  }

  private enum CreateSteps implements Step {
    CREATE_LISTENER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingV2ListenerResourceAction action =
            (AWSElasticLoadBalancingV2ListenerResourceAction) resourceAction;
        final Loadbalancingv2Api elbv2 = AsyncProxy.client(
            Loadbalancingv2Api.class, MessageHelper.userIdentity(action.info.getEffectiveUserId()));
        final CreateListenerType createListener = new CreateListenerType();
        createListener.setLoadBalancerArn(action.properties.getLoadBalancerArn());
        createListener.setPort(action.properties.getPort());
        createListener.setProtocol(action.properties.getProtocol());
        final Actions actions = new Actions();
        action.properties.getDefaultActions().forEach( elbAction -> {
          //TODO:STEVE: full action suppport
          final Action listenerAction = new Action();
          listenerAction.setTargetGroupArn(elbAction.getTargetGroupArn());
          listenerAction.setType(elbAction.getType());
          actions.getMember().add(listenerAction);
        });
        createListener.setDefaultActions(actions);
        if ("HTTPS".equals(action.properties.getProtocol()) || "TLS".equals(action.properties.getProtocol())) {
          createListener.setSslPolicy(action.properties.getSslPolicy());
          final String certificateArn = Stream.ofAll(action.properties.getCertificates()).headOption()
              .map(ElasticLoadBalancingV2CertificateProperties::getCertificateArn).getOrNull();
          if (certificateArn == null) {
            throw new ValidationErrorException("Certificate required for HTTPS/TLS listener");
          }
          final CertificateList certificateList = new CertificateList();
          final Certificate certificate = new Certificate();
          certificate.setCertificateArn(certificateArn);
          certificateList.getMember().add(certificate);
          createListener.setCertificates(certificateList);
        }
        final CreateListenerResponseType createResponse = elbv2.createListener(createListener);
        final Listener listener =
            createResponse.getCreateListenerResult().getListeners().getMember().get(0);
        action.info.setPhysicalResourceId(listener.getListenerArn());
        action.info.setCreatedEnoughToDelete(true);
        action.info.setListenerArn(JsonHelper.getStringFromJsonNode(new TextNode(listener.getListenerArn())));
        action.info.setReferenceValueJson(JsonHelper.getStringFromJsonNode(new TextNode(action.info.getPhysicalResourceId())));
        return action;
      }
    }
  }

  private enum DeleteSteps implements Step {
    DELETE_LISTENER {
      @Override
      public ResourceAction perform(ResourceAction resourceAction) throws Exception {
        final AWSElasticLoadBalancingV2ListenerResourceAction action =
            (AWSElasticLoadBalancingV2ListenerResourceAction) resourceAction;
        if (!Boolean.TRUE.equals(action.info.getCreatedEnoughToDelete())) return action;
        final Loadbalancingv2Api elbv2 = AsyncProxy.client(
            Loadbalancingv2Api.class, MessageHelper.userIdentity(action.info.getEffectiveUserId()));
        final DeleteListenerType deleteListener = new DeleteListenerType();
        deleteListener.setListenerArn(action.info.getPhysicalResourceId());
        try {
          elbv2.deleteListener(deleteListener);
        } catch (final Exception e) {
          if (!AsyncExceptions.isWebServiceErrorCode(e, "ListenerNotFound")) {
            throw e;
          }
        }
        return action;
      }
    }
  }  
}
