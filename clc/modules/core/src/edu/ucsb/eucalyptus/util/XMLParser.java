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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */
package edu.ucsb.eucalyptus.util;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.apache.commons.jxpath.NodeSet;
import org.apache.xml.dtm.ref.DTMNodeList;
import org.apache.xml.dtm.ref.DTMNodeIterator;
import org.apache.xml.dtm.DTMIterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;


public class XMLParser {

    private DocumentBuilderFactory docFactory;
    private DocumentBuilder docBuilder;
    private Document docRoot;
    private XPath xpath;
    private File file;
    private String rawData;

    public XMLParser() {
        xpath = XPathFactory.newInstance().newXPath();
        docFactory = DocumentBuilderFactory.newInstance();
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
    }

    public XMLParser(File file) {
        this();
        this.file = file;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            docRoot = docBuilder.parse(fileInputStream);
        } catch (Exception ex) {
            ex.printStackTrace();
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

    public String getValue(String name) {
        try {
            return (String) xpath.evaluate(name, docRoot, XPathConstants.STRING);
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

    public String getXML(String name) {
        if(rawData == null) {
            try {
                FileInputStream in = new FileInputStream(file);
                rawData = "";
                byte[] bytes = new byte[1024];
                int bytesRead = 0;
                while ((bytesRead = in.read(bytes)) > 0) {
                    rawData += new String(bytes, 0, bytesRead);
                }
                in.close();
            } catch (Exception ex) {
                ex.printStackTrace();
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