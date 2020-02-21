/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.service;

import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.route53.common.msgs.*;

/**
 *
 */
@ComponentNamed
public class Route53Service {

  public AssociateVPCWithHostedZoneResponseType associateVPCWithHostedZone(final AssociateVPCWithHostedZoneType request) {
    return request.getReply();
  }

  public ChangeResourceRecordSetsResponseType changeResourceRecordSets(final ChangeResourceRecordSetsType request) {
    return request.getReply();
  }

  public ChangeTagsForResourceResponseType changeTagsForResource(final ChangeTagsForResourceType request) {
    return request.getReply();
  }

  public CreateHealthCheckResponseType createHealthCheck(final CreateHealthCheckType request) {
    return request.getReply();
  }

  public CreateHostedZoneResponseType createHostedZone(final CreateHostedZoneType request) {
    return request.getReply();
  }

  public CreateQueryLoggingConfigResponseType createQueryLoggingConfig(final CreateQueryLoggingConfigType request) {
    return request.getReply();
  }

  public CreateReusableDelegationSetResponseType createReusableDelegationSet(final CreateReusableDelegationSetType request) {
    return request.getReply();
  }

  public CreateTrafficPolicyResponseType createTrafficPolicy(final CreateTrafficPolicyType request) {
    return request.getReply();
  }

  public CreateTrafficPolicyInstanceResponseType createTrafficPolicyInstance(final CreateTrafficPolicyInstanceType request) {
    return request.getReply();
  }

  public CreateTrafficPolicyVersionResponseType createTrafficPolicyVersion(final CreateTrafficPolicyVersionType request) {
    return request.getReply();
  }

  public CreateVPCAssociationAuthorizationResponseType createVPCAssociationAuthorization(final CreateVPCAssociationAuthorizationType request) {
    return request.getReply();
  }

  public DeleteHealthCheckResponseType deleteHealthCheck(final DeleteHealthCheckType request) {
    return request.getReply();
  }

  public DeleteHostedZoneResponseType deleteHostedZone(final DeleteHostedZoneType request) {
    return request.getReply();
  }

  public DeleteQueryLoggingConfigResponseType deleteQueryLoggingConfig(final DeleteQueryLoggingConfigType request) {
    return request.getReply();
  }

  public DeleteReusableDelegationSetResponseType deleteReusableDelegationSet(final DeleteReusableDelegationSetType request) {
    return request.getReply();
  }

  public DeleteTrafficPolicyResponseType deleteTrafficPolicy(final DeleteTrafficPolicyType request) {
    return request.getReply();
  }

  public DeleteTrafficPolicyInstanceResponseType deleteTrafficPolicyInstance(final DeleteTrafficPolicyInstanceType request) {
    return request.getReply();
  }

  public DeleteVPCAssociationAuthorizationResponseType deleteVPCAssociationAuthorization(final DeleteVPCAssociationAuthorizationType request) {
    return request.getReply();
  }

  public DisassociateVPCFromHostedZoneResponseType disassociateVPCFromHostedZone(final DisassociateVPCFromHostedZoneType request) {
    return request.getReply();
  }

  public GetAccountLimitResponseType getAccountLimit(final GetAccountLimitType request) {
    return request.getReply();
  }

  public GetChangeResponseType getChange(final GetChangeType request) {
    return request.getReply();
  }

  public GetCheckerIpRangesResponseType getCheckerIpRanges(final GetCheckerIpRangesType request) {
    return request.getReply();
  }

  public GetGeoLocationResponseType getGeoLocation(final GetGeoLocationType request) {
    return request.getReply();
  }

  public GetHealthCheckResponseType getHealthCheck(final GetHealthCheckType request) {
    return request.getReply();
  }

  public GetHealthCheckCountResponseType getHealthCheckCount(final GetHealthCheckCountType request) {
    return request.getReply();
  }

  public GetHealthCheckLastFailureReasonResponseType getHealthCheckLastFailureReason(final GetHealthCheckLastFailureReasonType request) {
    return request.getReply();
  }

  public GetHealthCheckStatusResponseType getHealthCheckStatus(final GetHealthCheckStatusType request) {
    return request.getReply();
  }

  public GetHostedZoneResponseType getHostedZone(final GetHostedZoneType request) {
    return request.getReply();
  }

