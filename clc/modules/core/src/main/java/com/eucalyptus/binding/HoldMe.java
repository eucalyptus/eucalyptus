/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2001-2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *   or implied.  See the License for the specific language governing
 *   permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.binding;

import java.io.InputStream;
import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.SOAPFactory;

public class HoldMe implements Lock {

  static {
    System.setProperty( "javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory" );
    System.setProperty( "javax.xml.stream.XMLOutputFactory", "com.ctc.wstx.stax.WstxOutputFactory" );
    System.setProperty( "javax.xml.stream.XMLEventFactory", "com.ctc.wstx.stax.WstxEventFactory" );
    System.setProperty( "org.jibx.runtime.impl.parser", "org.jibx.runtime.impl.StAXReaderFactory" );
  }

  private static XMLInputFactory  xmlInputFactory                   = XMLInputFactory.newInstance( );
  private static XMLOutputFactory xmlOutputFactory                  = XMLOutputFactory.newInstance( );
  
  public static Lock              canHas                            = maybeGetLock( );

  @Override
  public void lock( ) {}
  
  private static Lock maybeGetLock( ) {
    return new HoldMe( );
  }
  
  @Override
  public void lockInterruptibly( ) {}

  @Nonnull
  @Override
  public Condition newCondition( ) {
    throw new UnsupportedOperationException( );
  }
  
  @Override
  public boolean tryLock( ) {
    return true;
  }
  
  @Override
  public boolean tryLock( long arg0, @Nonnull TimeUnit arg1 ) {
    return true;
  }
  
  @Override
  public void unlock( ) {}
  
  public static OMFactory getDOOMFactory( ) {
    return OMAbstractFactory.getMetaFactory(OMAbstractFactory.FEATURE_DOM).getOMFactory();
  }
  
  public static SOAPFactory getOMSOAP11Factory( ) {
    return OMAbstractFactory.getSOAP11Factory( );
  }
  
  public static SOAPFactory getOMSOAP12Factory( ) {
    return OMAbstractFactory.getSOAP12Factory( );
  }
  
  public static OMFactory getOMFactory( ) {
    return OMAbstractFactory.getOMFactory( );
  }
  
  public static OMXMLParserWrapper getStAXOMBuilder( XMLStreamReader parser ) {
    return OMXMLBuilderFactory.createStAXOMBuilder( parser );
  }
  
  public static OMXMLParserWrapper getStAXOMBuilder( OMFactory doomFactory, XMLStreamReader xmlStreamReader ) {
    return OMXMLBuilderFactory.createStAXOMBuilder( doomFactory, xmlStreamReader );
  }
  
  public static XMLStreamReader getXMLStreamReader( InputStream in ) throws XMLStreamException {
    return HoldMe.getXMLInputFactory( ).createXMLStreamReader( in );
  }
  
  public static XMLStreamReader getXMLStreamReader( String text ) throws XMLStreamException {
    return HoldMe.getXMLInputFactory( ).createXMLStreamReader( new StringReader( text ) );
  }
  
  public static XMLInputFactory getXMLInputFactory( ) {
    return xmlInputFactory;
  }
  
  public static XMLOutputFactory getXMLOutputFactory( ) {
    return xmlOutputFactory;
  }
  
}
