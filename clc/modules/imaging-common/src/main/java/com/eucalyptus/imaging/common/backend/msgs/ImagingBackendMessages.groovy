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
@GroovyAddClassUUID
package com.eucalyptus.imaging.common.backend.msgs;
import com.eucalyptus.component.annotation.ComponentMessage

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

import com.eucalyptus.imaging.common.ImagingBackend

@ComponentMessage(ImagingBackend.class)
interface ImagingBackendMessage { 
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
