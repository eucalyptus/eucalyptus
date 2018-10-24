/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.imaging.service.ws

import com.eucalyptus.imaging.common.msgs.GetInstanceImportTaskType
import com.eucalyptus.imaging.common.msgs.PutInstanceImportTaskStatusType
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
  void testInternalRoundTrip() {
    URL resource = ImagingQueryBindingTest.getResource('/imaging-binding.xml')
    assertValidInternalRoundTrip( resource )
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
