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
package com.eucalyptus.cloudwatch.ws;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jibx.runtime.JiBXException;

import com.eucalyptus.binding.BindingException;
import com.eucalyptus.cloudwatch.BadRequestException;
import com.eucalyptus.cloudwatch.CloudWatchException;
import com.eucalyptus.cloudwatch.MissingParameterException;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.protocol.BaseQueryBinding;
import com.eucalyptus.ws.protocol.OperationParameter;
import com.eucalyptus.ws.protocol.QueryBindingInfo;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessageSupplier;


public class CloudWatchQueryBinding extends BaseQueryBinding<OperationParameter> {

  static final String CLOUDWATCH_NAMESPACE_PATTERN = "http://monitoring.amazonaws.com/doc/%s/"; 
  static final String CLOUDWATCH_DEFAULT_VERSION = "2010-08-01";              
  static final String CLOUDWATCH_DEFAULT_NAMESPACE = String.format( CLOUDWATCH_NAMESPACE_PATTERN, CLOUDWATCH_DEFAULT_VERSION );
  private static final Logger LOG = Logger.getLogger(CloudWatchQueryBinding.class);
  public CloudWatchQueryBinding() {
    super( CLOUDWATCH_NAMESPACE_PATTERN, CLOUDWATCH_DEFAULT_VERSION, OperationParameter.Action );
  }

  @Override
  public Object bind(MappingHttpRequest httpRequest) throws BindingException {
    try {
      return super.bind(httpRequest);
    } catch (BindingException ex) {
      final JiBXException cause = Exceptions.findCause(ex, JiBXException.class);
      if (cause != null && cause.getMessage() != null) {
        // Hack: convert exceptions with null values (i.e. required parameter) into
        // MissingParameterExceptions
        // Change to MissingParameterException:
        // Look for null value for element "{http://monitoring.amazonaws.com/doc/2010-08-01/}Namespace" from object
        if (cause.getMessage().startsWith("null value for element ")) {
          String element = cause.getMessage().substring(cause.getMessage().indexOf("}") + 1, cause.getMessage().indexOf("\" from object"));
          throw new WrappedBindingException(new MissingParameterException("The parameter " + element + " is required."), httpRequest);
        // deal with a couple of issues that are client related (bad data)
        } else if (cause.getMessage().startsWith("Error writing marshalled document")) {
          final IOException jibxCause = Exceptions.findCause(cause, IOException.class);
          if (jibxCause != null && jibxCause.getMessage() != null && 
              jibxCause.getMessage().startsWith("Error writing to stream: Invalid")) {
            throw new WrappedBindingException(new BadRequestException(jibxCause.getMessage()), httpRequest); 
          }
        }
        // TODO: deal with date format issues
      } 
      throw ex;
    }
  }

  @Override
  public void handleUpstream(ChannelHandlerContext ctx,
      ChannelEvent channelEvent) throws Exception {
    try {
      super.handleUpstream(ctx, channelEvent);
    } catch (Exception ex) {
      final WrappedBindingException cause = Exceptions.findCause(ex, WrappedBindingException.class);
      if (cause != null) {
        CloudWatchException cloudWatchException = Exceptions.findCause(cause, CloudWatchException.class);
        if (cloudWatchException != null) {
          final QueryBindingInfo info = Ats.inClassHierarchy( cloudWatchException.getClass() ).get( QueryBindingInfo.class );
          HttpResponseStatus status = info == null ? HttpResponseStatus.INTERNAL_SERVER_ERROR : new HttpResponseStatus( info.statusCode(), cloudWatchException.getCode() );
          final BaseMessage errorResp = new CloudWatchErrorHandler().buildErrorResponse( 
            cause.getMappingHttpRequest().getCorrelationId(),
            cloudWatchException.getRole(),
            cloudWatchException.getCode(),
            cloudWatchException.getMessage()
            );
          Contexts.response( new BaseMessageSupplier( errorResp, status ) );
          return;
        }
      }
      throw ex;
    }
  }
}
