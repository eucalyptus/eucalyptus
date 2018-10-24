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
package com.eucalyptus.ws.protocol

import com.eucalyptus.binding.Binding
import com.google.common.collect.Sets
import edu.ucsb.eucalyptus.msgs.BaseMessage
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

import static org.junit.Assert.*

/**
 *
 */
class QueryJsonBindingTestSupport extends QueryRequestBindingTestSupport {

  Binding createTestBindingForMessageType(Class messageType, String elementClass ) {
    ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider( false )
    scanner.addIncludeFilter( new AssignableTypeFilter( messageType ) );
    Set<String> messageClasses = Sets.newTreeSet( scanner.findCandidateComponents( messageType.package.name )*.beanClassName )
    List<Class> requestMessageClasses = messageClasses.collect( Class.&forName )
    assertFalse( "No classes found for binding", requestMessageClasses.isEmpty() )
    requestMessageClasses.find { Class clazz -> clazz.getSimpleName().equals(elementClass) } != null ?
        new TestBinding( requestMessageClasses  ) :
        null
  }

  void assertResponse(BaseQueryJsonBinding binding, BaseMessage bean, String expected ) {
    String actual = binding.marshall( bean )
    assertEquals( "Content", expected, actual )
  }
}
