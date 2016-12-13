/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
@GroovyAddClassUUID
package com.eucalyptus.portal.common.model

import com.eucalyptus.auth.policy.annotation.PolicyAction
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.portal.common.Portal
import com.eucalyptus.portal.common.policy.PortalPolicySpec
import com.eucalyptus.ws.WebServiceError
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import static com.eucalyptus.util.MessageValidation.validateRecursively

@ComponentMessage(Portal)
class PortalMessage extends PortalBaseMessage {

  public Map<String,String> validate( ) {
    return validateRecursively(
        Maps.newTreeMap( ),
        new PortalBaseMessage.PortalBaseMessageValidationAssistant( ),
        "",
        this )
  }
}

class ViewUsageType extends PortalMessage {
  String services;
  String usageTypes;
  String operations;
  Date timePeriodFrom;
  Date timePeriodTo;
  String reportGranularity;
}

class ViewUsageResult extends EucalyptusData {
  ViewUsageResult() { }
  ViewUsageResult(data) {
    this.data = data
  }
  String data;
}

class ViewUsageResponseType extends PortalMessage {
  @JsonUnwrapped
  ViewUsageResult result = new ViewUsageResult();
}

@PolicyAction(vendor=PortalPolicySpec.VENDOR_PORTAL, action="viewbilling")
class ViewMonthlyUsageType extends PortalMessage {
  String year;
  String month;
}

class ViewMonthlyUsageResult extends EucalyptusData {
  ViewMonthlyUsageResult() { }
  ViewMonthlyUsageResult(data) {
    this.data = data
  }
  String data;
}

class ViewMonthlyUsageResponseType extends PortalMessage {
  @JsonUnwrapped
  ViewMonthlyUsageResult result = new ViewMonthlyUsageResult();
}

class ModifyAccountType extends PortalMessage {
  Boolean userBillingAccess
}

class AccountSettings extends EucalyptusData {
  Boolean userBillingAccess

  AccountSettings withUserBillingAccess( Boolean userBillingAccess ) {
    this.userBillingAccess = userBillingAccess
    this
  }
}

class ModifyAccountResult extends EucalyptusData {
  AccountSettings accountSettings
}

class ModifyAccountResponseType extends PortalMessage {
  @JsonUnwrapped
  ModifyAccountResult result = new ModifyAccountResult( )
}

class ViewAccountType extends PortalMessage {
}

class ViewAccountResult extends EucalyptusData {
  AccountSettings accountSettings
}

class ViewAccountResponseType extends PortalMessage {
  @JsonUnwrapped
  ViewAccountResult result = new ViewAccountResult( )
}

class ModifyBillingType extends PortalMessage {
  String reportBucket
  Boolean detailedBillingEnabled = false
  ArrayList<String> activeCostAllocationTags = Lists.newArrayList( )
}

class ModifyBillingResult extends EucalyptusData {
  BillingSettings billingSettings = new BillingSettings( )
}

class ModifyBillingResponseType extends PortalMessage {
  @JsonUnwrapped
  ModifyBillingResult result = new ModifyBillingResult( )
}

class ViewBillingType extends PortalMessage {
}

class BillingSettings extends EucalyptusData {
  String reportBucket
  Boolean detailedBillingEnabled = false
  ArrayList<String> activeCostAllocationTags = Lists.newArrayList( )

  BillingSettings withReportBucket( String reportBucket ) {
    this.reportBucket = reportBucket
    this
  }

  BillingSettings withDetailedBillingEnabled( Boolean detailedBillingEnabled ) {
    this.detailedBillingEnabled = detailedBillingEnabled
    this
  }

  BillingSettings withActiveCostAllocationTags( Iterable<String> activeCostAllocationTags ) {
    this.activeCostAllocationTags = Lists.newArrayList( activeCostAllocationTags )
    this
  }
}

class BillingMetadata extends EucalyptusData {
  ArrayList<String> inactiveCostAllocationTags = Lists.newArrayList( )

  BillingMetadata withInactiveCostAllocationTags( Iterable<String> inactiveCostAllocationTags ) {
    this.inactiveCostAllocationTags = Lists.newArrayList( inactiveCostAllocationTags )
    this
  }
}

class ViewBillingResult extends EucalyptusData {
  BillingSettings billingSettings = new BillingSettings( )
  BillingMetadata billingMetadata = new BillingMetadata( )
}

class ViewBillingResponseType extends PortalMessage {
  @JsonUnwrapped
  ViewBillingResult result = new ViewBillingResult( )
}

class PortalError extends EucalyptusData {
  String type
  String code
  String message
}

class PortalErrorResponse extends PortalMessage implements WebServiceError {
  String requestId
  ArrayList<PortalError> error = Lists.newArrayList( )

  PortalErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @Override
  String getWebServiceErrorCode( ) {
    error?.getAt(0)?.code
  }

  @Override
  String getWebServiceErrorMessage( ) {
    error?.getAt(0)?.message
  }
}
