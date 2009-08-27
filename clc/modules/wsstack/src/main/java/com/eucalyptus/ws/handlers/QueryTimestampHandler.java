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
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.ws.handlers;

import java.util.Calendar;
import java.util.Map;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.AuthenticationException;
import com.eucalyptus.ws.MappingHttpRequest;
import com.eucalyptus.ws.handlers.HmacV2Handler.SecurityParameter;
import com.eucalyptus.ws.util.HmacUtils;

@ChannelPipelineCoverage("one")
public class QueryTimestampHandler extends MessageStackHandler {

  @Override
  public void incomingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpRequest ) {
      MappingHttpRequest httpRequest = ( MappingHttpRequest ) event.getMessage( );
      Map<String, String> parameters = httpRequest.getParameters( );
      if ( !parameters.containsKey( SecurityParameter.Timestamp.toString( ) ) && !parameters.containsKey( SecurityParameter.Expires.toString( ) ) ) { throw new AuthenticationException( "One of the following parameters must be specified: " + SecurityParameter.Timestamp + " OR "
          + SecurityParameter.Expires ); }
      // :: check the timestamp :://
      Calendar now = Calendar.getInstance( );
      Calendar expires = null;
      if ( parameters.containsKey( SecurityParameter.Timestamp.toString( ) ) ) {
        String timestamp = parameters.remove( SecurityParameter.Timestamp.toString( ) );
        expires = HmacUtils.parseTimestamp( timestamp );
        expires.add( Calendar.MINUTE, 5 );
      } else {
        String exp = parameters.remove( SecurityParameter.Expires.toString( ) );
        expires = HmacUtils.parseTimestamp( exp );
      }
      if ( now.after( expires ) ) throw new AuthenticationException( "Message has expired." );

    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {
  }

}
