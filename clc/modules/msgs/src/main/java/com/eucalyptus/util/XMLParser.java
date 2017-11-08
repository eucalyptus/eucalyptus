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

package com.eucalyptus.util;

import org.apache.log4j.Logger;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


public class XMLParser {

	private static Logger LOG = Logger.getLogger(XMLParser.class);

	private DocumentBuilder docBuilder;
	private Document docRoot;
	private XPath xpath;
	private File file;
	private String rawData;

	public XMLParser() {
		xpath = XPathFactory.newInstance().newXPath();
		docBuilder = getDocBuilder();
	}

	public XMLParser(File file) {
		this();
		this.file = file;
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			docRoot = docBuilder.parse(fileInputStream);
			fileInputStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if(fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
		}
	}

	public XMLParser(String xmlData) {
		this();
		this.rawData = xmlData;
		InputStream in = new ByteArrayInputStream(xmlData.getBytes());
		try {
			docRoot = docBuilder.parse(in);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	public static DocumentBuilderFactory getDocBuilderFactory() {
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		dFactory.setExpandEntityReferences(false);

		try {
			dFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch(ParserConfigurationException ex) {
			LOG.error(ex, ex);
		}

		try{
			dFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch(ParserConfigurationException ex) {
			LOG.error(ex, ex);
		}

		try {
			dFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		} catch(ParserConfigurationException ex) {
			LOG.error(ex, ex);
		}
		return dFactory;
	}

	public static DocumentBuilder getDocBuilder() {
		DocumentBuilderFactory dFactory = getDocBuilderFactory();
		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dFactory.newDocumentBuilder();
			dBuilder.setEntityResolver(new EntityResolver() {
				@Override
				public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
					return new InputSource(new StringReader(""));
				}
			});
		} catch (ParserConfigurationException ex) {
			LOG.error(ex, ex);
		}	
		return dBuilder;
	}

	public static DocumentBuilder getDocBuilderWithDTD() {
		DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
		dFactory.setExpandEntityReferences(false);

		try {
			dFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		} catch(ParserConfigurationException ex) {
			LOG.error(ex, ex);
		}

		DocumentBuilder dBuilder = null;
		try {
			dBuilder = dFactory.newDocumentBuilder();
			dBuilder.setEntityResolver(new EntityResolver() {
				@Override
				public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
					return new InputSource(new StringReader(""));
				}
			});
		} catch (ParserConfigurationException ex) {
			LOG.error(ex, ex);
		}	
		return dBuilder;
	}


	public String getValue(String name) {
		try {
			return (String) xpath.evaluate(name, docRoot, XPathConstants.STRING);
		} catch(XPathExpressionException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public String getValue(Node node, String name) {
		try {
			return (String) xpath.evaluate(name, node, XPathConstants.STRING);
		} catch(XPathExpressionException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public List<String> getValues(String name) {
		try {
			DTMNodeList nodes = (DTMNodeList) xpath.evaluate(name, docRoot, XPathConstants.NODESET);
			ArrayList<String> values = new ArrayList<String>();
			for (int i = 0; i < nodes.getLength(); ++i) {
				values.add(nodes.item(i).getFirstChild().getNodeValue());
			}
			return values;
		} catch(XPathExpressionException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public DTMNodeList getNodes(String name) {
		try {
			DTMNodeList nodes = (DTMNodeList) xpath.evaluate(name, docRoot, XPathConstants.NODESET);
			return nodes;
		} catch(XPathExpressionException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public DTMNodeList getNodes(Node node, String name) {
		try {
			DTMNodeList nodes = (DTMNodeList) xpath.evaluate(name, node, XPathConstants.NODESET);
			return nodes;
		} catch(XPathExpressionException ex) {
			ex.printStackTrace();
		}
		return null;
	}


	public String getXML(String name) {
		if(rawData == null) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				rawData = "";
				byte[] bytes = new byte[1024];
				int bytesRead = 0;
				while ((bytesRead = in.read(bytes)) > 0) {
					rawData += new String(bytes, 0, bytesRead);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if(in != null)
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}

		String startString = new String("<" + name + ">");
		String endString = new String("</" + name + ">");
		int start = rawData.indexOf(startString);
		int end = rawData.indexOf(endString);
		if(end > start) {
			end += endString.length();
			return rawData.substring(start, end);
		}
		return null;
	}
}
