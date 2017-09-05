/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
 ************************************************************************/
package com.eucalyptus.util;

import java.util.ArrayDeque;
import java.util.Deque;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;

/**
 *
 */
public class OMXMLStreamWriter implements XMLStreamWriter {

  private final OMFactory factory;
  private final Deque<OMContainer> stack = new ArrayDeque<>();

  public OMXMLStreamWriter( final OMFactory factory, final OMContainer container ) {
    this.factory = factory;
    this.stack.push( container );
  }

  private void attr( final OMAttribute attr ) throws XMLStreamException {
    OMContainer parent = stack.peek( );
    if ( !(parent instanceof OMElement) ) {
      throw new XMLStreamException( "Cannot add attribute to " + parent );
    }
    ((OMElement)parent).addAttribute( attr );
  }

  private void ns( final OMNamespace ns ) throws XMLStreamException {
    OMContainer parent = stack.peek( );
    if ( !(parent instanceof OMElement) ) {
      throw new XMLStreamException( "Cannot add namespce to " + parent );
    }
    ((OMElement)parent).declareNamespace( ns );
  }

  private void text( final OMText text ) throws XMLStreamException {
    node( text );
  }

  private void node( final OMNode node ) {
    OMContainer parent = stack.peek( );
    if ( parent != null ) {
      parent.addChild( node );
    }
    if ( node instanceof OMContainer ) {
      stack.push( (OMContainer) node );
    }
  }

  @Override
  public void writeStartElement( final String localName ) throws XMLStreamException {
    throw new XMLStreamException( "Namespace required" );
  }

  @Override
  public void writeStartElement( final String namespaceURI, final String localName ) throws XMLStreamException {
    node( factory.createOMElement( localName, namespaceURI, "" ) );
  }

  @Override
  public void writeStartElement( final String prefix, final String localName, final String namespaceURI ) throws XMLStreamException {
    node( factory.createOMElement( localName, namespaceURI, prefix ) );
  }

  @Override
  public void writeEmptyElement( final String namespaceURI, final String localName ) throws XMLStreamException {
    writeStartElement( namespaceURI, localName );
    writeEndElement( );
  }

  @Override
  public void writeEmptyElement( final String prefix, final String localName, final String namespaceURI ) throws XMLStreamException {
    writeStartElement( prefix, namespaceURI, localName );
    writeEndElement( );
  }

  @Override
  public void writeEmptyElement( final String localName ) throws XMLStreamException {
    throw new XMLStreamException( "Namespace required" );
  }

  @Override
  public void writeEndElement() throws XMLStreamException {
    stack.pop( );
  }

  @Override
  public void writeEndDocument() throws XMLStreamException {
  }

  @Override
  public void close() throws XMLStreamException {
  }

  @Override
  public void flush() throws XMLStreamException {
  }

  @Override
  public void writeAttribute( final String localName, final String value ) throws XMLStreamException {
    attr( factory.createOMAttribute( localName, null, value ) );
  }

  @Override
  public void writeAttribute( final String prefix, final String namespaceURI, final String localName, final String value ) throws XMLStreamException {
    attr( factory.createOMAttribute( localName, factory.createOMNamespace( namespaceURI, prefix ), value ) );
  }

  @Override
  public void writeAttribute( final String namespaceURI, final String localName, final String value ) throws XMLStreamException {
    attr( factory.createOMAttribute( localName, factory.createOMNamespace( namespaceURI, "" ), value ) );
  }

  @Override
  public void writeNamespace( final String prefix, final String namespaceURI ) throws XMLStreamException {
    ns( factory.createOMNamespace( namespaceURI, prefix ) );
  }

  @Override
  public void writeDefaultNamespace( final String namespaceURI ) throws XMLStreamException {
    ns( factory.createOMNamespace( namespaceURI, "" ) );
  }

  @Override
  public void writeComment( final String data ) throws XMLStreamException {
    text( factory.createOMText( data, XMLStreamConstants.COMMENT ) );
  }

  @Override
  public void writeProcessingInstruction( final String target ) throws XMLStreamException {
    throw new XMLStreamException( "Processing instructions not supported" );
  }

  @Override
  public void writeProcessingInstruction( final String target, final String data ) throws XMLStreamException {
    throw new XMLStreamException( "Processing instructions not supported" );
  }

  @Override
  public void writeCData( final String data ) throws XMLStreamException {
    text( factory.createOMText( data, XMLStreamConstants.CDATA ) );
  }

  @Override
  public void writeDTD( final String dtd ) throws XMLStreamException {
    throw new XMLStreamException( "DTD not supported" );
  }

  @Override
  public void writeEntityRef( final String name ) throws XMLStreamException {
    throw new XMLStreamException( "Entity references not supported" );
  }

  @Override
  public void writeStartDocument() throws XMLStreamException {
  }

  @Override
  public void writeStartDocument( final String version ) throws XMLStreamException {
  }

  @Override
  public void writeStartDocument( final String encoding, final String version ) throws XMLStreamException {
  }

  @Override
  public void writeCharacters( final String text ) throws XMLStreamException {
    text( factory.createOMText( text, XMLStreamConstants.CHARACTERS ) );
  }

  @Override
  public void writeCharacters( final char[] text, final int start, final int len ) throws XMLStreamException {
    writeCharacters( new String( text, start, len ) );
  }

  @Override
  public String getPrefix( final String uri ) throws XMLStreamException {
    return null;
  }

  @Override
  public void setPrefix( final String prefix, final String uri ) throws XMLStreamException {
  }

  @Override
  public void setDefaultNamespace( final String uri ) throws XMLStreamException {
  }

  @Override
  public void setNamespaceContext( final NamespaceContext context ) throws XMLStreamException {
  }

  @Override
  public NamespaceContext getNamespaceContext( ) {
    return null;
  }

  @Override
  public Object getProperty( final String name ) throws IllegalArgumentException {
    return null;
  }
}
