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

import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.portal.common.Portal
import com.eucalyptus.ws.WebServiceError
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
  ModifyAccountResult result = new ModifyAccountResult( )
}

class ViewAccountType extends PortalMessage {
}

class ViewAccountResult extends EucalyptusData {
  AccountSettings accountSettings
}

class ViewAccountResponseType extends PortalMessage {
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

class ViewBillingResult extends EucalyptusData {
  BillingSettings billingSettings = new BillingSettings( )
}

class ViewBillingResponseType extends PortalMessage {
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
