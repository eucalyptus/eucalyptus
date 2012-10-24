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

    public CsvDocument()
    {
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
    	return this;    	
    }

    @Override
	public Document addLabelCol(int indent, String val)
		throws IOException
	{
    	addEmptyLabelCols(indent);
    	addCol(val, 3);
    	addEmptyLabelCols(3-indent);
    	rowHasLabel = true;
    	return this;
	}	


    @Override
	public Document addValCol(String val)
    	throws IOException
    {
        return addCol(val, 1);
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
    		addEmptyLabelCols(6);
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
