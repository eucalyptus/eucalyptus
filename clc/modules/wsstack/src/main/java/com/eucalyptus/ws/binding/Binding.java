/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
 ******************************************************************************/
package com.eucalyptus.ws.binding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDataSource;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.SOAPFault;
import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultDetail;
import org.apache.axiom.soap.SOAPFaultReason;
import org.apache.axiom.soap.SOAPFaultText;
import org.apache.axiom.soap.SOAPFaultValue;
import org.apache.log4j.Logger;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallable;
import org.jibx.runtime.IXMLReader;
import org.jibx.runtime.JiBXException;
import org.jibx.runtime.impl.StAXReaderWrapper;
import org.jibx.runtime.impl.UnmarshallingContext;

import com.eucalyptus.ws.BindingException;
import com.eucalyptus.ws.WebServicesException;

public class Binding {

	private static Logger   LOG = Logger.getLogger( Binding.class );
	private final String    name;
	private IBindingFactory bindingFactory;
	private String          bindingErrorMsg;
	private int[]           bindingNamespaceIndexes;
	private String[]        bindingNamespacePrefixes;

	protected Binding( final String name ) throws BindingException {
		this.name = name;
		this.buildRest( );
		/* try {
      this.bindingFactory = BindingDirectory.getFactory( name, edu.ucsb.eucalyptus.msgs.RunInstancesType.class );
    } catch ( JiBXException e ) {
      throw new BindingException( e );
    }*/
	}

	public void seed( final Class seedClass ) throws BindingException {
		try {
			if ( seedClass.getSimpleName( ).equals( "Eucalyptus" ) ) {
				bindingFactory = BindingDirectory.getFactory( this.name, edu.ucsb.eucalyptus.msgs.RunInstancesType.class );
			} else if ( seedClass.getSimpleName( ).equals( "Walrus" ) ) {
				bindingFactory = BindingDirectory.getFactory( this.name, edu.ucsb.eucalyptus.msgs.GetBucketAccessControlPolicyType.class );
			} else if ( seedClass.getSimpleName( ).equals( "StorageController" ) ) {
				bindingFactory = BindingDirectory.getFactory( this.name, edu.ucsb.eucalyptus.msgs.StorageRequestType.class );
			} else {
				final Method[] methods = seedClass.getDeclaredMethods( );
				for ( final Method m : methods ) {
					try {
						this.bindingFactory = BindingDirectory.getFactory( this.name, m.getReturnType( ) );
						break;
					} catch ( final Exception e ) {
						this.bindingErrorMsg = e.getMessage( );
						Binding.LOG.warn( "No binding for " + m.getName( ), e );
					}
				}
				if ( this.bindingFactory == null ) { throw new BindingException( "Failed to construct BindingFactory for class: " + seedClass ); }
			}
		} catch ( JiBXException e1 ) {
			throw new BindingException( e1 );
		}

		this.buildRest( );
	}

	private void buildRest( ) {
		int[] indexes = null;
		String[] prefixes = null;
		if ( bindingFactory != null ) {
			String[] nsuris = bindingFactory.getNamespaces( );
			int xsiindex = nsuris.length;
			while ( --xsiindex >= 0 && !"http://www.w3.org/2001/XMLSchema-instance".equals( nsuris[xsiindex] ) )
				;
			// get actual size of index and prefix arrays to be allocated
			int nscount = 0;
			int usecount = nscount;
			if ( xsiindex >= 0 ) usecount++;
			// allocate and initialize the arrays
			indexes = new int[usecount];
			prefixes = new String[usecount];
			if ( xsiindex >= 0 ) {
				indexes[nscount] = xsiindex;
				prefixes[nscount] = "xsi";
			}
		}
		this.bindingNamespaceIndexes = indexes;
		this.bindingNamespacePrefixes = prefixes;
	}

	public OMElement toOM( final Object param ) {
		return this.toOM( param, null );
	}

