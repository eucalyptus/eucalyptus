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

import com.eucalyptus.portal.common.model.AccountSettings
import com.eucalyptus.portal.common.model.ModifyAccountType
import com.eucalyptus.portal.common.model.PortalMessage
import com.eucalyptus.portal.common.model.ViewAccountResponseType
import com.eucalyptus.portal.common.model.ViewAccountResult
import com.eucalyptus.portal.common.model.ViewMonthlyUsageType
import com.eucalyptus.portal.common.model.ViewUsageType
import com.eucalyptus.ws.protocol.QueryJsonBindingTestSupport
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

/**
 *
 */
class PortalServiceQueryBindingTest extends QueryJsonBindingTestSupport {

  @Test
  void testMessageQueryBindings( ) {
    PortalServiceQueryBinding binding = new PortalServiceQueryBinding( ) {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingForMessageType( PortalMessage, operationName )
      }

      @Override
      protected void validateBinding( final com.eucalyptus.binding.Binding currentBinding,
                                      final String operationName,
                                      final Map<String, String> params,
                                      final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

    // ModifyAccount
    bindAndAssertObject( binding, ModifyAccountType, 'ModifyAccount', new ModifyAccountType(
            userBillingAccess: true
    ), 1 )
    bindAndAssertParameters( binding, ModifyAccountType, 'ModifyAccount', new ModifyAccountType(
            userBillingAccess: true
    ), [
            UserBillingAccess: 'true'
    ] )

    // ViewAccount
    assertResponse( binding, new ViewAccountResponseType(
        result: new ViewAccountResult(
            accountSettings: new AccountSettings(
                userBillingAccess: true
            )
        )
    ), '''{"accountSettings":{"userBillingAccess":true}}''' )

    bindAndAssertObject(binding, ViewUsageType, 'ViewUsage', new ViewUsageType(
            services: 'Ec2',
            usageTypes: 'all',
            operations: 'all',
            timePeriodFrom: (new Date(System.currentTimeMillis()-24*60*60*1000)),
            timePeriodTo: new Date(System.currentTimeMillis()),
            reportGranularity: 'Hours'), 6);

    bindAndAssertObject(binding, ViewMonthlyUsageType, 'ViewMonthlyUsage', new ViewMonthlyUsageType(
            year: '2016',
            month: '12'
    ), 2);
  }
}
