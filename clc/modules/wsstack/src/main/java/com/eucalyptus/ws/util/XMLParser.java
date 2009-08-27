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
/*
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */
package com.eucalyptus.ws.util;

import org.apache.xml.dtm.ref.DTMNodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


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
