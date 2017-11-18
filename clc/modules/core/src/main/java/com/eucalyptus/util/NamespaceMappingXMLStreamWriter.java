/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.util;

import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.google.common.base.MoreObjects;

/**
 * Delegating XMLStreamWriter that maps namespaces.
 *
 * <p>Note that this class does not currently perform mapping for the NamespaceContext.</p>
 */
public class NamespaceMappingXMLStreamWriter implements XMLStreamWriter {

  private final XMLStreamWriter delegate;
  private final Map<String,String> namespaceMappings;

  public NamespaceMappingXMLStreamWriter( final XMLStreamWriter delegate,
                                          final Map<String,String> namespaceMappings ) {
    this.delegate = delegate;
    this.namespaceMappings = namespaceMappings;
  }

  private String mapNs( final String namespaceUri ) {
    return MoreObjects.firstNonNull( namespaceMappings.get(namespaceUri), namespaceUri );
  }

  @Override
  public void writeStartElement(final String localName) throws XMLStreamException {
    delegate.writeStartElement(localName);
  }

  @Override
  public void writeStartElement(final String namespaceURI, final String localName) throws XMLStreamException {
    delegate.writeStartElement( mapNs(namespaceURI), localName);
  }

  @Override
  public void writeStartElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
    delegate.writeStartElement(prefix, localName, mapNs(namespaceURI));
  }

  @Override
  public void writeEmptyElement(final String namespaceURI, final String localName) throws XMLStreamException {
    delegate.writeEmptyElement(mapNs(namespaceURI), localName);
  }

  @Override
  public void writeEmptyElement(final String prefix, final String localName, final String namespaceURI) throws XMLStreamException {
    delegate.writeEmptyElement(prefix, localName, mapNs(namespaceURI));
  }

  @Override
  public void writeEmptyElement(final String localName) throws XMLStreamException {
    delegate.writeEmptyElement(localName);
  }

  @Override
  public void writeEndElement() throws XMLStreamException {
    delegate.writeEndElement();
  }

  @Override
  public void writeEndDocument() throws XMLStreamException {
    delegate.writeEndDocument();
  }

  @Override
  public void close() throws XMLStreamException {
    delegate.close();
  }

  @Override
  public void flush() throws XMLStreamException {
    delegate.flush();
  }

  @Override
  public void writeAttribute(final String localName, final String value) throws XMLStreamException {
    delegate.writeAttribute(localName, value);
  }

  @Override
  public void writeAttribute(final String prefix, final String namespaceURI, final String localName, final String value) throws XMLStreamException {
    delegate.writeAttribute(prefix, mapNs(namespaceURI), localName, value);
  }

  @Override
  public void writeAttribute(final String namespaceURI, final String localName, final String value) throws XMLStreamException {
    delegate.writeAttribute(mapNs(namespaceURI), localName, value);
  }

  @Override
  public void writeNamespace(final String prefix, final String namespaceURI) throws XMLStreamException {
    delegate.writeNamespace(prefix, mapNs(namespaceURI));
  }

  @Override
  public void writeDefaultNamespace(final String namespaceURI) throws XMLStreamException {
    delegate.writeDefaultNamespace(mapNs(namespaceURI));
  }

  @Override
  public void writeComment(final String data) throws XMLStreamException {
    delegate.writeComment(data);
  }

  @Override
  public void writeProcessingInstruction(final String target) throws XMLStreamException {
    delegate.writeProcessingInstruction(target);
  }

  @Override
  public void writeProcessingInstruction(final String target, final String data) throws XMLStreamException {
    delegate.writeProcessingInstruction(target, data);
  }

  @Override
  public void writeCData(final String data) throws XMLStreamException {
    delegate.writeCData(data);
  }

  @Override
  public void writeDTD(final String dtd) throws XMLStreamException {
    delegate.writeDTD(dtd);
  }

  @Override
  public void writeEntityRef(final String name) throws XMLStreamException {
    delegate.writeEntityRef(name);
  }

  @Override
  public void writeStartDocument() throws XMLStreamException {
    delegate.writeStartDocument();
  }

  @Override
  public void writeStartDocument(final String version) throws XMLStreamException {
    delegate.writeStartDocument(version);
  }

  @Override
  public void writeStartDocument(final String encoding, final String version) throws XMLStreamException {
    delegate.writeStartDocument(encoding, version);
  }

  @Override
  public void writeCharacters(final String text) throws XMLStreamException {
    delegate.writeCharacters(text);
  }

  @Override
  public void writeCharacters(final char[] text, final int start, final int len) throws XMLStreamException {
    delegate.writeCharacters(text, start, len);
  }

  @Override
  public String getPrefix(final String uri) throws XMLStreamException {
    return delegate.getPrefix(mapNs(uri));
  }

  @Override
  public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
    delegate.setPrefix(prefix, mapNs(uri));
  }

  @Override
  public void setDefaultNamespace(final String uri) throws XMLStreamException {
    delegate.setDefaultNamespace(mapNs(uri));
  }

  @Override
  public void setNamespaceContext(final NamespaceContext context) throws XMLStreamException {
    delegate.setNamespaceContext(context);
  }

  @Override
  public NamespaceContext getNamespaceContext() {
    return delegate.getNamespaceContext();
  }

  @Override
  public Object getProperty(final String name) throws IllegalArgumentException {
    return delegate.getProperty(name);
  }
}
