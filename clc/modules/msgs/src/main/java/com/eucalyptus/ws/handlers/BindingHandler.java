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

package com.eucalyptus.ws.handlers;

import java.util.regex.Pattern;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.WebServicesException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

@ChannelHandler.Sharable
public class BindingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( BindingHandler.class );
  
  private Binding       binding;
  private String        namespace;
  private final Binding defaultBinding;
  private final Pattern namespacePattern;
  
  public BindingHandler( ) {
    super( );
    this.defaultBinding = null;
    this.namespacePattern = null;
  }
  
  public BindingHandler( final Binding binding ) {
    this.binding = binding;
    this.defaultBinding = binding;
    this.namespacePattern = null;
  }

  public BindingHandler( final Binding binding,
                         final Pattern namespacePattern ) {
    this.binding = binding;
    this.defaultBinding = binding;
    this.namespacePattern = namespacePattern;
  }

  @Override
  public void incomingMessage( final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      BaseMessage msg = null;
      Class msgType = null;
      String namespace = null;
      try {
        OMElement elem = httpMessage.getOmMessage( );
        OMNamespace omNs = elem.getNamespace( );
        namespace = omNs.getNamespaceURI( );
        if ( namespacePattern != null && !namespacePattern.matcher( namespace ).matches() ) {
          throw new WebServicesException( "Invalid request" );
        }
        this.binding = BindingManager.getBinding( namespace );
        msgType = this.binding.getElementClass( httpMessage.getOmMessage( ).getLocalName( ) );
      } catch ( BindingException ex ) {
        if ( this.defaultBinding != null ) {
          this.namespace = namespace;
          this.binding = this.defaultBinding;
          try {
            msgType = this.binding.getElementClass( httpMessage.getOmMessage( ).getLocalName( ) );
          } catch ( Exception ex1 ) {
            throw new WebServicesException( "Failed to find binding for namespace: " + namespace
                                            + " due to: "
                                            + ex.getMessage( ), ex );
          }
        }
      } catch ( Exception e1 ) {
        LOG.error( e1.getMessage( ) + " while attempting to bind: " + httpMessage.getOmMessage( ) );
        Logs.extreme( ).error( httpMessage.getSoapEnvelope( ).toString( ), e1 );
        if ( this.binding == null ) {
          throw new WebServicesException( e1 );
        } else {
          throw new WebServicesException( "Failed to find binding for namespace: " + namespace
                                          + " due to: "
                                          + e1.getMessage( ), e1 );
        }
      }
      try {
        if ( httpMessage instanceof MappingHttpRequest ) {
          if ( msgType != null ) {
            msg = ( BaseMessage ) this.binding.fromOM( httpMessage.getOmMessage( ), msgType );
          } else {
            msg = ( BaseMessage ) this.binding.fromOM( httpMessage.getOmMessage( ) );
          }
        } else {
          msg = ( BaseMessage ) this.binding.fromOM( httpMessage.getOmMessage( ) );
        }
      } catch ( Exception e1 ) {
        try {
          msg = ( BaseMessage ) this.binding.fromOM( httpMessage.getOmMessage( ), this.namespace );
        } catch ( Exception ex ) {
          LOG.warn( "FAILED TO PARSE:\n" + httpMessage.getMessageString( ) );
          throw new WebServicesException( e1 );
        }
      }
      msg.setCorrelationId( httpMessage.getCorrelationId( ) );
      httpMessage.setMessage( msg );
    }
  }
  
  @Override
  public void outgoingMessage( final ChannelHandlerContext ctx, final MessageEvent event ) throws Exception {
    if ( event.getMessage( ) instanceof MappingHttpMessage ) {
      MappingHttpMessage httpMessage = ( MappingHttpMessage ) event.getMessage( );
      OMElement omElem;
      if ( httpMessage.getMessage( ) instanceof EucalyptusErrorMessageType || httpMessage.getMessage( ) == null ) {
        return;
      } else if ( httpMessage.getMessage( ) instanceof ExceptionResponseType ) {
        ExceptionResponseType msg = ( ExceptionResponseType ) httpMessage.getMessage( );
        String createFaultDetails = Logs.isExtrrreeeme( )
          ? Exceptions.string( msg.getException( ) )
          : msg.getException( ).getMessage( );
        omElem = Binding.createFault( msg.getRequestType( ), msg.getMessage( ), createFaultDetails );
        if ( httpMessage instanceof MappingHttpResponse ) {
          ( ( MappingHttpResponse ) httpMessage ).setStatus( msg.getHttpStatus( ) );
        }
      } else {
        Class targetClass = httpMessage.getMessage( ).getClass( );
        while ( !targetClass.getSimpleName( ).endsWith( "Type" ) ) {
          targetClass = targetClass.getSuperclass( );
        }
        Class responseClass = ClassLoader.getSystemClassLoader( ).loadClass( targetClass.getName( ) );
        try {
          omElem = this.binding.toOM( httpMessage.getMessage( ), this.namespace );
        } catch ( BindingException ex ) {
          omElem = BindingManager.getDefaultBinding( ).toOM( httpMessage.getMessage( ) );
        } catch ( Exception ex ) {
          Logs.exhaust( ).debug( ex, ex );
          throw ex;
        }
      }
      httpMessage.setOmMessage( omElem );
    }
  }
  
}
