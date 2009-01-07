/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Chris Grzegorczyk grze@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.util;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.*;
import org.mule.module.client.MuleClient;

public class Messaging {

  private static Logger LOG = Logger.getLogger( Messaging.class );

  private static MuleClient getClient() throws MuleException {
    return new MuleClient();
  }

  public static void dispatch( String dest, Object msg ) {
    MuleEvent context = RequestContext.getEvent();
    try {
      getClient().dispatch( dest, msg, null );
    }
    catch ( MuleException e ) {
      LOG.error( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

  public static Object send( String dest, Object msg ) throws EucalyptusCloudException {
    MuleEvent context = RequestContext.getEvent();
    try {
      MuleMessage reply = getClient().send( dest, msg, null );

      if ( reply.getExceptionPayload() != null )
        throw new EucalyptusCloudException( reply.getExceptionPayload().getRootException() );
      else
        return reply.getPayload();
    }
    catch ( MuleException e ) {
      LOG.error( e, e );
      throw new EucalyptusCloudException( e );
    } finally {
      RequestContext.setEvent( context );
    }
  }

}
