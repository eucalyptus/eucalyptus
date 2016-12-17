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
package com.eucalyptus.portal.ws

import com.eucalyptus.portal.common.model.Ec2ReportsMessage
import com.eucalyptus.portal.common.model.InstanceUsageFilter
import com.eucalyptus.portal.common.model.InstanceUsageFilters
import com.eucalyptus.portal.common.model.InstanceUsageGroup
import com.eucalyptus.portal.common.model.ViewInstanceUsageReportType
import com.eucalyptus.portal.common.model.ViewReservedInstanceUtilizationReportType
import com.eucalyptus.ws.protocol.QueryJsonBindingTestSupport
import com.google.common.collect.Lists
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

class Ec2ReportsServiceQueryBindingTest  extends QueryJsonBindingTestSupport {
    @Test
    void testMessageQueryBindings() {
        Ec2ReportsServiceQueryBinding binding = new Ec2ReportsServiceQueryBinding() {
            @Override
            protected com.eucalyptus.binding.Binding getBindingWithElementClass(final String operationName) {
                createTestBindingForMessageType(Ec2ReportsMessage, operationName)
            }

            @Override
            protected void validateBinding(final com.eucalyptus.binding.Binding currentBinding,
                                           final String operationName,
                                           final Map<String, String> params,
                                           final BaseMessage eucaMsg) {
                // Validation requires compiled bindings
            }
        }

        // ViewInstanceUsageReport
        bindAndAssertObject(binding, ViewInstanceUsageReportType,
                'ViewInstanceUsageReport', new ViewInstanceUsageReportType(
                granularity: 'Daily',
                timeRangeStart:  (new Date(System.currentTimeMillis()-30*24*60*60*1000)),
                timeRangeEnd: new Date(System.currentTimeMillis()),
                groupBy: new InstanceUsageGroup(
                        type: 'Tag',
                        key: 'User'
                ),
                filters: new InstanceUsageFilters(
                        member: Lists.newArrayList(
                                new InstanceUsageFilter(
                                        type: 'InstanceType',
                                        key: 'm1.small'
                                ),
                                new InstanceUsageFilter(
                                        type: 'Tag',
                                        key: 'User',
                                        value: 'EUCA'
                                ),
                                new InstanceUsageFilter(
                                        type: 'Platforms',
                                        key: 'Linux/Unix'
                                ),
                                new InstanceUsageFilter(
                                        type: 'Platforms',
                                        key: 'Windows'
                                )
                        )
                )
        ), 14);

        // ViewReservedInstanceUtilizationReport
        bindAndAssertObject(binding, ViewReservedInstanceUtilizationReportType,
                'ViewReservedInstanceUtilizationReport', new ViewReservedInstanceUtilizationReportType(), 0);
    }
}