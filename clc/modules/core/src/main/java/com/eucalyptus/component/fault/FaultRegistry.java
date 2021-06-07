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
package com.eucalyptus.component.fault;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.eucalyptus.util.XMLParser;

public class FaultRegistry {
	private static final Logger LOG = Logger.getLogger(FaultRegistry.class);
	private static final String EUCAFAULTS = "eucafaults";
	private static final String COMMON = "common";
	private static final String VAR = "var";
	private static final String FAULT = "fault";
	private static final String ID = "id";
	private static final String VERSION = "version";
	private static final String DESCRIPTION = "description";
	private static final String NAME = "name";
	private static final String VALUE = "value";
	private static final String LOCALIZED = "localized";
	private static final String MESSAGE = "message";
	private static final String XML_SUFFIX = ".xml";
	private static final String COMMON_XML = COMMON + XML_SUFFIX;
	
	public static final Fault SUPPRESSED_FAULT = new Fault(); // token to indicate fault is suppressed

	public FaultRegistry() {
		commonMap = new HashMap<String, Common>();
		faultMap = new HashMap<Integer, Fault>();
		suppressedFaults = new HashSet<Integer>();
	}
		
	void crawlDirectory(File rootDir) {
		LOG.debug("Crawling fault directory " + rootDir);
		if (rootDir != null && rootDir.isDirectory()) {
			File commonXMLFile = new File(rootDir, COMMON_XML);
			if ( commonXMLFile.isFile( ) ) {
				parseCommonXMLFile( commonXMLFile, commonMap );
				File[] faultFiles = rootDir.listFiles( new FaultFileFilter( ) );
				if ( faultFiles != null ) {
					for ( File faultFile : faultFiles ) {
						parseFaultXMLFile( faultFile, faultMap, commonMap, suppressedFaults );
					}
				}
			} else {
				LOG.info(commonXMLFile + " either does not exist or is not a file, skipping directory.");
			}
		} else {
			LOG.info(rootDir + " either does not exist or is not a directory, skipping.");
		}
	}

