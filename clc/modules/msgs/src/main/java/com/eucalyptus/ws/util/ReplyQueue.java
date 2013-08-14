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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.util;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.message.ExceptionMessage;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.system.Ats;
import com.eucalyptus.ws.EucalyptusWebServiceException;
import com.eucalyptus.ws.WebServicesException;
import com.eucalyptus.ws.protocol.QueryBindingInfo;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;
import edu.ucsb.eucalyptus.msgs.HasRequest;

public class ReplyQueue {
  private static Logger LOG = Logger.getLogger( ReplyQueue.class );
  
  public void handle( BaseMessage responseMessage ) {
    Contexts.response( responseMessage );
  }
  
  public void handle( ExceptionMessage exMsg ) {
    Throwable cause = exMsg.getException( );

    if ( cause instanceof MessagingException ) {
      MessagingException messagingEx = ( ( MessagingException ) cause );
      cause = messagingEx.getCause( );
      Object payload = exMsg.getPayload( );
      BaseMessage msg = convert( payload );
      if( msg != null ) {
        if ( cause instanceof EucalyptusWebServiceException ) {
          final QueryBindingInfo info = Ats.inClassHierarchy( cause.getClass( ) ).get( QueryBindingInfo.class );
          final HttpResponseStatus status = info == null ? HttpResponseStatus.INTERNAL_SERVER_ERROR : new HttpResponseStatus( info.statusCode(), "" );
          Contexts.response( new ExceptionResponseType( msg, ((EucalyptusWebServiceException) cause).getCode( ), cause.getMessage( ), status, cause )  );
        } else {
          Contexts.response( new ExceptionResponseType( msg, cause.getMessage( ), HttpResponseStatus.NOT_ACCEPTABLE, cause )  );
        }
      } else {
        LOG.error( "Failed to identify request context for received error: " + exMsg.toString( ) );
        cause = new WebServicesException( "Failed to identify request context for received error: " + exMsg.toString( ) + " while handling error: " + cause.getMessage( ), cause, HttpResponseStatus.NOT_ACCEPTABLE );
        Contexts.responseError( cause );
      }
    } else if ( cause instanceof MuleException ) {
      LOG.error( "Error service request: " + cause.getMessage( ), cause );
      cause = new WebServicesException( cause.getMessage( ), cause, HttpResponseStatus.NOT_FOUND );
      Contexts.responseError( cause );
    } else {
      Contexts.responseError( cause );
    }
  }

  private BaseMessage convert( Object payload ) {
    BaseMessage ret = null;
    if ( payload instanceof BaseMessage ) {
      ret = ( BaseMessage ) payload;
    } else if ( payload instanceof HasRequest ) {
      ret = ( ( HasRequest ) payload ).getRequest( );
    } else if ( payload instanceof String ) {
      try {
        ret = ( BaseMessage ) BindingManager.getDefaultBinding( ).fromOM( ( String ) payload );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
    } else {
      payload = RequestContext.getEvent( ).getMessage( ).getPayload( );
      if ( payload instanceof BaseMessage ) {
        ret = ( BaseMessage ) payload;
//      } else if ( payload instanceof Allocation ) {
//        ret = ( ( Allocation ) payload ).getRequest( );
      } else if ( payload instanceof String ) {
        try {
          ret = ( BaseMessage ) BindingManager.getDefaultBinding( ).fromOM( ( String ) payload );
        } catch ( Exception ex ) {
          LOG.error( ex , ex );
        }
      }
    }
    return ret;
  }
  
}
