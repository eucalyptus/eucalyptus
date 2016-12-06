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

import com.eucalyptus.portal.common.model.GetTagKeysResponseType
import com.eucalyptus.portal.common.model.GetTagKeysResult
import com.eucalyptus.portal.common.model.GetTagKeysType
import com.eucalyptus.portal.common.model.TagMessage
import com.eucalyptus.ws.protocol.QueryJsonBindingTestSupport
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

/**
 *
 */
class TagServiceQueryBindingTest extends QueryJsonBindingTestSupport {

  @Test
  void testMessageQueryBindings( ) {
    TagServiceQueryBinding binding = new TagServiceQueryBinding( ) {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingForMessageType( TagMessage, operationName )
      }

      @Override
      protected void validateBinding( final com.eucalyptus.binding.Binding currentBinding,
                                      final String operationName,
                                      final Map<String, String> params,
                                      final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

    // GetTagKeys
    bindAndAssertObject( binding, GetTagKeysType, 'GetTagKeys', new GetTagKeysType( ), 0 )
    bindAndAssertParameters( binding, GetTagKeysType, 'GetTagKeys', new GetTagKeysType( ), [:] )
    assertResponse( binding, new GetTagKeysResponseType(
        result: new GetTagKeysResult(
            keys: [
                'Name'
            ]
        )
    ), '''{"keys":["Name"]}''' )
  }
}
