package com.eucalyptus.reporting.art.renderer.document;

import java.io.IOException;
import java.io.Writer;

public interface Document {

	public abstract Document open() throws IOException;

	public abstract Document close() throws IOException;

	public abstract Document tableOpen() throws IOException;

	public abstract Document tableClose() throws IOException;

	public abstract Document textLine(String text, int emphasis)
			throws IOException;

	public abstract Document newRow() throws IOException;

	public abstract Document addLabelCol(int indent, String val)
			throws IOException;

	public abstract Document addValCol(String val) throws IOException;

	public abstract Document addValCol(Long val) throws IOException;

	public abstract Document addValCol(Double val) throws IOException;

	public abstract Document addValCol(String val, int colspan, String align)
			throws IOException;

	public abstract Document addEmptyValCols(int num) throws IOException;

	public abstract Document addEmptyLabelCols(int num) throws IOException;
	
	public void setWriter(Writer writer);

}