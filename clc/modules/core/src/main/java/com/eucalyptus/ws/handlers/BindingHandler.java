/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2016 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.ws.handlers;

import java.util.regex.Pattern;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;

import com.eucalyptus.binding.Binding;
import com.eucalyptus.binding.BindingException;
import com.eucalyptus.binding.BindingManager;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.http.MappingHttpMessage;
import com.eucalyptus.http.MappingHttpRequest;
import com.eucalyptus.http.MappingHttpResponse;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.ws.WebServicesException;

import edu.ucsb.eucalyptus.msgs.BaseMessage;
import edu.ucsb.eucalyptus.msgs.BaseMessages;
import edu.ucsb.eucalyptus.msgs.EucalyptusErrorMessageType;
import edu.ucsb.eucalyptus.msgs.ExceptionResponseType;

public class BindingHandler extends MessageStackHandler {
  private static Logger LOG = Logger.getLogger( BindingHandler.class );
  
  private final BindingHandlerContext context;

  public static abstract class BindingHandlerContext {
    public boolean namespaceMismatch( final String namespace ) {
      return false;
    }

    public void updateBindingForNamespace( final String namespace ) {
    }

    public boolean updateForDefaultBinding( final String namespace ) {
      return false;
    }

    public String getNamespace( ) {
      return null;
    }

    public abstract Binding getBinding( );
  }

  public static class StaticBindingHandlerContext extends BindingHandlerContext {
    private final Binding binding;

    public StaticBindingHandlerContext( final Binding binding ) {
      this.binding = binding;
    }

    @Override
    public Binding getBinding( ) {
      return binding;
    }
  }

  public static class DynamicBindingHandlerContext extends BindingHandlerContext {
    private Binding       binding;
    private String        namespace;
    private final Binding defaultBinding;
    private final Pattern namespacePattern;
    private final Class<? extends ComponentId> component;

    public DynamicBindingHandlerContext( final Binding binding,
                                         final Pattern namespacePattern,
                                         final Class<? extends ComponentId> component ) {
      this.binding = binding;
      this.defaultBinding = binding;
      this.namespacePattern = namespacePattern;
      this.component = component;
    }

    @Override
    public boolean namespaceMismatch( final String namespace ) {
      return namespacePattern != null && !namespacePattern.matcher( namespace ).matches( );
    }

    @Override
    public void updateBindingForNamespace( final String namespace ) {
      this.binding = BindingManager.getBinding( namespace, component );
    }

    @Override
    public boolean updateForDefaultBinding( final String namespace ) {
      if ( this.defaultBinding != null ) {
        this.namespace = namespace;
        this.binding = this.defaultBinding;
        return true;
      }
      return false;
    }

    @Override
    public String getNamespace( ) {
      return namespace;
    }

    @Override
    public Binding getBinding( ) {
      return binding;
    }
  }

  /**
   * Create a static context which will always use the given binding.
   *
   * <p>A BindingHandler created with such a context can safely be cached</p>
   */
  public static BindingHandlerContext context( final Binding binding ) {
    return new StaticBindingHandlerContext( binding );

  }

  /**
   * Create a dynamic context which will always use the given binding.
   *
   * <p>A BindingHandler created with such a context should only be used once
   * and should not be cached.</p>
   */
  public static BindingHandlerContext context( final Binding binding,
                                               final Pattern namespacePattern,
                                               final Class<? extends ComponentId> component ) {
    return new DynamicBindingHandlerContext( binding, namespacePattern, component );

  }

  public BindingHandler( final BindingHandlerContext context ) {
    this.context = context;
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
        if ( context.namespaceMismatch( namespace ) ) {
          throw new WebServicesException( "Invalid request" );
        }
        context.updateBindingForNamespace( namespace );
        msgType = this.context.getBinding( ).getElementClass( httpMessage.getOmMessage( ).getLocalName( ) );
      } catch ( BindingException ex ) {
        if ( this.context.updateForDefaultBinding( namespace ) ) {
          try {
            msgType = this.context.getBinding( ).getElementClass( httpMessage.getOmMessage( ).getLocalName( ) );
          } catch ( Exception ex1 ) {
            throw new WebServicesException( "Failed to find binding for namespace: " + namespace
                                            + " due to: "
                                            + ex.getMessage( ), ex );
          }
        }
      } catch ( Exception e1 ) {
        LOG.error( e1.getMessage( ) + " while attempting to bind: " + httpMessage.getOmMessage( ) );
        Logs.extreme( ).error( httpMessage.getSoapEnvelope( ).toString( ), e1 );
        if ( this.context.getBinding( ) == null ) {
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
            msg = ( BaseMessage ) this.context.getBinding( ).fromOM( httpMessage.getOmMessage( ), msgType );
          } else {
            msg = ( BaseMessage ) this.context.getBinding( ).fromOM( httpMessage.getOmMessage( ) );
          }
        } else {
          msg = ( BaseMessage ) this.context.getBinding( ).fromOM( httpMessage.getOmMessage( ) );
        }
      } catch ( Exception e1 ) {
        try {
          msg = ( BaseMessage ) this.context.getBinding( ).fromOM( httpMessage.getOmMessage( ), this.context.getNamespace( ) );
        } catch ( Exception ex ) {
          LOG.warn( "FAILED TO PARSE:\n" + httpMessage.getMessageString( ) );
          throw new WebServicesException( e1 );
        }
      }
      
      // in case the base message has request ID in its correlation ID prefix, 
      // we should reset the correlation ID using the request ID
      if ( httpMessage.getCorrelationId() != null && 
          msg.getCorrelationId()!=null && 
          msg.hasRequestId()) {
        try{
          final Context ctx = Contexts.lookup(httpMessage.getCorrelationId());
          // reset correlation ID
          msg.regardingRequestId(msg.getCorrelationId());
          httpMessage.setCorrelationId(msg.getCorrelationId());
          Contexts.update(ctx,  httpMessage.getCorrelationId());
        }catch(final Exception ex){
          ;
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
        try {
          omElem = this.context.getBinding( ).toOM( httpMessage.getMessage( ), this.context.getNamespace( ) );
        } catch ( BindingException ex ) {
          omElem = BaseMessages.toOm( (BaseMessage) httpMessage.getMessage( ) );
        } catch ( Exception ex ) {
          Logs.exhaust( ).debug( ex, ex );
          throw ex;
        }
      }
      httpMessage.setOmMessage( omElem );
    }
  }
  
}
