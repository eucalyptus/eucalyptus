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

package com.eucalyptus.ws.handlers.wssecurity;

import java.security.cert.X509Certificate;
import java.util.Collection;
import javax.naming.AuthenticationException;
import javax.xml.ws.WebServiceException;
import org.apache.axiom.soap.SOAPEnvelope;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.Partition;
import com.eucalyptus.component.Partitions;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.crypto.util.WSSecurity;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.ws.handlers.MessageStackHandler;
import com.eucalyptus.ws.handlers.WsSecHandler;
import com.eucalyptus.ws.util.CredentialProxy;
import com.google.common.collect.Lists;


public class BrokerWsSecHandler extends MessageStackHandler implements ChannelHandler {

  @Override
  public void incomingMessage( MessageEvent event ) throws Exception {
    final Object o = event.getMessage( );
    if ( o instanceof MappingHttpMessage ) {
    	final MappingHttpMessage httpRequest = ( MappingHttpMessage ) o;
        final SOAPEnvelope envelope = httpRequest.getSoapEnvelope( );
       
        X509Certificate cert = WSSecurity.verifyWSSec(envelope);
        boolean found = false;
        // accept any CC's cert for now, but ideally VB would
        // want to only accept requests from the paired CC
        for (Partition part : Partitions.list()) {
        	if (cert.equals(part.getCertificate())) {
        		found = true;
        		break;
        	}
        }
        if( !found ) {
        	throw new WebServiceException("Authentication failure: cert is not trusted");
        }
        User admin = Accounts.lookupSystemAdmin( ); 
        Contexts.lookup( ( ( MappingHttpMessage ) o ).getCorrelationId( ) ).setUser( admin );
    }
  }

  @Override
  public void outgoingMessage( ChannelHandlerContext ctx, MessageEvent event ) throws Exception {}

}
 
