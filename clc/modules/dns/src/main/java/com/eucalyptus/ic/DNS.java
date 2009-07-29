/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2009, Eucalyptus Systems, Inc.
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
 * Author: Neil Soman neil@eucalyptus.com
 */

package com.eucalyptus.ic;

import com.eucalyptus.util.EucalyptusCloudException;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.ic.WalrusMessaging;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.msgs.EventRecord;
import org.apache.log4j.Logger;

public class DNS {

	private static Logger LOG = Logger.getLogger( DNS.class );

	public EucalyptusMessage handle( EucalyptusMessage msg )
	{
		LOG.warn("DNS is queuing message");
		LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_RECEIVED, msg.getClass().getSimpleName() )) ;
		long startTime = System.currentTimeMillis();
		try
		{
			WalrusMessaging.enqueue( msg );
		}
		catch ( EucalyptusCloudException e )
		{
			return new EucalyptusErrorMessageType( this.getClass().getSimpleName(), msg, e.getMessage() );
		}
		EucalyptusMessage reply = null;
		reply = WalrusMessaging.dequeue( msg.getCorrelationId() );
		LOG.info( EventRecord.create( this.getClass().getSimpleName(), msg.getUserId(), msg.getCorrelationId(), EventType.MSG_SERVICED, ( System.currentTimeMillis() - startTime ) ) );
		if ( reply == null )
			return new EucalyptusErrorMessageType( this.getClass().getSimpleName(), msg, "Received a NULL reply" );
		return reply;
	}

}
