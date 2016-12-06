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
