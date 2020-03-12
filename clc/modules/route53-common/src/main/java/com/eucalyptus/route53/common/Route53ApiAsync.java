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
import com.eucalyptus.util.async.CheckedListenableFuture;


@ComponentPart(Route53.class)
public interface Route53ApiAsync {

  CheckedListenableFuture<AssociateVPCWithHostedZoneResponseType> associateVPCWithHostedZoneAsync(final AssociateVPCWithHostedZoneType request);

  CheckedListenableFuture<ChangeResourceRecordSetsResponseType> changeResourceRecordSetsAsync(final ChangeResourceRecordSetsType request);

  CheckedListenableFuture<ChangeTagsForResourceResponseType> changeTagsForResourceAsync(final ChangeTagsForResourceType request);

  CheckedListenableFuture<CreateHealthCheckResponseType> createHealthCheckAsync(final CreateHealthCheckType request);

  CheckedListenableFuture<CreateHostedZoneResponseType> createHostedZoneAsync(final CreateHostedZoneType request);

  CheckedListenableFuture<CreateQueryLoggingConfigResponseType> createQueryLoggingConfigAsync(final CreateQueryLoggingConfigType request);

  CheckedListenableFuture<CreateReusableDelegationSetResponseType> createReusableDelegationSetAsync(final CreateReusableDelegationSetType request);

  CheckedListenableFuture<CreateTrafficPolicyResponseType> createTrafficPolicyAsync(final CreateTrafficPolicyType request);

  CheckedListenableFuture<CreateTrafficPolicyInstanceResponseType> createTrafficPolicyInstanceAsync(final CreateTrafficPolicyInstanceType request);

  CheckedListenableFuture<CreateTrafficPolicyVersionResponseType> createTrafficPolicyVersionAsync(final CreateTrafficPolicyVersionType request);

  CheckedListenableFuture<CreateVPCAssociationAuthorizationResponseType> createVPCAssociationAuthorizationAsync(final CreateVPCAssociationAuthorizationType request);

  CheckedListenableFuture<DeleteHealthCheckResponseType> deleteHealthCheckAsync(final DeleteHealthCheckType request);

  CheckedListenableFuture<DeleteHostedZoneResponseType> deleteHostedZoneAsync(final DeleteHostedZoneType request);

  CheckedListenableFuture<DeleteQueryLoggingConfigResponseType> deleteQueryLoggingConfigAsync(final DeleteQueryLoggingConfigType request);

  CheckedListenableFuture<DeleteReusableDelegationSetResponseType> deleteReusableDelegationSetAsync(final DeleteReusableDelegationSetType request);

  CheckedListenableFuture<DeleteTrafficPolicyResponseType> deleteTrafficPolicyAsync(final DeleteTrafficPolicyType request);

  CheckedListenableFuture<DeleteTrafficPolicyInstanceResponseType> deleteTrafficPolicyInstanceAsync(final DeleteTrafficPolicyInstanceType request);

  CheckedListenableFuture<DeleteVPCAssociationAuthorizationResponseType> deleteVPCAssociationAuthorizationAsync(final DeleteVPCAssociationAuthorizationType request);

  CheckedListenableFuture<DisassociateVPCFromHostedZoneResponseType> disassociateVPCFromHostedZoneAsync(final DisassociateVPCFromHostedZoneType request);

  CheckedListenableFuture<GetAccountLimitResponseType> getAccountLimitAsync(final GetAccountLimitType request);

  CheckedListenableFuture<GetChangeResponseType> getChangeAsync(final GetChangeType request);

  CheckedListenableFuture<GetCheckerIpRangesResponseType> getCheckerIpRangesAsync(final GetCheckerIpRangesType request);

  default CheckedListenableFuture<GetCheckerIpRangesResponseType> getCheckerIpRangesAsync() {
    return getCheckerIpRangesAsync(new GetCheckerIpRangesType());
  }

  CheckedListenableFuture<GetGeoLocationResponseType> getGeoLocationAsync(final GetGeoLocationType request);

  default CheckedListenableFuture<GetGeoLocationResponseType> getGeoLocationAsync() {
    return getGeoLocationAsync(new GetGeoLocationType());
  }

  CheckedListenableFuture<GetHealthCheckResponseType> getHealthCheckAsync(final GetHealthCheckType request);

  CheckedListenableFuture<GetHealthCheckCountResponseType> getHealthCheckCountAsync(final GetHealthCheckCountType request);

  default CheckedListenableFuture<GetHealthCheckCountResponseType> getHealthCheckCountAsync() {
    return getHealthCheckCountAsync(new GetHealthCheckCountType());
  }

  CheckedListenableFuture<GetHealthCheckLastFailureReasonResponseType> getHealthCheckLastFailureReasonAsync(final GetHealthCheckLastFailureReasonType request);

  CheckedListenableFuture<GetHealthCheckStatusResponseType> getHealthCheckStatusAsync(final GetHealthCheckStatusType request);

  CheckedListenableFuture<GetHostedZoneResponseType> getHostedZoneAsync(final GetHostedZoneType request);

  CheckedListenableFuture<GetHostedZoneCountResponseType> getHostedZoneCountAsync(final GetHostedZoneCountType request);

  default CheckedListenableFuture<GetHostedZoneCountResponseType> getHostedZoneCountAsync() {
    return getHostedZoneCountAsync(new GetHostedZoneCountType());
  }

  CheckedListenableFuture<GetHostedZoneLimitResponseType> getHostedZoneLimitAsync(final GetHostedZoneLimitType request);

  CheckedListenableFuture<GetQueryLoggingConfigResponseType> getQueryLoggingConfigAsync(final GetQueryLoggingConfigType request);

