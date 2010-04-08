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
package com.eucalyptus.binding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMOutputFormat;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IMarshaller;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.IXMLWriter;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.StAXWriter;
import org.jibx.runtime.impl.UnmarshallingContext;

import com.eucalyptus.ws.WebServicesException;
import com.google.common.collect.Maps;

public class Binding {

  private static Logger   LOG = Logger.getLogger( Binding.class );
  private final String    name;
  private IBindingFactory bindingFactory;
  private Map<String,Class> elementToClassMap = Maps.newHashMap( );

  protected Binding( final String name ) throws BindingException {
    this.name = name;
  }
  
  public IBindingFactory seed( final Class seed ) throws BindingException {
    try {
      this.bindingFactory = BindingDirectory.getFactory( this.name, seed );
      String[] mappedClasses = bindingFactory.getMappedClasses( );
      for( int i = 0; i < mappedClasses.length; i ++ ) {
        if( bindingFactory.getElementNames( )[i] != null ) {
          try {
            elementToClassMap.put( bindingFactory.getElementNames( )[i], Class.forName( mappedClasses[i] ) );
            LOG.trace( "Caching binding for " + this.name + " on element " + bindingFactory.getElementNames( )[i] + " to class " + mappedClasses[i] );
          } catch ( ClassNotFoundException e ) {
            LOG.trace( e, e );
          }
        }
      }
    } catch ( JiBXException e ) {
      LOG.debug( e, e );
      throw new BindingException( "Failed to build binding factory for " + this.name + " with seed class " + seed.getCanonicalName( ) );
    }
    return this.bindingFactory;
  }
  
  private IBindingFactory getBindingFactory( Class c ) throws BindingException {
    if( this.bindingFactory == null ) {
      return this.seed( c );
    } else {
      return this.bindingFactory;
    }
  }

  public OMElement toOM( final Object param ) throws BindingException {
    return this.toOM( param, null );
  }

  public OMElement toOM( final Object param, final String altNs ) throws BindingException {
    final OMFactory factory = HoldMe.getOMFactory( );
    if ( param == null ) {
      throw new BindingException( "Cannot bind null value" );
    } else if ( !( param instanceof IMarshallable ) ) {
      throw new BindingException( "No JiBX <mapping> defined for class " + param.getClass( ) );
    }

    final IMarshallable mrshable = ( IMarshallable ) param;
    final OMDataSource src = new JiBXDataSource( mrshable, this.bindingFactory );
    final int index = mrshable.JiBX_getIndex( );
    final OMNamespace appns = factory.createOMNamespace( this.bindingFactory.getElementNamespaces( )[index], "" );
    OMElement retVal = factory.createOMElement( src, this.bindingFactory.getElementNames( )[index], appns );
    final String origNs = retVal.getNamespace( ).getNamespaceURI( );
    if ( ( altNs != null ) && !altNs.equals( origNs ) ) {
      try {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream( );
        retVal.serialize( bos );
        String retString = bos.toString( );
        retString = retString.replaceAll( origNs, altNs );
        HoldMe.canHas.lock( );
        try {
          final StAXOMBuilder stAXOMBuilder = HoldMe.getStAXOMBuilder( HoldMe.getXMLStreamReader( retString ) );
          retVal = stAXOMBuilder.getDocumentElement( );
        } finally {
          HoldMe.canHas.unlock( );
        }
      } catch ( final XMLStreamException e ) {
        LOG.error( e, e );
      }
    }

    return retVal;
  }

  public UnmarshallingContext getNewUnmarshalContext( final OMElement param ) throws JiBXException {
    if ( this.bindingFactory == null ) {
      throw new RuntimeException( "Binding bootstrap failed to construct the binding factory for " + this.name );
    }
    final UnmarshallingContext ctx = ( UnmarshallingContext ) this.bindingFactory.createUnmarshallingContext( );
    final IXMLReader reader = new StAXReaderWrapper( param.getXMLStreamReader( ), "SOAP-message", true );
    ctx.setDocument( reader );
    ctx.toTag( );
    return ctx;
  }

  public Object fromOM( final String text ) throws Exception {
    HoldMe.canHas.lock( );
    try {
      final StAXOMBuilder builder = HoldMe.getStAXOMBuilder( HoldMe.getXMLStreamReader( text ) );
      return this.fromOM( builder.getDocumentElement( ) );
    } finally {
      HoldMe.canHas.unlock( );
    }
  }

  public Object fromOM( final OMElement param, final Class type ) throws WebServicesException {
    try {
      final UnmarshallingContext ctx = this.getNewUnmarshalContext( param );
      return ctx.unmarshalElement( type );
    } catch ( final Exception e ) {
      LOG.fatal( e, e );
      throw new WebServicesException( e.getMessage( ) );
    }
  }

