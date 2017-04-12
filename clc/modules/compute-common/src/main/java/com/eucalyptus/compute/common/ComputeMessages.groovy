/*************************************************************************
 * Copyright 2009-2016 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

@GroovyAddClassUUID
package com.eucalyptus.compute.common

import com.eucalyptus.binding.HttpParameterMapping
import com.eucalyptus.component.annotation.ComponentMessage
import com.eucalyptus.compute.common.internal.tags.Filters
import com.eucalyptus.system.Threads
import com.eucalyptus.util.Exceptions
import com.eucalyptus.ws.WebServiceError
import edu.ucsb.eucalyptus.msgs.BaseMessage
import edu.ucsb.eucalyptus.msgs.EucalyptusData
import edu.ucsb.eucalyptus.msgs.GroovyAddClassUUID
import org.jboss.netty.handler.codec.http.HttpResponseStatus

/**
 * 
 */
@ComponentMessage(Compute.class)
class ComputeMessage extends BaseMessage implements Cloneable, Serializable {

  public ComputeMessage( ) {
  }

  public ComputeMessage( ComputeMessage msg ) {
    this();
    regarding(msg);
    regardingUserRequest(msg);
    this.userId = msg.userId;
    this.effectiveUserId = msg.effectiveUserId;
    this.correlationId = msg.correlationId;
  }

  public ComputeMessage(final String userId) {
    this();
    this.userId = userId;
    this.effectiveUserId = userId;
  }

  protected Set<String> getTagKeys(
      List<ResourceTagSpecification> tagSpecifications,
      String resourceType,
      String resourceId
  ) {
    ( tagSpecifications && resourceId == null ?
         tagSpecifications.find{ it.resourceType==resourceType }?.tagSet?.collect{ it.key }?:[] :
         [] ) as Set<String>
  }

  protected String getTagValue(
      List<ResourceTagSpecification> tagSpecifications,
      String resourceType,
      String resourceId,
      String tagKey
  ) {
    String value = null
    if ( tagSpecifications && resourceId == null  ) {
      specifications:
      for ( ResourceTagSpecification tagSpecification : tagSpecifications ) {
        if ( tagSpecification.resourceType==resourceType && tagSpecification.tagSet ) {
          for ( ResourceTag tag : tagSpecification.tagSet ) {
            if ( tag.key == tagKey ) {
              value = tag.value
              break specifications
            }
          }
        }
      }
    }
    value
  }
}

@ComponentMessage(Compute.class)
public class ExceptionResponseType extends BaseMessage {
  String source = "not available";
  String message = "not available";
  String requestType = "not available";
  Throwable exception;
  String error = "not available";
  HttpResponseStatus httpStatus = HttpResponseStatus.BAD_REQUEST;
  public ExceptionResponseType() {
  }
  public ExceptionResponseType( BaseMessage msg, String message, Throwable exception ) {
    this( msg, message, HttpResponseStatus.BAD_REQUEST, exception );
  }
  public ExceptionResponseType( BaseMessage msg, String message, HttpResponseStatus httpStatus, Throwable exception ) {
    this( msg, msg?.getClass()?.getSimpleName(), message, httpStatus, exception )
  }
  public ExceptionResponseType( BaseMessage msg, String requestType, String message, HttpResponseStatus httpStatus, Throwable exception ) {
    super( msg );
    this.httpStatus = httpStatus;
    this.source = exception.getClass( ).getCanonicalName( );
    this.message = message?:exception.getMessage()?:exception.getClass()
    this.requestType = requestType
    this.exception = exception;
    if( this.exception != null ) {
      this.error = Exceptions.string( exception );
    } else {
      this.error = '';
    }
    this.set_return(false);
  }
}

public class EucalyptusErrorMessageType extends ComputeMessage {

  String source;
  String message;
  String requestType = "not available";
  Throwable exception;

  public EucalyptusErrorMessageType() {
  }

  public EucalyptusErrorMessageType(String source, String message) {
    this.source = source;
    this.message = message;
  }

  public EucalyptusErrorMessageType(String source, BaseMessage msg, String message) {
    this(source, message);
    regardingUserRequest( msg );
    this.requestType = msg != null ? msg.getClass().getSimpleName() : this.requestType;
  }

  public String toString() {
    return String.format("SERVICE: %s PROBLEM: %s MSG-TYPE: %s", this.source, this.message, this.requestType);
  }
}

public class Filter extends EucalyptusData {
  String name;
  @HttpParameterMapping (parameter = "Value")
  ArrayList<String> valueSet = new ArrayList<String>( );

  Filter( ) {
  }

  /**
   * Create a filter with any wildcards in the value escaped.
   */
  static Filter filter( String name, String value ) {
    Filter filter = new Filter( )
    filter.name = name
    filter.valueSet.add( Filters.escape( value ) )
    filter
  }

  /**
   * Create a filter with any wildcards in the values escaped.
   */
  static Filter filter( String name, Iterable<String> values ) {
    Filter filter = new Filter( )
    filter.name = name
    values.each { String value -> filter.valueSet.add( Filters.escape( value ) ) }
    filter
  }
}

public class ErrorDetail extends EucalyptusData {
  String type
  String code
  String message
  String stackTrace
  public ErrorDetail() {  }
}

public class ErrorResponse extends ComputeMessage implements WebServiceError {
  String requestId
  ArrayList<ErrorDetail> error = new ArrayList<ErrorDetail>( )

  ErrorResponse( ) {
    set_return( false )
  }

  @Override
  String toSimpleString( ) {
    "${error?.getAt(0)?.type} error (${webServiceErrorCode}): ${webServiceErrorMessage}"
  }

  @Override
  String getWebServiceErrorCode( ) {
    error?.getAt(0)?.code
  }

  @Override
  String getWebServiceErrorMessage( ) {
    error?.getAt(0)?.message
  }
}