package com.eucalyptus.binding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMException;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.soap.SOAPFactory;

public class HoldMe implements Lock {
  public static final String      OM_FACTORY_NAME_PROPERTY          = "om.factory";
  public static final String      SOAP11_FACTORY_NAME_PROPERTY      = "soap11.factory";
  public static final String      SOAP12_FACTORY_NAME_PROPERTY      = "soap12.factory";
  private static final String     DEFAULT_OM_FACTORY_CLASS_NAME     = "org.apache.axiom.om.impl.llom.factory.OMLinkedListImplFactory";
  private static final String     DEFAULT_SOAP11_FACTORY_CLASS_NAME = "org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory";
  private static final String     DEFAULT_SOAP12_FACTORY_CLASS_NAME = "org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory";
  static {
    System.setProperty( "javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory" );
    System.setProperty( "javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory" );
    System.setProperty( "javax.xml.stream.XMLEventFactory", "com.ctc.wstx.stax.WstxEventFactory" );
    System.setProperty( "org.jibx.runtime.impl.parser", "org.jibx.runtime.impl.StAXReaderFactory" );
//    System.setProperty( "org.jibx.runtime.impl.parser", "org.jibx.runtime.impl.XMLPullReaderFactory" );
  }
  private static XMLInputFactory  xmlInputFactory                   = XMLInputFactory.newInstance( );
  private static XMLOutputFactory xmlOutputFactory                  = XMLOutputFactory.newInstance( );
  
  public static Lock              canHas                            = maybeGetLock( );
  public static boolean           reuse                             = true;
  
  @Override
  public void lock( ) {}
  
  private static Lock maybeGetLock( ) {
    if ( reuse ) {
      //      return new ReentrantLock( );
      return new HoldMe( );
    } else {
      return new HoldMe( );
    }
  }
  
  @Override
  public void lockInterruptibly( ) throws InterruptedException {}
  
  @Override
  public Condition newCondition( ) {
    return null;
  }
  
  @Override
  public boolean tryLock( ) {
    return true;
  }
  
  @Override
  public boolean tryLock( long arg0, TimeUnit arg1 ) throws InterruptedException {
    return true;
  }
  
  @Override
  public void unlock( ) {}
  
  public static OMFactory getDOOMFactory( ) {
    if ( reuse )
      return DOOMAbstractFactory.getOMFactory( );
    else return new org.apache.axiom.om.impl.dom.factory.OMDOMFactory( );
  }
  
  public static SOAPFactory getDOOMSOAP11Factory( ) {
    if ( reuse )
      return DOOMAbstractFactory.getSOAP11Factory( );
    else return new org.apache.axiom.soap.impl.dom.soap11.SOAP11Factory( );
  }
  
  public static SOAPFactory getDOOMSOAP12Factory( ) {
    if ( reuse )
      return DOOMAbstractFactory.getSOAP12Factory( );
    else return new org.apache.axiom.soap.impl.dom.soap12.SOAP12Factory( );
  }
  
  public static SOAPFactory getOMSOAP11Factory( ) {
    if ( reuse ) return OMAbstractFactory.getSOAP11Factory( );
    String omFactory;
    try {
      omFactory = System.getProperty( SOAP11_FACTORY_NAME_PROPERTY );
      if ( omFactory == null || "".equals( omFactory ) ) {
        omFactory = DEFAULT_SOAP11_FACTORY_CLASS_NAME;
      }
    } catch ( SecurityException e ) {
      omFactory = DEFAULT_SOAP11_FACTORY_CLASS_NAME;
    }
    SOAPFactory defaultSOAP11OMFactory;
    try {
      defaultSOAP11OMFactory = ( SOAPFactory ) ClassLoader.getSystemClassLoader( ).loadClass( omFactory ).newInstance( );
    } catch ( InstantiationException e ) {
      throw new OMException( e );
    } catch ( IllegalAccessException e ) {
      throw new OMException( e );
    } catch ( ClassNotFoundException e ) {
      throw new OMException( e );
    }
    return defaultSOAP11OMFactory;
  }
  
