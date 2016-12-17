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
package com.eucalyptus.portal.common.model

import com.eucalyptus.auth.policy.annotation.PolicyAction
import com.eucalyptus.binding.HttpEmbedded
import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.portal.common.Ec2Reports
import com.eucalyptus.portal.common.policy.Ec2ReportsPolicySpec
import com.eucalyptus.ws.WebServiceError
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import edu.ucsb.eucalyptus.msgs.EucalyptusData

import javax.annotation.Nonnull

import static com.eucalyptus.util.MessageValidation.validateRecursively

@ComponentMessage(Ec2Reports)
class Ec2ReportsMessage extends PortalBaseMessage {

    public Map<String,String> validate( ) {
        return validateRecursively(
                Maps.newTreeMap( ),
                new PortalBaseMessage.PortalBaseMessageValidationAssistant( ),
                "",
                this )
    }
}

@PolicyAction(vendor=Ec2ReportsPolicySpec.VENDOR_EC2REPORTS, action="viewinstanceusagereport")
class ViewInstanceUsageReportType extends Ec2ReportsMessage {
    String granularity;
    InstanceUsageFilters filters;
    Date timeRangeStart;
    Date timeRangeEnd;
    InstanceUsageGroup groupBy;
}

class InstanceUsageFilters extends EucalyptusData {
    InstanceUsageFilters() {  }

    @HttpEmbedded(multiple=true)
    @HttpParameterMapping(parameter="member")
    ArrayList<InstanceUsageFilter> member = new ArrayList<InstanceUsageFilter>();
}

class InstanceUsageFilter extends EucalyptusData {
    InstanceUsageFilter() { }
    @Nonnull
    String type;

    @Nonnull
    String key;

    String value;
}

class InstanceUsageGroup extends EucalyptusData {
    InstanceUsageGroup() { }
    @Nonnull
    String type;

    String key;
}
class ViewInstanceUsageResult extends EucalyptusData {
    ViewInstanceUsageResult() { }
    ViewInstanceUsageResult(data) {
        this.usageReport = data
    }
    String usageReport;
}

class ViewInstanceUsageReportResponseType extends Ec2ReportsMessage {
    @JsonUnwrapped
    ViewInstanceUsageResult result = new ViewInstanceUsageResult();
}

@PolicyAction(vendor=Ec2ReportsPolicySpec.VENDOR_EC2REPORTS, action="viewreservedinstanceutilizationreport")
class ViewReservedInstanceUtilizationReportType extends Ec2ReportsMessage { }

class ViewReservedInstanceUtilizationResult extends EucalyptusData {
    ViewReservedInstanceUtilizationResult() { }
    ViewReservedInstanceUtilizationResult(data) {
        this.utilizationReport = data
    }
    String utilizationReport;
}

class ViewReservedInstanceUtilizationReportResponseType extends Ec2ReportsMessage {
    @JsonUnwrapped
    ViewReservedInstanceUtilizationResult result = new ViewReservedInstanceUtilizationResult();
}

class Ec2ReportsError extends EucalyptusData {
    String type
    String code
    String message
}

class Ec2ReportsErrorResponse extends Ec2ReportsMessage implements WebServiceError {
    String requestId
    ArrayList<Ec2ReportsError> error = Lists.newArrayList( )

    Ec2ReportsErrorResponse( ) {
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


