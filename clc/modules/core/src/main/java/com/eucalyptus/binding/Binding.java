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

package com.eucalyptus.binding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.StAXWriter;
import org.jibx.runtime.impl.UnmarshallingContext;
import com.eucalyptus.util.NamespaceMappingXMLStreamWriter;
import com.eucalyptus.util.OMXMLStreamWriter;
import com.eucalyptus.util.ThrowingFunction;
import com.eucalyptus.ws.WebServicesException;
import com.google.common.collect.Maps;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Binding {
  
  private static Logger       LOG                 = Logger.getLogger( Binding.class );
  private final String        name;
  private IBindingFactory     bindingFactory;
  private Map<String, Class>  elementToClassMap   = Maps.newHashMap( );
  private Map<String, String> classToElementMap   = Maps.newHashMap( );
  private Map<String, String> classToNamespaceMap = Maps.newHashMap( );
  
  protected Binding( final String name ) {
    this.name = name;
  }
  
  public boolean hasElementClass( String elementName ) throws BindingException {
    return this.elementToClassMap.containsKey( elementName );
    
  }
  
  public Class getElementClass( String elementName ) throws BindingException {
    if ( !this.elementToClassMap.containsKey( elementName ) ) {
      throw new BindingElementNotFoundException( elementName, "Failed to find corresponding class mapping for element: " + elementName + " in namespace: " + this.name );
    }
    return this.elementToClassMap.get( elementName );
  }
  
  public IBindingFactory seed( final Class seed ) throws BindingException {
    final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader( );
    try {
      this.bindingFactory = BindingDirectory.getFactory( this.name, seed, systemClassLoader );
      final String[] mappedClasses = this.bindingFactory.getMappedClasses( );
      for ( int i = 0; i < mappedClasses.length; i++ ) {
        if ( this.bindingFactory.getElementNames( )[i] != null ) {
          try {
            this.elementToClassMap.put( this.bindingFactory.getElementNames( )[i], systemClassLoader.loadClass( mappedClasses[i] ) );
            this.classToElementMap.put( mappedClasses[i], this.bindingFactory.getElementNames( )[i] );
            this.classToNamespaceMap.put( mappedClasses[i], this.bindingFactory.getElementNamespaces( )[i] );
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
  
  public String toStream( final OutputStream outputStream, final Object param ) throws BindingException {
    return toStream( outputStream, param, null );
  }

  public String toStream( final OutputStream outputStream, final Object param, final String altNs ) throws BindingException {
    return toWriter(
        (__) -> HoldMe.getXMLOutputFactory( ).createXMLStreamWriter( outputStream, null ),
        param,
        altNs );
  }

  public OMElement toOM( final Object param ) throws BindingException {
    return this.toOM( param, null );
  }

  public OMElement toOM( final Object param, final String altNs ) throws BindingException {
    final OMDocument[] document = new OMDocument[1];
    toWriter( holdMe -> {
      final OMFactory factory = HoldMe.getOMFactory( );
      document[0] = factory.createOMDocument( );
      return new OMXMLStreamWriter( factory, document[0] );
    }, param, altNs );
    return document[0].getOMDocumentElement( );
  }

  private Object bindingReplace( final Object param) {
    if ( param instanceof BindingReplace ) {
      return ((BindingReplace<?>)param).bindingReplace( );
    } else {
      return param;
    }
  }
  
  private String toWriter( final ThrowingFunction<Void,XMLStreamWriter,XMLStreamException> writerFunction, final Object paramObj, final String altNs ) throws BindingException {
    if ( paramObj == null ) {
      throw new BindingException( "Cannot bind null value" );
    }
    final Object param = bindingReplace( paramObj );
    if ( !( param instanceof IMarshallable ) ) {
      throw new BindingException( "No JiBX <mapping> defined for class " + param.getClass( ) );
    }
    final IMarshallable mrshable = ( IMarshallable ) param;
    final String fqName = mrshable.JiBX_getName( );
    final String origNs = this.classToNamespaceMap.get(fqName);
    final String useNs = altNs != null
        ? altNs
        : origNs;
    if ( this.bindingFactory == null ) {
      throw new BindingException( "Failed to prepare binding factory (unset) for message: " + param.getClass( ).getCanonicalName( ) + " with namespace: " + useNs );
    }
    if ( this.bindingFactory.getElementNamespaces( ) == null ) {
      LOG.error( "Binding factory's element namespace is empty" );
      throw new BindingException( "Failed to prepare binding factory for message: " + param.getClass( ).getCanonicalName( ) + " with namespace: " + useNs );
    }
    HoldMe.canHas.lock( );
    try {
      final IMarshallingContext mctx = this.bindingFactory.createMarshallingContext( );
      final XMLStreamWriter wrtr = new NamespaceMappingXMLStreamWriter(
          writerFunction.apply( null ),
          Collections.singletonMap( origNs, useNs ) );
      final StAXWriter staxWriter = new StAXWriter( this.bindingFactory.getNamespaces( ), wrtr );
      mctx.setXmlWriter( staxWriter );
      mctx.marshalDocument( param );
      mctx.getXmlWriter( ).flush( );
    } catch ( XMLStreamException | JiBXException | IOException e ) {
      throw new BindingException( this.name + " failed to marshall type " + param.getClass( ).getCanonicalName( ) + " with ns:" + useNs + " caused by: "
          + e.getMessage( ), e );
    } finally {
      HoldMe.canHas.unlock( );
    }
    return useNs;
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
  
  public static <T> List<T> listFactory( ) {
    return ( List<T> ) new ArrayList( );
  }
  
  public Object fromOM( final OMElement param, final Class type ) throws WebServicesException {
    try {
      final UnmarshallingContext ctx = this.getNewUnmarshalContext( param );
      return ctx.unmarshalElement( type );
    } catch ( final Exception e ) {
      LOG.warn( e, e );
      throw new WebServicesException( e.getMessage( ) );
    }
  }
  
  public Object fromOM( final OMElement param, final String namespace ) throws WebServicesException {
    StringWriter out = new StringWriter( );
    try {
      param.serialize( out );
      return this.fromOM( out.toString( ).replace( param.getNamespace( ).getNamespaceURI( ), namespace ) );
    } catch ( Exception e ) {
      LOG.warn( e, e );
      throw new WebServicesException( e.getMessage( ) );
    }
  }
  
  public Object fromOM( final OMElement param ) throws WebServicesException {
    try {
      final UnmarshallingContext ctx = this.getNewUnmarshalContext( param );
      return ctx.unmarshalElement( );
    } catch ( final Exception e ) {
      LOG.warn( e, e );
      throw new WebServicesException( e.getMessage( ) );
    }
  }

  @SuppressWarnings( "unchecked" )
  public <T> T fromStream( final Class<T> type, final InputStream in ) throws Exception {
    HoldMe.canHas.lock( );
    try {
      if ( this.bindingFactory == null ) {
        throw new RuntimeException( "Binding bootstrap failed to construct the binding factory for " + this.name );
      }
      final UnmarshallingContext ctx = ( UnmarshallingContext ) this.bindingFactory.createUnmarshallingContext( );
      final IXMLReader reader = new StAXReaderWrapper( HoldMe.getXMLStreamReader( in ), type.getSimpleName(), true );
      ctx.setDocument( reader );
      return (T) ctx.unmarshalElement( type );
    } finally {
      HoldMe.canHas.unlock( );
    }
  }

  public static String createRestFault( String faultCode, String faultReason, String faultDetails ) {
    faultCode = ( faultCode != null
      ? faultCode
      : HttpResponseStatus.INTERNAL_SERVER_ERROR.toString( ) );
    faultReason = ( faultReason != null
      ? faultReason
      : "unknown" );
    faultDetails = ( faultDetails != null
      ? faultDetails
      : "unknown" );
    return new StringBuilder( ).append( "<?xml version=\"1.0\"?><Response><Errors><Error><Code>" )
                              .append( faultCode.replaceAll( "<", "&lt;" ).replaceAll( ">", "&gt;" ) )
                              .append( "</Code><Message>" ).append( faultReason.replaceAll( "<", "&lt;" ).replaceAll( ">", "&gt;" ) )
                              .append( "</Message></Error></Errors><RequestID>" ).append( faultDetails.replaceAll( "<", "&lt;" ).replaceAll( ">", "&gt;" ) )
                              .append( "</RequestID></Response>" ).toString( );
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
  
}