  public GetHostedZoneCountResponseType getHostedZoneCount(final GetHostedZoneCountType request) {
    return request.getReply();
  }

  public GetHostedZoneLimitResponseType getHostedZoneLimit(final GetHostedZoneLimitType request) {
    return request.getReply();
  }

  public GetQueryLoggingConfigResponseType getQueryLoggingConfig(final GetQueryLoggingConfigType request) {
    return request.getReply();
  }

  public GetReusableDelegationSetResponseType getReusableDelegationSet(final GetReusableDelegationSetType request) {
    return request.getReply();
  }

  public GetReusableDelegationSetLimitResponseType getReusableDelegationSetLimit(final GetReusableDelegationSetLimitType request) {
    return request.getReply();
  }

  public GetTrafficPolicyResponseType getTrafficPolicy(final GetTrafficPolicyType request) {
    return request.getReply();
  }

  public GetTrafficPolicyInstanceResponseType getTrafficPolicyInstance(final GetTrafficPolicyInstanceType request) {
    return request.getReply();
  }

  public GetTrafficPolicyInstanceCountResponseType getTrafficPolicyInstanceCount(final GetTrafficPolicyInstanceCountType request) {
    return request.getReply();
  }

  public ListGeoLocationsResponseType listGeoLocations(final ListGeoLocationsType request) {
    return request.getReply();
  }

  public ListHealthChecksResponseType listHealthChecks(final ListHealthChecksType request) {
    return request.getReply();
  }

  public ListHostedZonesResponseType listHostedZones(final ListHostedZonesType request) {
    return request.getReply();
  }

  public ListHostedZonesByNameResponseType listHostedZonesByName(final ListHostedZonesByNameType request) {
    return request.getReply();
  }

  public ListQueryLoggingConfigsResponseType listQueryLoggingConfigs(final ListQueryLoggingConfigsType request) {
    return request.getReply();
  }

  public ListResourceRecordSetsResponseType listResourceRecordSets(final ListResourceRecordSetsType request) {
    return request.getReply();
  }

  public ListReusableDelegationSetsResponseType listReusableDelegationSets(final ListReusableDelegationSetsType request) {
    return request.getReply();
  }

  public ListTagsForResourceResponseType listTagsForResource(final ListTagsForResourceType request) {
    return request.getReply();
  }

  public ListTagsForResourcesResponseType listTagsForResources(final ListTagsForResourcesType request) {
    return request.getReply();
  }

  public ListTrafficPoliciesResponseType listTrafficPolicies(final ListTrafficPoliciesType request) {
    return request.getReply();
  }

  public ListTrafficPolicyInstancesResponseType listTrafficPolicyInstances(final ListTrafficPolicyInstancesType request) {
    return request.getReply();
  }

  public ListTrafficPolicyInstancesByHostedZoneResponseType listTrafficPolicyInstancesByHostedZone(final ListTrafficPolicyInstancesByHostedZoneType request) {
    return request.getReply();
  }

  public ListTrafficPolicyInstancesByPolicyResponseType listTrafficPolicyInstancesByPolicy(final ListTrafficPolicyInstancesByPolicyType request) {
    return request.getReply();
  }

  public ListTrafficPolicyVersionsResponseType listTrafficPolicyVersions(final ListTrafficPolicyVersionsType request) {
    return request.getReply();
  }

  public ListVPCAssociationAuthorizationsResponseType listVPCAssociationAuthorizations(final ListVPCAssociationAuthorizationsType request) {
    return request.getReply();
  }

  public TestDNSAnswerResponseType testDNSAnswer(final TestDNSAnswerType request) {
    return request.getReply();
  }

  public UpdateHealthCheckResponseType updateHealthCheck(final UpdateHealthCheckType request) {
    return request.getReply();
  }

  public UpdateHostedZoneCommentResponseType updateHostedZoneComment(final UpdateHostedZoneCommentType request) {
    return request.getReply();
  }

  public UpdateTrafficPolicyCommentResponseType updateTrafficPolicyComment(final UpdateTrafficPolicyCommentType request) {
    return request.getReply();
  }

  public UpdateTrafficPolicyInstanceResponseType updateTrafficPolicyInstance(final UpdateTrafficPolicyInstanceType request) {
    return request.getReply();
  }

}
