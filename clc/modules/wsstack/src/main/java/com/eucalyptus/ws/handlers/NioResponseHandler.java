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
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.ws.handlers;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.ws.MappingHttpMessage;
import com.eucalyptus.ws.WebServicesException;

import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

@ChannelPipelineCoverage( "one" )
public class NioResponseHandler extends MessageStackHandler {
  private static Logger     LOG      = Logger.getLogger( NioResponseHandler.class );

  private final Lock        canHas   = new ReentrantLock( );
  private final Condition   finished = canHas.newCondition( );
  private EucalyptusMessage response = null;
  private Exception         ex       = null;

  @Override
  public void exceptionCaught( Throwable e ) throws Exception {
    this.canHas.lock( );
    if ( e instanceof Exception ) {
      this.ex = ( Exception ) e;
    } else {
      this.ex = new WebServicesException( e );
    }
    this.finished.signal( );
    this.canHas.unlock( );
  }

  @Override
  public void exceptionCaught( ChannelHandlerContext ctx, ExceptionEvent e ) throws Exception {
    this.exceptionCaught( e.getCause( ) );
  }

  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
  }

  @Override
  public void incomingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    MappingHttpMessage httpResponse = ( MappingHttpMessage ) event.getMessage( );
    this.canHas.lock( );
    this.response = (EucalyptusMessage) httpResponse.getMessage( );
    this.finished.signal( );
    this.canHas.unlock( );
  }

  public EucalyptusMessage getResponse( ) throws Exception {
    this.canHas.lock( );
    try {
      if ( this.response == null && this.ex == null ) {
        this.finished.await( );
      }
      if ( ex != null ) { throw this.ex; }
      return this.response;
    } finally {
      this.canHas.unlock( );
    }
  }

}