  CheckedListenableFuture<GetReusableDelegationSetResponseType> getReusableDelegationSetAsync(final GetReusableDelegationSetType request);

  CheckedListenableFuture<GetReusableDelegationSetLimitResponseType> getReusableDelegationSetLimitAsync(final GetReusableDelegationSetLimitType request);

  CheckedListenableFuture<GetTrafficPolicyResponseType> getTrafficPolicyAsync(final GetTrafficPolicyType request);

  CheckedListenableFuture<GetTrafficPolicyInstanceResponseType> getTrafficPolicyInstanceAsync(final GetTrafficPolicyInstanceType request);

  CheckedListenableFuture<GetTrafficPolicyInstanceCountResponseType> getTrafficPolicyInstanceCountAsync(final GetTrafficPolicyInstanceCountType request);

  default CheckedListenableFuture<GetTrafficPolicyInstanceCountResponseType> getTrafficPolicyInstanceCountAsync() {
    return getTrafficPolicyInstanceCountAsync(new GetTrafficPolicyInstanceCountType());
  }

  CheckedListenableFuture<ListGeoLocationsResponseType> listGeoLocationsAsync(final ListGeoLocationsType request);

  default CheckedListenableFuture<ListGeoLocationsResponseType> listGeoLocationsAsync() {
    return listGeoLocationsAsync(new ListGeoLocationsType());
  }

  CheckedListenableFuture<ListHealthChecksResponseType> listHealthChecksAsync(final ListHealthChecksType request);

  default CheckedListenableFuture<ListHealthChecksResponseType> listHealthChecksAsync() {
    return listHealthChecksAsync(new ListHealthChecksType());
  }

  CheckedListenableFuture<ListHostedZonesResponseType> listHostedZonesAsync(final ListHostedZonesType request);

  default CheckedListenableFuture<ListHostedZonesResponseType> listHostedZonesAsync() {
    return listHostedZonesAsync(new ListHostedZonesType());
  }

  CheckedListenableFuture<ListHostedZonesByNameResponseType> listHostedZonesByNameAsync(final ListHostedZonesByNameType request);

  default CheckedListenableFuture<ListHostedZonesByNameResponseType> listHostedZonesByNameAsync() {
    return listHostedZonesByNameAsync(new ListHostedZonesByNameType());
  }

  CheckedListenableFuture<ListQueryLoggingConfigsResponseType> listQueryLoggingConfigsAsync(final ListQueryLoggingConfigsType request);

  default CheckedListenableFuture<ListQueryLoggingConfigsResponseType> listQueryLoggingConfigsAsync() {
    return listQueryLoggingConfigsAsync(new ListQueryLoggingConfigsType());
  }

  CheckedListenableFuture<ListResourceRecordSetsResponseType> listResourceRecordSetsAsync(final ListResourceRecordSetsType request);

  CheckedListenableFuture<ListReusableDelegationSetsResponseType> listReusableDelegationSetsAsync(final ListReusableDelegationSetsType request);

  default CheckedListenableFuture<ListReusableDelegationSetsResponseType> listReusableDelegationSetsAsync() {
    return listReusableDelegationSetsAsync(new ListReusableDelegationSetsType());
  }

  CheckedListenableFuture<ListTagsForResourceResponseType> listTagsForResourceAsync(final ListTagsForResourceType request);

  CheckedListenableFuture<ListTagsForResourcesResponseType> listTagsForResourcesAsync(final ListTagsForResourcesType request);

  CheckedListenableFuture<ListTrafficPoliciesResponseType> listTrafficPoliciesAsync(final ListTrafficPoliciesType request);

  default CheckedListenableFuture<ListTrafficPoliciesResponseType> listTrafficPoliciesAsync() {
    return listTrafficPoliciesAsync(new ListTrafficPoliciesType());
  }

  CheckedListenableFuture<ListTrafficPolicyInstancesResponseType> listTrafficPolicyInstancesAsync(final ListTrafficPolicyInstancesType request);

  default CheckedListenableFuture<ListTrafficPolicyInstancesResponseType> listTrafficPolicyInstancesAsync() {
    return listTrafficPolicyInstancesAsync(new ListTrafficPolicyInstancesType());
  }

  CheckedListenableFuture<ListTrafficPolicyInstancesByHostedZoneResponseType> listTrafficPolicyInstancesByHostedZoneAsync(final ListTrafficPolicyInstancesByHostedZoneType request);

  CheckedListenableFuture<ListTrafficPolicyInstancesByPolicyResponseType> listTrafficPolicyInstancesByPolicyAsync(final ListTrafficPolicyInstancesByPolicyType request);

  CheckedListenableFuture<ListTrafficPolicyVersionsResponseType> listTrafficPolicyVersionsAsync(final ListTrafficPolicyVersionsType request);

  CheckedListenableFuture<ListVPCAssociationAuthorizationsResponseType> listVPCAssociationAuthorizationsAsync(final ListVPCAssociationAuthorizationsType request);

  CheckedListenableFuture<TestDNSAnswerResponseType> testDNSAnswerAsync(final TestDNSAnswerType request);

  CheckedListenableFuture<UpdateHealthCheckResponseType> updateHealthCheckAsync(final UpdateHealthCheckType request);

  CheckedListenableFuture<UpdateHostedZoneCommentResponseType> updateHostedZoneCommentAsync(final UpdateHostedZoneCommentType request);

  CheckedListenableFuture<UpdateTrafficPolicyCommentResponseType> updateTrafficPolicyCommentAsync(final UpdateTrafficPolicyCommentType request);

  CheckedListenableFuture<UpdateTrafficPolicyInstanceResponseType> updateTrafficPolicyInstanceAsync(final UpdateTrafficPolicyInstanceType request);

}
