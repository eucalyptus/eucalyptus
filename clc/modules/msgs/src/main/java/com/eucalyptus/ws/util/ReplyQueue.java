/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.util;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.message.ExceptionMessage;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import com.eucalyptus.context.ServiceContext;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.ws.WebServicesException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

public class ReplyQueue {
  public static Logger LOG = Logger.getLogger( ReplyQueue.class );
  
  public void handle( BaseMessage responseMessage ) {
    ServiceContext.response( responseMessage );
  }
  
  public void handle( ExceptionMessage exMsg ) {
    Throwable cause = exMsg.getException( );
    EventRecord.here( ReplyQueue.class, EventType.MSG_REPLY, cause.getClass( ).getCanonicalName( ), cause.getMessage( ) ).debug( );

    if ( cause instanceof MessagingException ) {
      MessagingException messagingEx = ( ( MessagingException ) cause );
      cause = messagingEx.getCause( );
      MuleMessage muleMsg = messagingEx.getUmoMessage( );
      Object payload = muleMsg.getPayload( );
      BaseMessage msg = convert( payload );
      if( msg != null ) {
        this.handle( new ExceptionResponseType( msg, cause.getMessage( ), HttpResponseStatus.NOT_ACCEPTABLE, cause )  );
        return;
      } else {
        LOG.error( "Failed to identify request context for recieved error: " + exMsg.toString( ) );
      }
    } else if ( cause instanceof MuleException ) {
      LOG.error( "Error service request: " + cause.getMessage( ), cause );
      cause = new WebServicesException( cause.getMessage( ), cause, HttpResponseStatus.NOT_FOUND );
      try {
        Context ctx = Contexts.lookup( );
        Channels.fireExceptionCaught( ctx.getChannel( ), cause );
      } catch ( IllegalContextAccessException ex ) {
        LOG.error( ex );
        LOG.error( cause, cause );
      }
    } else {
      try {
        Context ctx = Contexts.lookup( );
        Channels.fireExceptionCaught( ctx.getChannel( ), cause );
      } catch ( IllegalContextAccessException ex ) {
        LOG.error( ex );
        LOG.error( cause, cause );
      }
    }
  }
  
  private BaseMessage convert( Object payload ) {
    BaseMessage ret = null;
    if ( payload instanceof BaseMessage ) {
      ret = ( BaseMessage ) payload;
    } else if ( payload instanceof String ) {
      try {
        ret = ( BaseMessage ) BindingManager.getBinding( "msgs_eucalyptus_com" ).fromOM( ( String ) payload );
      } catch ( Throwable ex ) {
        LOG.error( ex , ex );
      }
    } else {
      payload = RequestContext.getEvent( ).getMessage( ).getPayload( );
      if ( payload instanceof BaseMessage ) {
        ret = ( BaseMessage ) payload;
//      } else if ( payload instanceof VmAllocationInfo ) {
//        ret = ( ( VmAllocationInfo ) payload ).getRequest( );
      } else if ( payload instanceof String ) {
        try {
          ret = ( BaseMessage ) BindingManager.getBinding( "msgs_eucalyptus_com" ).fromOM( ( String ) payload );
        } catch ( Throwable ex ) {
          LOG.error( ex , ex );
        }
      }
    }
    return ret;
  }
  
}
