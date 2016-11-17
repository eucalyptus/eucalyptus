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

import com.eucalyptus.portal.common.model.AccountSettings
import com.eucalyptus.portal.common.model.ModifyAccountType
import com.eucalyptus.portal.common.model.PortalMessage
import com.eucalyptus.portal.common.model.ViewAccountResponseType
import com.eucalyptus.portal.common.model.ViewAccountResult
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
  }
}
