/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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

package com.eucalyptus.http;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpMessage;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpVersion;
import com.eucalyptus.auth.principal.User;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public abstract class MappingHttpMessage extends DefaultHttpMessage implements HttpMessage {
  private static Logger LOG = Logger.getLogger( MappingHttpMessage.class );
  private String       correlationId;
  private String       messageString;
  private SOAPEnvelope soapEnvelope;
  private OMElement    omMessage;
  protected Object       message;
  private User         user;
  private Boolean      keepAlive = Boolean.TRUE;
  private final Long         timestamp = System.currentTimeMillis( );

  protected MappingHttpMessage( HttpVersion version ) {
    super( version );
  }

  public SOAPEnvelope getSoapEnvelope( ) {
    return soapEnvelope;
  }

  public void setSoapEnvelope( SOAPEnvelope soapEnvelope ) {
    this.soapEnvelope = soapEnvelope;
  }

  public OMElement getOmMessage( ) {
    return omMessage;
  }

  public void setOmMessage( OMElement omMessage ) {
    this.omMessage = omMessage;
  }

  public Object getMessage( ) {
    return message;
  }

  public void setMessage( Object message ) {
    if( message instanceof BaseMessage && this.getCorrelationId()!=null) {
      ((BaseMessage)message).setCorrelationId( this.getCorrelationId( ) );
    }
    this.message = message;
  }

  public String getMessageString( ) {
    return messageString;
  }

  public void setMessageString( String messageString ) {
    this.messageString = messageString;
  }

  public User getUser( ) {
    return user;
  }

  public void setUser( User user ) {
    this.user = user;
  }

  public Boolean getKeepAlive( ) {
    return keepAlive;
  }

  public void setKeepAlive( Boolean keepAlive ) {
    this.keepAlive = keepAlive;
  }

  public final Long getTimestamp( ) {
    return this.timestamp;
  }

  public final String getCorrelationId( ) {
    return this.correlationId;
  }

  public void setCorrelationId( String correlationId ) {
    this.correlationId = correlationId;
  }
  
  public String logMessage( ) {
    StringBuffer buf = new StringBuffer();
    buf.append( "============================================\n" );
    buf.append( "HTTP" ).append( this.getProtocolVersion( ) ).append( '\n' );
    for( String s : this.getHeaderNames( ) ) {
      buf.append( s ).append( ": " ).append( this.getHeader( s ) ).append( '\n' );
    }
    buf.append( "============================================\n" );
    buf.append( this.getContent( ).toString( "UTF-8" ) ).append( '\n' );;
    buf.append( "============================================\n" );
    return buf.toString( );
  }

  /**
   * Get the message from within a ChannelEvent. Returns null if no message found.
   * 
   * @param <T>
   * @param e
   * @return message or null if no msg.
   */
  public static <T extends MappingHttpMessage> T extractMessage( ChannelEvent e ) {
    if ( e instanceof MessageEvent ) {
      final MessageEvent msge = ( MessageEvent ) e;
      if ( msge.getMessage( ) instanceof MappingHttpRequest ) {
        return ( T ) msge.getMessage( );
      } else if ( msge.getMessage( ) instanceof MappingHttpResponse ) {
          return ( T ) msge.getMessage( );
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
