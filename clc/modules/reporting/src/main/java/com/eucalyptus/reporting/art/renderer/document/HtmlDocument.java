/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.reporting.art.renderer.document;

import java.io.IOException;
import java.io.Writer;


public class HtmlDocument
	implements Document
{
	private static final int LABEL_WIDTH = 50;
	private static final int VALUE_WIDTH = 80;
	
    private StringBuilder rowSb;
    private Writer writer;
    private boolean rowHasLabel = false;

    public HtmlDocument()
    {
        rowSb = null;
    }

    @Override
    public void setWriter(Writer writer)
    {
    	this.writer = writer;
    }
    
    @Override
	public Document open()
    	throws IOException
    {
        writer.write("<html><body>\n");
    	return this;
    }
    
    @Override
	public Document close()
    	throws IOException
    {
    	writer.write("</body></html>\n");
    	writer.flush();
    	return this;
    }
    
    @Override
	public Document tableOpen()
    	throws IOException
    {
    	writer.write("<table style=\"white-space: nowrap\">\n");
    	return this;
    }

    @Override
	public Document tableClose()
		throws IOException
	{
    	if (rowSb != null) {
    		writer.write("<tr>" + rowSb.toString() + "</tr>\n");
    	}
    	writer.write("</table>\n");
    rowSb = new StringBuilder();
    return this;
	}
    
    @Override
	public Document textLine(String text, int emphasis)
    	throws IOException
    {
    	writer.write(String.format("<h%d>%s</h%d>\n", emphasis, text, emphasis));
    	return this;    	
    }

    @Override
	public Document newRow()
    	throws IOException
    {
    	rowHasLabel = false;
    	if (rowSb != null) {
    		writer.write("<tr>" + rowSb.toString() + "</tr>\n");
    	}
        rowSb = new StringBuilder();
    	return this;    	
    }

    @Override
	public Document addLabelCol(int indent, String val)
		throws IOException
	{
    	addEmptyLabelCols(indent);
        rowSb.append(String.format("<td width=%d colspan=%d align=\"left\">%s</td>",LABEL_WIDTH,3,val));
    	addEmptyLabelCols(3-indent);
    	rowHasLabel = true;
    	return this;
	}	


    @Override
	public Document addValCol(String val)
    	throws IOException
    {
        return addCol(val, VALUE_WIDTH, 1, "center");
    }

    @Override
	public Document addValCol(Long val)
		throws IOException
    {
        return addCol((val==null)?"-":val.toString(), VALUE_WIDTH, 1, "center");
    }

    @Override
	public Document addValCol(Double val)
		throws IOException
    {
        return addCol((val==null)?"-":String.format("%3.1f", val), VALUE_WIDTH, 1, "center");
    }

    @Override
	public HtmlDocument addValCol(String val, int colspan, String align)
		throws IOException
	{
    	return addCol(val, VALUE_WIDTH, colspan, align);
	}

    private HtmlDocument addCol(String val, int width, int colspan, String align)
		throws IOException
    {
    	if (!rowHasLabel) {
    		addEmptyLabelCols(6);
    		rowHasLabel = true;
    	}
        rowSb.append(String.format("<td width=%d colspan=%d align=%s>%s</td>",width,colspan,align,val));
        return this;
    }

    @Override
	public HtmlDocument addEmptyValCols(int num)
		throws IOException
   {
        for (int i=0; i<num; i++) {
            rowSb.append("<td width=" + VALUE_WIDTH + ">&nbsp;</td>");
        }
        return this;
    }

    @Override
	public Document addEmptyLabelCols(int num)
		throws IOException
	{
    	for (int i=0; i<num; i++) {
        	rowSb.append("<td width=" + LABEL_WIDTH + ">&nbsp;</td>");
    	}
    	return this;
	}

}
