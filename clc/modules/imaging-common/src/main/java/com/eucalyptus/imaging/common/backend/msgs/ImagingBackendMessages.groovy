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
@GroovyAddClassUUID
package com.eucalyptus.imaging.common.backend.msgs;
import com.eucalyptus.component.annotation.ComponentMessage

import edu.ucsb.eucalyptus.msgs.BaseMessageMarker;
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import com.eucalyptus.imaging.common.ImagingBackend

@ComponentMessage(ImagingBackend.class)
interface ImagingBackendMessage  extends BaseMessageMarker{ 
}

/** *******************************************************************************/
public class ImportImageType extends com.eucalyptus.imaging.common.ImportImageType implements ImagingBackendMessage { }
public class ImportImageResponseType extends com.eucalyptus.imaging.common.ImportImageResponseType implements ImagingBackendMessage { }
public class DescribeConversionTasksType extends com.eucalyptus.imaging.common.DescribeConversionTasksType implements ImagingBackendMessage { }
public class DescribeConversionTasksResponseType extends com.eucalyptus.imaging.common.DescribeConversionTasksResponseType implements ImagingBackendMessage { }
public class CancelConversionTaskType extends  com.eucalyptus.imaging.common.CancelConversionTaskType implements ImagingBackendMessage { }
public class CancelConversionTaskResponseType extends  com.eucalyptus.imaging.common.CancelConversionTaskResponseType implements ImagingBackendMessage { }
public class PutInstanceImportTaskStatusType extends com.eucalyptus.imaging.common.PutInstanceImportTaskStatusType implements ImagingBackendMessage {
  String sourceIp;
}
public class PutInstanceImportTaskStatusResponseType extends com.eucalyptus.imaging.common.PutInstanceImportTaskStatusResponseType implements ImagingBackendMessage {}
public class GetInstanceImportTaskType extends com.eucalyptus.imaging.common.GetInstanceImportTaskType implements ImagingBackendMessage { 
  String sourceIp;
}
public class GetInstanceImportTaskResponseType extends com.eucalyptus.imaging.common.GetInstanceImportTaskResponseType implements ImagingBackendMessage { }
