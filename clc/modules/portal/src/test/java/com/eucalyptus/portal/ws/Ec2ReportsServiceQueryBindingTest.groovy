/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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