	public OMElement toOM( final Object param, final String altNs ) {
		final OMFactory factory = OMAbstractFactory.getOMFactory( );
		if ( param == null ) {
			throw new RuntimeException( "Cannot bind null value" );
		} else if ( !( param instanceof IMarshallable ) ) { throw new RuntimeException( "No JiBX <mapping> defined for class " + param.getClass( ) ); }
		if ( this.bindingFactory == null ) {
			try {
				this.bindingFactory = BindingDirectory.getFactory( this.name, param.getClass( ) );
			} catch ( final JiBXException e ) {
				Binding.LOG.error( e, e );
				throw new RuntimeException( this.bindingErrorMsg );
			}
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
				final XMLStreamWriter xmlStream = XMLOutputFactory.newInstance( ).createXMLStreamWriter( bos );
				retVal.serialize( xmlStream );
				xmlStream.flush( );
				xmlStream.close( );
				String retString = bos.toString( );
				retString = retString.replaceAll( origNs, altNs );
				final ByteArrayInputStream bis = new ByteArrayInputStream( retString.getBytes( ) );
				final StAXOMBuilder stAXOMBuilder = new StAXOMBuilder( bis );
				retVal = stAXOMBuilder.getDocumentElement( );
			} catch ( final XMLStreamException e ) {
				Binding.LOG.error( e, e );
			}
		}

		return retVal;
	}

	public UnmarshallingContext getNewUnmarshalContext( final OMElement param ) throws JiBXException {
		if ( this.bindingFactory == null ) {
			try {
				this.bindingFactory = BindingDirectory.getFactory( this.name, Class.forName( "edu.ucsb.eucalyptus.msgs." + param.getLocalName( ) + "Type" ) );
			} catch ( final Exception e ) {
				Binding.LOG.error( e, e );
				throw new RuntimeException( this.bindingErrorMsg );
			}
		}
		final UnmarshallingContext ctx = ( UnmarshallingContext ) this.bindingFactory.createUnmarshallingContext( );
		final IXMLReader reader = new StAXReaderWrapper( param.getXMLStreamReaderWithoutCaching( ), "SOAP-message", true );
		ctx.setDocument( reader );
		ctx.toTag( );
		return ctx;
	}

	public Object fromOM( final String text ) throws Exception {
		final XMLStreamReader parser = XMLInputFactory.newInstance( ).createXMLStreamReader( new ByteArrayInputStream( text.getBytes( ) ) );
		final StAXOMBuilder builder = new StAXOMBuilder( parser );
		return this.fromOM( builder.getDocumentElement( ) );
	}

	public Object fromOM( final OMElement param, final Class type ) throws WebServicesException {
		try {
			final UnmarshallingContext ctx = this.getNewUnmarshalContext( param );
			return ctx.unmarshalElement( type );
		} catch ( final Exception e ) {
			Binding.LOG.fatal( e, e );
			throw new WebServicesException( e.getMessage( ) );
		}
	}

	public Object fromOM( final OMElement param ) throws WebServicesException {
		try {
			final UnmarshallingContext ctx = this.getNewUnmarshalContext( param );
			return ctx.unmarshalElement( ); // Class.forName( "edu.ucsb.eucalyptus.msgs." + param.getLocalName( ) + "Type" ) );
		} catch ( final Exception e ) {
			Binding.LOG.fatal( e, e );
			throw new WebServicesException( e.getMessage( ) );
		}
	}

	public static SOAPEnvelope createFault(  String faultCode, String faultReason, String faultDetails )  {
		SOAPFactory soapFactory = OMAbstractFactory.getSOAP11Factory();

		SOAPFaultCode soapFaultCode = soapFactory.createSOAPFaultCode();
		soapFaultCode.setText( faultCode );

		SOAPFaultReason soapFaultReason = soapFactory.createSOAPFaultReason();
		soapFaultReason.setText( faultReason );

		SOAPFaultDetail soapFaultDetail = soapFactory.createSOAPFaultDetail();
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
