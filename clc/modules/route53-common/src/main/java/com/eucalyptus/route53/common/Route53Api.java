/*
 * Copyright 2020 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.route53.common;

import com.eucalyptus.component.annotation.ComponentPart;
import com.eucalyptus.route53.common.msgs.*;


@ComponentPart(Route53.class)
public interface Route53Api {

  AssociateVPCWithHostedZoneResponseType associateVPCWithHostedZone(final AssociateVPCWithHostedZoneType request);

  ChangeResourceRecordSetsResponseType changeResourceRecordSets(final ChangeResourceRecordSetsType request);

  ChangeTagsForResourceResponseType changeTagsForResource(final ChangeTagsForResourceType request);

  CreateHealthCheckResponseType createHealthCheck(final CreateHealthCheckType request);

  CreateHostedZoneResponseType createHostedZone(final CreateHostedZoneType request);

  CreateQueryLoggingConfigResponseType createQueryLoggingConfig(final CreateQueryLoggingConfigType request);

  CreateReusableDelegationSetResponseType createReusableDelegationSet(final CreateReusableDelegationSetType request);

  CreateTrafficPolicyResponseType createTrafficPolicy(final CreateTrafficPolicyType request);

  CreateTrafficPolicyInstanceResponseType createTrafficPolicyInstance(final CreateTrafficPolicyInstanceType request);

  CreateTrafficPolicyVersionResponseType createTrafficPolicyVersion(final CreateTrafficPolicyVersionType request);

  CreateVPCAssociationAuthorizationResponseType createVPCAssociationAuthorization(final CreateVPCAssociationAuthorizationType request);

  DeleteHealthCheckResponseType deleteHealthCheck(final DeleteHealthCheckType request);

  DeleteHostedZoneResponseType deleteHostedZone(final DeleteHostedZoneType request);

  DeleteQueryLoggingConfigResponseType deleteQueryLoggingConfig(final DeleteQueryLoggingConfigType request);

  DeleteReusableDelegationSetResponseType deleteReusableDelegationSet(final DeleteReusableDelegationSetType request);

  DeleteTrafficPolicyResponseType deleteTrafficPolicy(final DeleteTrafficPolicyType request);

  DeleteTrafficPolicyInstanceResponseType deleteTrafficPolicyInstance(final DeleteTrafficPolicyInstanceType request);

  DeleteVPCAssociationAuthorizationResponseType deleteVPCAssociationAuthorization(final DeleteVPCAssociationAuthorizationType request);

  DisassociateVPCFromHostedZoneResponseType disassociateVPCFromHostedZone(final DisassociateVPCFromHostedZoneType request);

  GetAccountLimitResponseType getAccountLimit(final GetAccountLimitType request);

  GetChangeResponseType getChange(final GetChangeType request);

  GetCheckerIpRangesResponseType getCheckerIpRanges(final GetCheckerIpRangesType request);

  default GetCheckerIpRangesResponseType getCheckerIpRanges() {
    return getCheckerIpRanges(new GetCheckerIpRangesType());
  }

  GetGeoLocationResponseType getGeoLocation(final GetGeoLocationType request);

  default GetGeoLocationResponseType getGeoLocation() {
    return getGeoLocation(new GetGeoLocationType());
  }

  GetHealthCheckResponseType getHealthCheck(final GetHealthCheckType request);

  GetHealthCheckCountResponseType getHealthCheckCount(final GetHealthCheckCountType request);

  default GetHealthCheckCountResponseType getHealthCheckCount() {
    return getHealthCheckCount(new GetHealthCheckCountType());
  }

  GetHealthCheckLastFailureReasonResponseType getHealthCheckLastFailureReason(final GetHealthCheckLastFailureReasonType request);

  GetHealthCheckStatusResponseType getHealthCheckStatus(final GetHealthCheckStatusType request);

  GetHostedZoneResponseType getHostedZone(final GetHostedZoneType request);

  GetHostedZoneCountResponseType getHostedZoneCount(final GetHostedZoneCountType request);

  default GetHostedZoneCountResponseType getHostedZoneCount() {
    return getHostedZoneCount(new GetHostedZoneCountType());
  }

  GetHostedZoneLimitResponseType getHostedZoneLimit(final GetHostedZoneLimitType request);

  GetQueryLoggingConfigResponseType getQueryLoggingConfig(final GetQueryLoggingConfigType request);

  GetReusableDelegationSetResponseType getReusableDelegationSet(final GetReusableDelegationSetType request);

  GetReusableDelegationSetLimitResponseType getReusableDelegationSetLimit(final GetReusableDelegationSetLimitType request);

  GetTrafficPolicyResponseType getTrafficPolicy(final GetTrafficPolicyType request);

  GetTrafficPolicyInstanceResponseType getTrafficPolicyInstance(final GetTrafficPolicyInstanceType request);

  GetTrafficPolicyInstanceCountResponseType getTrafficPolicyInstanceCount(final GetTrafficPolicyInstanceCountType request);

  default GetTrafficPolicyInstanceCountResponseType getTrafficPolicyInstanceCount() {
    return getTrafficPolicyInstanceCount(new GetTrafficPolicyInstanceCountType());
  }

  ListGeoLocationsResponseType listGeoLocations(final ListGeoLocationsType request);

  default ListGeoLocationsResponseType listGeoLocations() {
    return listGeoLocations(new ListGeoLocationsType());
  }

  ListHealthChecksResponseType listHealthChecks(final ListHealthChecksType request);

  default ListHealthChecksResponseType listHealthChecks() {
    return listHealthChecks(new ListHealthChecksType());
  }

  ListHostedZonesResponseType listHostedZones(final ListHostedZonesType request);

  default ListHostedZonesResponseType listHostedZones() {
    return listHostedZones(new ListHostedZonesType());
  }

  ListHostedZonesByNameResponseType listHostedZonesByName(final ListHostedZonesByNameType request);

  default ListHostedZonesByNameResponseType listHostedZonesByName() {
    return listHostedZonesByName(new ListHostedZonesByNameType());
  }

  ListQueryLoggingConfigsResponseType listQueryLoggingConfigs(final ListQueryLoggingConfigsType request);

  default ListQueryLoggingConfigsResponseType listQueryLoggingConfigs() {
    return listQueryLoggingConfigs(new ListQueryLoggingConfigsType());
  }

  ListResourceRecordSetsResponseType listResourceRecordSets(final ListResourceRecordSetsType request);

  ListReusableDelegationSetsResponseType listReusableDelegationSets(final ListReusableDelegationSetsType request);

  default ListReusableDelegationSetsResponseType listReusableDelegationSets() {
    return listReusableDelegationSets(new ListReusableDelegationSetsType());
  }

  ListTagsForResourceResponseType listTagsForResource(final ListTagsForResourceType request);

  ListTagsForResourcesResponseType listTagsForResources(final ListTagsForResourcesType request);

  ListTrafficPoliciesResponseType listTrafficPolicies(final ListTrafficPoliciesType request);

  default ListTrafficPoliciesResponseType listTrafficPolicies() {
    return listTrafficPolicies(new ListTrafficPoliciesType());
  }

  ListTrafficPolicyInstancesResponseType listTrafficPolicyInstances(final ListTrafficPolicyInstancesType request);

  default ListTrafficPolicyInstancesResponseType listTrafficPolicyInstances() {
    return listTrafficPolicyInstances(new ListTrafficPolicyInstancesType());
  }

  ListTrafficPolicyInstancesByHostedZoneResponseType listTrafficPolicyInstancesByHostedZone(final ListTrafficPolicyInstancesByHostedZoneType request);

  ListTrafficPolicyInstancesByPolicyResponseType listTrafficPolicyInstancesByPolicy(final ListTrafficPolicyInstancesByPolicyType request);

  ListTrafficPolicyVersionsResponseType listTrafficPolicyVersions(final ListTrafficPolicyVersionsType request);

  ListVPCAssociationAuthorizationsResponseType listVPCAssociationAuthorizations(final ListVPCAssociationAuthorizationsType request);

  TestDNSAnswerResponseType testDNSAnswer(final TestDNSAnswerType request);

  UpdateHealthCheckResponseType updateHealthCheck(final UpdateHealthCheckType request);

  UpdateHostedZoneCommentResponseType updateHostedZoneComment(final UpdateHostedZoneCommentType request);

  UpdateTrafficPolicyCommentResponseType updateTrafficPolicyComment(final UpdateTrafficPolicyCommentType request);

  UpdateTrafficPolicyInstanceResponseType updateTrafficPolicyInstance(final UpdateTrafficPolicyInstanceType request);

}
