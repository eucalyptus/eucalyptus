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
 ************************************************************************/

package com.eucalyptus.reporting.art.renderer.document;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class CsvDocument
	implements Document
{
	private List<String> colVals = null;
    private Writer writer;
    private boolean rowHasLabel = false;
    private int rowIndent = 3;

    @Override
    public void setUnlabeledRowIndent( final int num ) {
      this.rowIndent = num;
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
    	return this;
    }
    
    @Override
	public Document close()
    	throws IOException
    {
    	writer.flush();
    	return this;
    }
    
    @Override
	public Document tableOpen()
    	throws IOException
    {
    	return this;
    }

    private void writeRow()
    	throws IOException
    {
    	StringBuilder sb = new StringBuilder();
    	for (int i=0; i<colVals.size(); i++) {
    		if (i>0) sb.append(",");
    		sb.append(colVals.get(i));
    	}
    	sb.append("\n");
    	writer.write(sb.toString());
    }
    
    @Override
	public Document tableClose()
		throws IOException
	{
    	if (colVals != null) {
    		writeRow();
    	}
    colVals = new ArrayList<String>();
    return this;
	}
    
    @Override
	public Document textLine(String text, int emphasis)
    	throws IOException
    {
    	writer.write(text + "\n");
    	return this;    	
    }

    @Override
	public Document newRow()
    	throws IOException
    {
    	if (colVals != null && !colVals.isEmpty()) {
    		writeRow();
    	}
    	colVals = new ArrayList<String>();
    	rowHasLabel = false;
    	return this;    	
    }

    @Override
	public Document addLabelCol(int indent, String val)
		throws IOException
	{
    	rowHasLabel = true;
    	addEmptyLabelCols(indent);
    	addCol(val, 1);
    	addEmptyLabelCols(rowIndent-(indent+1));
    	return this;
	}	


    @Override
	public Document addValCol(String val)
    	throws IOException
    {
        return addCol(val, 1);
    }

  @Override
  public Document addValCol(Integer val)
      throws IOException
  {
    return addCol((val==null)?null:val.toString(), 1);
  }

  @Override
	public Document addValCol(Long val)
		throws IOException
    {
        return addCol((val==null)?null:val.toString(), 1);
    }

    @Override
	public Document addValCol(Double val)
		throws IOException
    {
        return addCol((val==null)?null:String.format("%3.1f", val), 1);
    }

    @Override
	public CsvDocument addValCol(String val, int colspan, String align)
		throws IOException
	{
    	return addCol(val, colspan);
	}

    private CsvDocument addCol(String val, int colspan)
		throws IOException
    {
    	if (!rowHasLabel) {
    		addEmptyLabelCols(rowIndent);
    		rowHasLabel = true;
    	}
    	colVals.add(val);
    	for (int i=1; i<colspan; i++) {
    		colVals.add("");
    	}
        return this;
    }

    @Override
	public CsvDocument addEmptyValCols(int num)
		throws IOException
   {
        for (int i=0; i<num; i++) {
        	colVals.add("");
        }
        return this;
    }

    @Override
	public Document addEmptyLabelCols(int num)
		throws IOException
	{
    	for (int i=0; i<num; i++) {
        	colVals.add("");
    	}
    	return this;
	}
}
