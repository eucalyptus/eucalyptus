/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.util;

import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import com.google.common.base.MoreObjects;

/**
 * Delegating XMLStreamReader that maps namespaces.
 *
 * <p>Note that this class does not currently perform mapping for the NamespaceContext.</p>
 */
public class NamespaceMappingXMLStreamReader implements XMLStreamReader {
  private final XMLStreamReader delegate;
  private final Map<String,String> namespaceMappings;

  public NamespaceMappingXMLStreamReader( final XMLStreamReader delegate,
                                          final Map<String,String> namespaceMappings ) {
    this.delegate = delegate;
    this.namespaceMappings = namespaceMappings;
  }

  private String mapNs( final String namespaceUri ) {
    return MoreObjects.firstNonNull( namespaceMappings.get(namespaceUri), namespaceUri );
  }

  @Override
  public Object getProperty( final String name ) throws IllegalArgumentException {
    return delegate.getProperty( name );
  }

  @Override
  public int next() throws XMLStreamException {
    return delegate.next( );
  }

  @Override
  public void require( final int type, final String namespaceURI, final String localName ) throws XMLStreamException {
    delegate.require( type, mapNs( namespaceURI ), localName );
  }

  @Override
  public String getElementText() throws XMLStreamException {
    return delegate.getElementText( );
  }

  @Override
  public int nextTag() throws XMLStreamException {
    return delegate.nextTag( );
  }

  @Override
  public boolean hasNext() throws XMLStreamException {
    return delegate.hasNext( );
  }

  @Override
  public void close() throws XMLStreamException {
    delegate.close( );
  }

  @Override
  public String getNamespaceURI( final String prefix ) {
    return mapNs( delegate.getNamespaceURI( prefix ) );
  }

  @Override
  public boolean isStartElement() {
    return delegate.isStartElement( );
  }

  @Override
  public boolean isEndElement() {
    return delegate.isEndElement( );
  }

  @Override
  public boolean isCharacters() {
    return delegate.isCharacters( );
  }

  @Override
  public boolean isWhiteSpace() {
    return delegate.isWhiteSpace( );
  }

  @Override
  public String getAttributeValue( final String namespaceURI, final String localName ) {
    return delegate.getAttributeValue( mapNs( namespaceURI ), localName );
  }

  @Override
  public int getAttributeCount() {
    return delegate.getAttributeCount( );
  }

  @Override
  public QName getAttributeName( final int index ) {
    return delegate.getAttributeName( index );
  }

  @Override
  public String getAttributeNamespace( final int index ) {
    return delegate.getAttributeNamespace( index );
  }

  @Override
  public String getAttributeLocalName( final int index ) {
    return delegate.getAttributeLocalName( index );
  }

  @Override
  public String getAttributePrefix( final int index ) {
    return delegate.getAttributePrefix( index );
  }

  @Override
  public String getAttributeType( final int index ) {
    return delegate.getAttributeType( index );
  }

  @Override
  public String getAttributeValue( final int index ) {
    return delegate.getAttributeValue( index );
  }

  @Override
  public boolean isAttributeSpecified( final int index ) {
    return delegate.isAttributeSpecified( index );
  }

  @Override
  public int getNamespaceCount() {
    return delegate.getNamespaceCount( );
  }

  @Override
  public String getNamespacePrefix( final int index ) {
    return delegate.getNamespacePrefix( index );
  }

  @Override
  public String getNamespaceURI( final int index ) {
    return mapNs( delegate.getNamespaceURI( index ) );
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return delegate.getNamespaceContext( );
  }

  @Override
  public int getEventType() {
    return delegate.getEventType( );
  }

  @Override
  public String getText() {
    return delegate.getText( );
  }

  @Override
  public char[] getTextCharacters() {
    return delegate.getTextCharacters( );
  }

  @Override
  public int getTextCharacters( final int sourceStart, final char[] target, final int targetStart, final int length ) throws XMLStreamException {
    return delegate.getTextCharacters( sourceStart, target, targetStart, length );
  }

  @Override
  public int getTextStart() {
    return delegate.getTextStart( );
  }

  @Override
  public int getTextLength() {
    return delegate.getTextLength( );
  }

  @Override
  public String getEncoding() {
    return delegate.getEncoding( );
  }

  @Override
  public boolean hasText() {
    return delegate.hasText( );
  }

  @Override
  public Location getLocation() {
    return delegate.getLocation( );
  }

  @Override
  public QName getName() {
    return delegate.getName( );
  }

  @Override
  public String getLocalName() {
    return delegate.getLocalName( );
  }

  @Override
  public boolean hasName() {
    return delegate.hasName( );
  }

  @Override
  public String getNamespaceURI() {
    return mapNs( delegate.getNamespaceURI( ) );
  }

  @Override
  public String getPrefix() {
    return delegate.getPrefix( );
  }

  @Override
  public String getVersion() {
    return delegate.getVersion( );
  }

  @Override
  public boolean isStandalone() {
    return delegate.isStandalone( );
  }

  @Override
  public boolean standaloneSet() {
    return delegate.standaloneSet( );
  }

  @Override
  public String getCharacterEncodingScheme() {
    return delegate.getCharacterEncodingScheme( );
  }

  @Override
  public String getPITarget() {
    return delegate.getPITarget( );
  }

  @Override
  public String getPIData() {
    return delegate.getPIData( );
  }
}
