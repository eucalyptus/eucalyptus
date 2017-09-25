/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
package com.eucalyptus.auth.ws

import com.eucalyptus.auth.euare.common.msgs.CreateOpenIdConnectProviderType
import com.eucalyptus.auth.euare.common.msgs.DeleteOpenIdConnectProviderType
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

  @Test
  void testDeleteOpenIdConnectProvider( ) {
    bindAndAssertParameters(
        DeleteOpenIdConnectProviderType,
        "DeleteOpenIdConnectProvider",
        new DeleteOpenIdConnectProviderType(
          openIDConnectProviderArn: 'arn:aws:iam::123456789012:oidc-provider/auth.globus.org'
        ) ,
        [
          OpenIDConnectProviderArn  : 'arn:aws:iam::123456789012:oidc-provider/auth.globus.org'
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
