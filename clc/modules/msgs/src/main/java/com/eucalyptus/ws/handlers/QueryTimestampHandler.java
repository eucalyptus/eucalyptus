/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers;

import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.login.AuthenticationException;
import com.eucalyptus.auth.util.SecurityParameter;
import com.eucalyptus.auth.util.Timestamps;
import com.eucalyptus.http.MappingHttpRequest;

@ChannelPipelineCoverage( "one" )
public class QueryTimestampHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( QueryTimestampHandler.class );
  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws AuthenticationException {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      Map<String, String> parameters = httpRequest.getParameters( );
      if ( !parameters.containsKey( SecurityParameter.Timestamp.toString( ) ) && !parameters.containsKey( SecurityParameter.Expires.toString( ) ) ) {
        throw new AuthenticationException( "One of the following parameters must be specified: " + SecurityParameter.Timestamp + " OR " + SecurityParameter.Expires );
      }
      Calendar now = null;
      Calendar expires = null;
      String timestamp = null;
      String exp = null;
      try {
        now = Calendar.getInstance( );
        expires = null;
        if ( parameters.containsKey( SecurityParameter.Timestamp.toString( ) ) ) {
          timestamp = parameters.remove( SecurityParameter.Timestamp.toString( ) );
          try {
            expires = Timestamps.parseTimestamp( timestamp );
          } catch ( Exception e ) {
            expires = Timestamps.parseTimestamp( URLDecoder.decode( timestamp ) );
          }
          expires.add( Calendar.MINUTE, 15 );
        } else {
          exp = parameters.remove( SecurityParameter.Expires.toString( ) );
          try {
            expires = Timestamps.parseTimestamp( exp );
          } catch ( Exception e ) {
            expires = Timestamps.parseTimestamp( URLDecoder.decode( exp ) );
          }
        }
      } catch ( Throwable t ) {
        LOG.debug( t, t );
        throw new AuthenticationException( "Failure to parse timestamp: Timestamp=" + timestamp + " Expires=" + exp );
      }
      if ( now.after( expires ) ) {
        expires.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
        String expiryTime = String.format( "%4d-%02d-%02d'T'%02d:%02d:%02d", expires.get( Calendar.YEAR ), expires.get( Calendar.MONTH ) + 1, expires.get( Calendar.DAY_OF_MONTH ) + 1, expires.get( Calendar.HOUR_OF_DAY ), expires.get( Calendar.MINUTE ), expires.get( Calendar.SECOND ) );
        throw new AuthenticationException( "Message has expired (times in UTC): Timestamp=" + timestamp + " Expires=" + exp + " Deadline=" + expiryTime );
      }
    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {}

}
