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

package edu.ucsb.eucalyptus.transport.binding;

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.jibx.JiBXDataSource;
import org.apache.log4j.Logger;
import org.jibx.runtime.*;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.UnmarshallingContext;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;

public class Binding {

  private static Logger LOG = Logger.getLogger( Binding.class );
  private String name;
  private IBindingFactory bindingFactory;
  private String bindingErrorMsg;
  private int[] bindingNamespaceIndexes;
  private String[] bindingNamespacePrefixes;

  protected Binding( String name ) throws JiBXException
  {
    this.name = name;
    this.buildRest();
  }

  protected Binding( String name, Class seedClass ) throws JiBXException
  {
    this.name = name;
    this.seed( seedClass );
  }

  public void seed( Class seedClass ) throws JiBXException
  {
    if ( seedClass.getSimpleName().equals( "Eucalyptus" ) )
    {
      bindingFactory = BindingDirectory.getFactory( this.name, edu.ucsb.eucalyptus.msgs.RunInstancesType.class );
    }
    else if ( seedClass.getSimpleName().equals( "Walrus" ) )
    {
      bindingFactory = BindingDirectory.getFactory( this.name, edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType.class );
    }
    else if ( seedClass.getSimpleName().equals("StorageController")) {
      bindingFactory = BindingDirectory.getFactory(this.name, edu.ucsb.eucalyptus.msgs.StorageRequestType.class);
    } else {
      Method[] methods = seedClass.getDeclaredMethods();
      for ( Method m : methods )
        try
        {
          bindingFactory = BindingDirectory.getFactory( this.name, m.getReturnType() );
          break;
        }
        catch ( Exception e )
        {
          this.bindingErrorMsg = e.getMessage();
          LOG.warn( "No binding for " + m.getName(), e );
        }
      if ( bindingFactory == null )
        throw new JiBXException( "Failed to construct BindingFactory for class: " + seedClass );
    }
    buildRest();
  }

  private void buildRest()
  {
    int[] indexes = null;
    String[] prefixes = null;
    if ( bindingFactory != null )
    {
      String[] nsuris = bindingFactory.getNamespaces();
      int xsiindex = nsuris.length;
      while ( --xsiindex >= 0 && !"http://www.w3.org/2001/XMLSchema-instance".equals( nsuris[ xsiindex ] ) ) ;
      // get actual size of index and prefix arrays to be allocated
      int nscount = 0;
      int usecount = nscount;
      if ( xsiindex >= 0 )
        usecount++;
      // allocate and initialize the arrays
      indexes = new int[usecount];
      prefixes = new String[usecount];
      if ( xsiindex >= 0 )
      {
        indexes[ nscount ] = xsiindex;
        prefixes[ nscount ] = "xsi";
      }
    }
    this.bindingNamespaceIndexes = indexes;
    this.bindingNamespacePrefixes = prefixes;
  }

  public OMElement mappedChild( Object value, OMFactory factory )
  {
    IMarshallable mrshable = ( IMarshallable ) value;
    OMDataSource src = new JiBXDataSource( mrshable, bindingFactory );
    int index = mrshable.JiBX_getIndex();
    OMNamespace appns = factory.createOMNamespace( bindingFactory.getElementNamespaces()[ index ], "" );
    OMElement retVal = factory.createOMElement( src, bindingFactory.getElementNames()[ index ], appns );
    return retVal;
  }

  public OMElement toOM( Object param )
  {
    return this.toOM( param, OMAbstractFactory.getOMFactory() );
  }

  public OMElement toOM( Object param, OMFactory factory )
  {
    if ( param instanceof IMarshallable )
    {
      if ( bindingFactory == null )
        try
        {
          bindingFactory = BindingDirectory.getFactory( this.name, param.getClass() );
        }
        catch ( JiBXException e )
        {
          LOG.error( e, e );
          throw new RuntimeException( this.bindingErrorMsg );
        }
      return ( mappedChild( param, factory ) );
    }
    else if ( param == null )
      throw new RuntimeException( "Cannot bind null value" );
    else
      throw new RuntimeException( "No JiBX <mapping> defined for class " + param.getClass() );
  }

  public SOAPEnvelope toEnvelope( SOAPFactory factory, Object param, boolean optimizeContent )
  {
    SOAPEnvelope envelope = factory.getDefaultEnvelope();
    if ( param != null )
      envelope.getBody().addChild( toOM( param, factory ) );
    return envelope;
  }

  public UnmarshallingContext getNewUnmarshalContext( OMElement param ) throws JiBXException
  {
    if ( bindingFactory == null )
      try
      {
        bindingFactory = BindingDirectory.getFactory( this.name, Class.forName( "edu.ucsb.eucalyptus.msgs." + param.getLocalName() + "Type" ) );
      }
      catch ( Exception e )
      {
        LOG.error( e, e );
        throw new RuntimeException( this.bindingErrorMsg );
      }
    UnmarshallingContext ctx = ( UnmarshallingContext ) this.bindingFactory.createUnmarshallingContext();
    IXMLReader reader = new StAXReaderWrapper( param.getXMLStreamReaderWithoutCaching(), "SOAP-message", true );
    ctx.setDocument( reader );
    ctx.toTag();
    return ctx;
  }

  public Object fromOM( String text ) throws Exception
  {
    XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader( new ByteArrayInputStream( text.getBytes() ) );
    StAXOMBuilder builder = new StAXOMBuilder( parser );
    return this.fromOM( builder.getDocumentElement() );
  }

  public Object fromOM( OMElement param, Class type ) throws AxisFault
  {
    try
    {
      UnmarshallingContext ctx = getNewUnmarshalContext( param );
      return ctx.unmarshalElement( type );
    }
    catch ( Exception e )
    {
      LOG.fatal( e,e );
      throw new AxisFault( e.getMessage() );
    }
  }

  public Object fromOM( OMElement param ) throws AxisFault
  {
    try
    {
      UnmarshallingContext ctx = getNewUnmarshalContext( param );
      return ctx.unmarshalElement( Class.forName( "edu.ucsb.eucalyptus.msgs." + param.getLocalName() + "Type" ) );
    }
    catch ( Exception e )
    {
      LOG.fatal( e,e );
      throw new AxisFault( e.getMessage() );
    }
  }

}