  public Object fromOM( final OMElement param ) throws WebServicesException {
    try {
      final UnmarshallingContext ctx = this.getNewUnmarshalContext( param );
      return ctx.unmarshalElement( );
    } catch ( final Exception e ) {
      LOG.fatal( e, e );
      throw new WebServicesException( e.getMessage( ) );
    }
  }
  public static String createRestFault( String faultCode, String faultReason, String faultDetails ) {
    return new StringBuffer().append("<?xml version=\"1.0\"?><Response><Errors><Error><Code>")
      .append(faultCode.replaceAll("<","&lt;").replaceAll(">","&gt;")).append("</Code><Message>")
      .append(faultReason.replaceAll("<","&lt;").replaceAll(">","&gt;")).append("</Message></Error></Errors><RequestID>")
      .append(faultDetails.replaceAll("<","&lt;").replaceAll(">","&gt;")).append("</RequestID></Response>").toString( );
  }
  public static SOAPEnvelope createFault( String faultCode, String faultReason, String faultDetails ) {
    SOAPFactory soapFactory = HoldMe.getOMSOAP11Factory( );

    SOAPFaultCode soapFaultCode = soapFactory.createSOAPFaultCode( );
    soapFaultCode.setText( faultCode );

    SOAPFaultReason soapFaultReason = soapFactory.createSOAPFaultReason( );
    soapFaultReason.setText( faultReason );

    SOAPFaultDetail soapFaultDetail = soapFactory.createSOAPFaultDetail( );
    soapFaultDetail.setText( faultDetails );

    SOAPEnvelope soapEnv = soapFactory.getDefaultEnvelope( );
    SOAPFault soapFault = soapFactory.createSOAPFault( );
    soapFault.setCode( soapFaultCode );
    soapFault.setDetail( soapFaultDetail );
    soapFault.setReason( soapFaultReason );
    soapEnv.getBody( ).addFault( soapFault );
    return soapEnv;
  }

  private static class JiBXDataSource implements OMDataSource {
    private final int             marshallerIndex;
    private final String          elementName;
    private final String          elementNamespace;
    private final String          elementNamespacePrefix;
    private final int             elementNamespaceIndex;
    private final int[]           openNamespaceIndexes;
    private final String[]        openNamespacePrefixes;
    private final Object          dataObject;
    private final IBindingFactory bindingFactory;
    private static Logger         LOG = Logger.getLogger( JiBXDataSource.class );

    public JiBXDataSource( final IMarshallable obj, final IBindingFactory factory ) {
      this.marshallerIndex = -1;
      this.dataObject = obj;
      this.bindingFactory = factory;
      this.elementName = this.elementNamespace = this.elementNamespacePrefix = null;
      this.elementNamespaceIndex = -1;
      this.openNamespaceIndexes = null;
      this.openNamespacePrefixes = null;
    }

    private void marshal( final boolean full, final IMarshallingContext ctx ) throws JiBXException {
      try {
        if ( this.marshallerIndex < 0 ) {
          if ( this.dataObject instanceof IMarshallable ) {
            ( ( IMarshallable ) this.dataObject ).marshal( ctx );
          } else {
            throw new IllegalStateException( "Object of class " + this.dataObject.getClass( ).getName( ) + " needs a JiBX <mapping> to be marshalled" );
          }
        } else {
          final IXMLWriter wrtr = ctx.getXmlWriter( );
          String name = this.elementName;
          int nsidx = 0;
          if ( full ) {
            nsidx = this.elementNamespaceIndex;
            wrtr.startTagNamespaces( nsidx, name, this.openNamespaceIndexes, this.openNamespacePrefixes );
          } else {
            wrtr.openNamespaces( this.openNamespaceIndexes, this.openNamespacePrefixes );
            if ( !"".equals( this.elementNamespacePrefix ) ) {
              name = this.elementNamespacePrefix + ':' + name;
            }
            wrtr.startTagOpen( 0, name );
          }
          final IMarshaller mrsh = ctx.getMarshaller( this.marshallerIndex, this.bindingFactory.getMappedClasses( )[this.marshallerIndex] );
          mrsh.marshal( this.dataObject, ctx );
          wrtr.endTag( nsidx, name );
        }
        ctx.getXmlWriter( ).flush( );

      } catch ( final IOException e ) {
        throw new JiBXException( "Error marshalling XML representation: " + e.getMessage( ), e );
      }
    }

    public void serialize( final OutputStream output, final OMOutputFormat format ) throws XMLStreamException {
      try {
        final IMarshallingContext ctx = this.bindingFactory.createMarshallingContext( );
        ctx.setOutput( output, format == null ? null : format.getCharSetEncoding( ) );
        this.marshal( true, ctx );
      } catch ( final JiBXException e ) {
        throw new XMLStreamException( "Error in JiBX marshalling: " + e.getMessage( ), e );
      }
    }

    public void serialize( final Writer writer, final OMOutputFormat format ) throws XMLStreamException {
      try {
        final IMarshallingContext ctx = this.bindingFactory.createMarshallingContext( );
        ctx.setOutput( writer );
        this.marshal( true, ctx );
      } catch ( final JiBXException e ) {
        throw new XMLStreamException( "Error in JiBX marshalling: " + e.getMessage( ), e );
      }
    }

    public void serialize( final XMLStreamWriter xmlWriter ) throws XMLStreamException {
      try {
        boolean full = true;
        final String[] nss = this.bindingFactory.getNamespaces( );
        if ( this.marshallerIndex >= 0 ) {
          String prefix = xmlWriter.getPrefix( this.elementNamespace );
          if ( this.elementNamespacePrefix.equals( prefix ) ) {
            full = false;
            for ( int i = 0; i < this.openNamespaceIndexes.length; i++ ) {
              final String uri = nss[i];
              prefix = xmlWriter.getPrefix( uri );
              if ( !this.openNamespacePrefixes[i].equals( prefix ) ) {
                full = true;
                break;
              }
            }
          }
        }
        final IXMLWriter writer = new StAXWriter( nss, xmlWriter );
        final IMarshallingContext ctx = this.bindingFactory.createMarshallingContext( );
        ctx.setXmlWriter( writer );
        this.marshal( full, ctx );
      } catch ( final JiBXException e ) {
        throw new XMLStreamException( "Error in JiBX marshalling: " + e.getMessage( ), e );
      }
    }

    public XMLStreamReader getReader( ) throws XMLStreamException {
      final ByteArrayOutputStream bos = new ByteArrayOutputStream( );
      this.serialize( bos, null );
      return HoldMe.getXMLStreamReader( new ByteArrayInputStream( bos.toByteArray( ) ) );
    }
  }

}
