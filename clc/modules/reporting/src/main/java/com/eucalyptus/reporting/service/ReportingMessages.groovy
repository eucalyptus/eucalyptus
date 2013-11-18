/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.reporting.service

import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.component.id.Reporting
import com.eucalyptus.reporting.export.ReportingExport
import com.eucalyptus.binding.HttpParameterMapping

import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID

@ComponentMessage(Reporting.class)
class ReportingMessage extends BaseMessage {
}

class ExportReportDataType extends ReportingMessage {
  @HttpParameterMapping (parameter = "Start")
  String startDate
  @HttpParameterMapping (parameter = "End")
  String endDate
  boolean dependencies
}

class ExportReportDataResponseType extends ReportingMessage  {
  ExportDataResultType result
  ReportingResponseMetadataType responseMetadata = new ReportingResponseMetadataType( );
}

class ReportingResponseMetadataType extends EucalyptusData {
  String requestId
}

class ReportingErrorType extends EucalyptusData {
  String type
  String code
  String message
  ReportingErrorDetailType detail = new ReportingErrorDetailType( );
}

class ReportingErrorDetailType extends EucalyptusData {
}

class ReportingErrorResponseType extends ReportingMessage {
  String requestId
  HttpResponseStatus httpStatus;
  ArrayList<ReportingErrorType> errors = new ArrayList<ReportingErrorType>()
}

class ExportDataResultType extends EucalyptusData {
  ExportDataResultType(){}
  ExportDataResultType( data ){ this.data = data }
  ReportingExport data
}

class DeleteReportDataType extends ReportingMessage {
  @HttpParameterMapping (parameter = "End")
  String endDate
}

class DeleteReportDataResponseType extends ReportingMessage  {
  DeleteDataResultType result
  ReportingResponseMetadataType responseMetadata = new ReportingResponseMetadataType( );
}

class DeleteDataResultType extends EucalyptusData {
  DeleteDataResultType(){}
  DeleteDataResultType( count ){ this.count = count }
  int count
}

class GenerateReportType extends ReportingMessage {
  @HttpParameterMapping (parameter = "Start")
  String startDate
  @HttpParameterMapping (parameter = "End")
  String endDate
  String type
  String format
  String timeUnit
  String sizeUnit
  String sizeTimeTimeUnit
  String sizeTimeSizeUnit
}

class GenerateReportResponseType extends ReportingMessage  {
  GenerateReportResultType result
  ReportingResponseMetadataType responseMetadata = new ReportingResponseMetadataType( );
}

class GenerateReportResultType extends EucalyptusData {
  GenerateReportResultType(){}
  GenerateReportResultType( data ){ this.data = data }
  String data
}