  public static SOAPFactory getOMSOAP12Factory( ) {
    if ( reuse ) return OMAbstractFactory.getSOAP12Factory( );
    String omFactory;
    try {
      omFactory = System.getProperty( SOAP12_FACTORY_NAME_PROPERTY );
      if ( omFactory == null || "".equals( omFactory ) ) {
        omFactory = DEFAULT_SOAP12_FACTORY_CLASS_NAME;
      }
    } catch ( SecurityException e ) {
      omFactory = DEFAULT_SOAP12_FACTORY_CLASS_NAME;
    }
    
    SOAPFactory defaultSOAP12OMFactory;
    try {
      defaultSOAP12OMFactory = ( SOAPFactory ) ClassLoader.getSystemClassLoader( ).loadClass( omFactory ).newInstance( );
    } catch ( InstantiationException e ) {
      throw new OMException( e );
    } catch ( IllegalAccessException e ) {
      throw new OMException( e );
    } catch ( ClassNotFoundException e ) {
      throw new OMException( e );
    }
    return defaultSOAP12OMFactory;
  }
  
  public static OMFactory getOMFactory( ) {
    if ( reuse ) return OMAbstractFactory.getOMFactory( );
    String omFactory;
    try {
      omFactory = System.getProperty( OM_FACTORY_NAME_PROPERTY );
      if ( omFactory == null || "".equals( omFactory ) ) {
        omFactory = DEFAULT_OM_FACTORY_CLASS_NAME;
      }
    } catch ( SecurityException e ) {
      omFactory = DEFAULT_OM_FACTORY_CLASS_NAME;
    }
    
    OMFactory defaultOMFactory;
    try {
      defaultOMFactory = ( OMFactory ) ClassLoader.getSystemClassLoader( ).loadClass( omFactory ).newInstance( );
    } catch ( InstantiationException e ) {
      throw new OMException( e );
    } catch ( IllegalAccessException e ) {
      throw new OMException( e );
    } catch ( ClassNotFoundException e ) {
      throw new OMException( e );
    }
    return defaultOMFactory;
  }
  
  public static class ThrowAwayStAXOMBuilder extends StAXOMBuilder {
    
    public ThrowAwayStAXOMBuilder( XMLStreamReader parser ) {
      super( HoldMe.getOMFactory( ), parser );
    }
    
    public ThrowAwayStAXOMBuilder( OMFactory doomFactory, XMLStreamReader parser ) {
      super( doomFactory, parser );
    }
  }
  
  public static StAXOMBuilder getStAXOMBuilder( XMLStreamReader parser ) {
    if ( reuse )
      return new StAXOMBuilder( parser );
    else return new ThrowAwayStAXOMBuilder( parser );
  }
  
  public static StAXOMBuilder getStAXOMBuilder( OMFactory doomFactory, XMLStreamReader xmlStreamReader ) {
    if ( reuse )
      return new StAXOMBuilder( doomFactory, xmlStreamReader );
    else return new ThrowAwayStAXOMBuilder( doomFactory, xmlStreamReader );
  }
  
  public static XMLStreamReader getXMLStreamReader( InputStream in ) throws XMLStreamException {
    return HoldMe.getXMLInputFactory( ).createXMLStreamReader( in );
  }
  
  public static XMLStreamReader getXMLStreamReader( String text ) throws XMLStreamException {
    return HoldMe.getXMLInputFactory( ).createXMLStreamReader( new ByteArrayInputStream( text.getBytes( ) ) );
  }
  
  public static XMLInputFactory getXMLInputFactory( ) {
    return reuse ? xmlInputFactory : XMLInputFactory.newInstance( );
  }
  
  public static XMLOutputFactory getXMLOutputFactory( ) {
    return reuse ? xmlOutputFactory : XMLOutputFactory.newInstance( );
  }
  
}
