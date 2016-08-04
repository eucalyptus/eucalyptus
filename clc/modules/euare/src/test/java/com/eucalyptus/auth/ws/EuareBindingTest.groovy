/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.auth.ws

import com.eucalyptus.auth.euare.CreateOpenIdConnectProviderType
import com.eucalyptus.ws.protocol.QueryBindingTestSupport
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.junit.Test

/**
 * 
 */
class EuareBindingTest extends QueryBindingTestSupport {

  @Test
  void testValidBinding() {
    URL resource = EuareBindingTest.class.getResource( '/euare-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = EuareBindingTest.class.getResource( '/euare-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testCreateOpenIdConnectProvider( ) {
    bindAndAssertParameters(
        CreateOpenIdConnectProviderType,
        "CreateOpenIdConnectProvider",
        new CreateOpenIdConnectProviderType(
          url: 'https://auth.globus.org/',
          clientIDList: [ '659067ec-9698-44a8-88ea-db31e071447a' ],
          thumbprintList: [ 'e26e90c1e76c7fc02d63c71913cc2291c52d8b58' ],
        ) ,
        [
          Url                       : 'https://auth.globus.org/',
          'ClientIDList.member.1'   : '659067ec-9698-44a8-88ea-db31e071447a',
          'ThumbprintList.member.1' : 'e26e90c1e76c7fc02d63c71913cc2291c52d8b58'
        ]
    )
  }

  def <T> T bindAndAssertParameters(
      final Class<T> messageClass,
      final String action,
      final Object bean,
      final Map<String, String> parameters) {
    return super.bindAndAssertParameters(getBinding( ), messageClass, action, bean, parameters)
  }

  private EuareQueryBinding getBinding( ) {
    URL resource = EuareBindingTest.class.getResource( '/euare-binding.xml' )

    String version = "2010-05-08"
    new EuareQueryBinding( ) {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass(final String operationName) {
        createTestBindingFromXml(resource, operationName)
      }

      @Override
      String getNamespace() {
        return getNamespaceForVersion(version);
      }

      @Override
      protected void validateBinding(final com.eucalyptus.binding.Binding currentBinding,
                                     final String operationName,
                                     final Map<String, String> params,
                                     final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    }

  }
}