	private void parseCommonXMLFile(File commonXMLFile,
			Map<String, Common> commonMap) {
		try {
			LOG.warn("Parsing common file " + commonXMLFile);
			DocumentBuilder dBuilder = XMLParser.getDocBuilder();
			if (dBuilder != null) {
	  		  Document doc = dBuilder.parse(commonXMLFile);
			  Element docElement = doc.getDocumentElement();
			  docElement.normalize();
			  if (!EUCAFAULTS.equalsIgnoreCase(docElement.getTagName())) {
				  LOG.warn("File " + commonXMLFile + " contains the wrong outer XML tag, will not be parsed.");
			  } else {
				  NodeList children = docElement.getChildNodes();
				  final int length = children.getLength();
				  for (int i=0;i < length; i++) {
					  Node currentNode = children.item(i);
					  if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
						  Element currentElement = (Element) currentNode;
						  if (COMMON.equalsIgnoreCase(currentElement.getTagName())) {
							  parseCommonElement(currentElement, commonMap);
						  }
				  	  }
				  }
			    }
		      LOG.debug("Successfully parsed " + commonXMLFile);
			}
		} catch (SAXException ex) {
			LOG.error(ex);
		} catch (IOException ex) {
			LOG.error(ex);
		}
	}

	private void parseFaultXMLFile(File faultXMLFile,
			Map<Integer, Fault> faultMap, Map<String, Common> commonMap, Set<Integer> suppressedFaults) {
		try {
			LOG.debug("Parsing fault file " + faultXMLFile);
			// Special case, zero length file.  (Turn on or off)
			int faultId = parseIdFromFileName(faultXMLFile.getName());
			if (suppressedFaults.contains(faultId) && faultXMLFile.length() != 0) {
				LOG.debug("Unsupressing fault " + faultId);
				suppressedFaults.remove(faultId);
			// Zero length means suppress fault
			} else if (faultXMLFile.exists() && faultXMLFile.length() == 0) {
				LOG.debug("Supressing fault " + faultId);
				faultMap.remove(faultId);
				suppressedFaults.add(faultId);
				return;
			}
			DocumentBuilder dBuilder = XMLParser.getDocBuilder();
			Document doc = dBuilder.parse(faultXMLFile);
			Element docElement = doc.getDocumentElement();
			docElement.normalize();
			if (!EUCAFAULTS.equalsIgnoreCase(docElement.getTagName())) {
				LOG.warn("File " + faultXMLFile + " contains the wrong outer XML tag, will not be parsed.");
			} else {
				// The way C parses the code, it uses exactly one common.xml file, the highest priority one.
				// We will do the same here.  All existing faults have a reference to the common map though,
				// and it is possible that a different common.xml file will be parsed.  We will thus keep
				// the map reference around, and clear it out when we find a new common.xml file.
				// TODO: consider simply overwriting values in the map.
				
				commonMap.clear(); // Since all faults reference this common map, and we have a ner
				                   // common.xml file, we will start over, rather than overwrite fields.
									
				NodeList children = docElement.getChildNodes();
				final int length = children.getLength();
				for (int i=0;i < length; i++) {
					Node currentNode = children.item(i);
					if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
						Element currentElement = (Element) currentNode;
						if (FAULT.equalsIgnoreCase(currentElement.getTagName())) {
							Fault fault = parseFaultElement(currentElement, commonMap);
							if (fault.getId() != faultId) {
								LOG.warn("Fault " + fault.getId() + " found in file " + faultXMLFile + ", in the wrong file.  Will not be processed");
							} else {
								LOG.debug("Successfully parsed " + faultXMLFile + " and read in fault " + fault.getId());
								faultMap.put(fault.getId(), fault);
							}
						}
					}
				}
			}
		} catch (SAXException ex) {
			LOG.error(ex);
		} catch (IOException ex) {
			LOG.error(ex);
		}
	}

	private Fault parseFaultElement(Element element,
			Map<String, Common> commonMap2) {
		if (element == null) return null;
		Fault fault = new Fault();
		fault.setId(-1);
		try {
			fault.setId(Integer.parseInt(getAttribute(element, ID)));
		} catch (Exception ex) {
			LOG.warn("Illegal ID passed in, will use -1");
		}
		fault.setCommonMap(commonMap);
		FaultMessage message = new FaultMessage();
		fault.setMessage(message);
		message.setMessage(getAttribute(element, MESSAGE));
		message.setLocalized(getAttribute(element, LOCALIZED));
		fault.setFaultFieldMap(new HashMap<FaultFieldName, FaultField>());
		NodeList children = element.getChildNodes();
		final int length = children.getLength();
		for (int i=0;i < length; i++) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = (Element) currentNode;
				FaultFieldName name = null;
				try {
				 	name = FaultFieldName.valueOf(currentElement.getTagName());
				} catch (Exception ex) {
					// not a tag we like
					continue;
				}
				FaultField faultField = new FaultField();
				faultField.setName(name);
				faultField.setLocalizedAttribute(getAttribute(currentElement, LOCALIZED));
				faultField.setMessageAttribute(getAttribute(currentElement, MESSAGE));
				faultField.setLocalizedElement(getTextElement(currentElement, LOCALIZED));
				faultField.setMessageElement(getTextElement(currentElement, MESSAGE));
				fault.getFaultFieldMap().put(faultField.getName(), faultField);
			}
		}
		return fault;
	}

	private String getTextElement(Element element, String subElementName) {
		if (element == null || subElementName == null) return null;
		NodeList children = element.getChildNodes();
		final int length = children.getLength();
		for (int i=0;i < length; i++) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = (Element) currentNode;
				if (subElementName.equalsIgnoreCase(currentElement.getTagName())) {
					// for text elements, it is the first child
					try {
						return currentElement.getChildNodes().item(0).getNodeValue();
					} catch (Exception ex) {
						return null;
					}
				}
			}
		}
		return null;
	}

	private int parseIdFromFileName(String name) {
		// filename should be NNNN.xml
		try {
			return Integer.parseInt(name.substring(0, name.length() - XML_SUFFIX.length()));
		} catch (Exception ex) {
			return -1;
		}
	}

	private void parseCommonElement(Element element,
			Map<String, Common> commonMap) {
		if (element == null) return;
		NodeList children = element.getChildNodes();
		final int length = children.getLength();
		for (int i=0;i < length; i++) {
			Node currentNode = children.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = (Element) currentNode;
				if (VAR.equalsIgnoreCase(currentElement.getTagName())) {
					Common common = new Common();
					common.setName(getAttribute(currentElement, NAME));
					common.setValue(getAttribute(currentElement, VALUE));
					common.setLocalized(getAttribute(currentElement, LOCALIZED));
					if (common.getName() != null) {
						commonMap.put(common.getName(), common);
					}
				}
			}
		}
	}
	private Set<Integer> suppressedFaults; // new request
	private Map<String, Common> commonMap;
	private Map<Integer, Fault> faultMap;

	private String getAttribute(Element element, String attributeName) {
		if (element == null) return null;
		NamedNodeMap namedNodeMap = element.getAttributes();
		if (namedNodeMap == null) return null;
		Node localizedNode = namedNodeMap.getNamedItem(attributeName);
		if (localizedNode == null) return null;
		return localizedNode.getNodeValue();
	}
	
	public class FaultFileFilter implements FileFilter {
		@Override
		public boolean accept(File f) {
			if (f != null && f.isFile() && f.getName().toLowerCase().endsWith(XML_SUFFIX)) {
				String name = f.getName().toLowerCase();
				String prefix = name.substring(0, name.length() - XML_SUFFIX.length());
				try {
					return Integer.parseInt(prefix) > 0;
				} catch (NumberFormatException ignore) {
					; 
				}
			}
			return false;
		}
	}

	public Fault lookupFault(int id) {
		if (suppressedFaults.contains(id)) {
			return SUPPRESSED_FAULT;
		}
		Fault fault = faultMap.get(id);
		if (fault == null) {
			return fault;
		} else {
			return (Fault) fault.clone(); // so they can do withVar() w/o damaging the template
		}
	}
	
}
