/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.imaging.service.ws

import com.eucalyptus.imaging.PutInstanceImportTaskStatusType
import com.eucalyptus.imaging.GetInstanceImportTaskType
import com.eucalyptus.imaging.ws.ImagingQueryBinding
import com.eucalyptus.ws.protocol.QueryBindingTestSupport

import edu.ucsb.eucalyptus.msgs.BaseMessage

import org.junit.Test

/**
 *
 */
class ImagingQueryBindingTest extends QueryBindingTestSupport {
  @Test
  void testValidBinding() {
    URL resource = ImagingQueryBindingTest.class.getResource( '/imaging-binding.xml' )
    assertValidBindingXml( resource )
  }

  @Test
  void testValidQueryBinding() {
    URL resource = ImagingQueryBindingTest.class.getResource( '/imaging-binding.xml' )
    assertValidQueryBinding( resource )
  }

  @Test
  void testMessageQueryBindings() {
    URL resource = ImagingQueryBindingTest.class.getResource( '/imaging-binding.xml' )
    ImagingQueryBinding iqb = new ImagingQueryBinding() {
      @Override
      protected com.eucalyptus.binding.Binding getBindingWithElementClass( final String operationName ) {
        createTestBindingFromXml( resource, operationName )
      }

      @Override
      protected void validateBinding( final com.eucalyptus.binding.Binding currentBinding,
                                      final String operationName,
                                      final Map<String, String> params,
                                      final BaseMessage eucaMsg) {
        // Validation requires compiled bindings
      }
    };

    // Get task
    bindAndAssertObject( iqb, GetInstanceImportTaskType.class, "GetInstanceImportTaskType", new GetInstanceImportTaskType( ), 0 );
  
    // Put status
    bindAndAssertObject( iqb, PutInstanceImportTaskStatusType.class, "PutInstanceImportTaskStatus", new PutInstanceImportTaskStatusType(
      importTaskId : 'import-vol-123',
      status: 'DONE',
      volumeId: 'vol-123',
      message: 'Done',
      bytesConverted: 10L), 5
    );
  
    bindAndAssertParameters( iqb, PutInstanceImportTaskStatusType.class, "PutInstanceImportTaskStatus", new PutInstanceImportTaskStatusType(
      importTaskId : 'import-vol-123',
      status: 'DONE',
      volumeId: 'vol-123',
      ),[
        'ImportTaskId': 'import-vol-123',
        'Status': 'DONE',
        'VolumeId': 'vol-123'
      ])
  }
}
