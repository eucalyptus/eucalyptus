package com.eucalyptus.reporting.art.util;

public class IndentingStringBuffer
{
	private static final int NUM_SPACES_PER_INDENT_DEFAULT = 3;
	
	private final StringBuffer sb;
	private final String indentSpaces;
	
	public IndentingStringBuffer(final int numSpacesPerIndent)
	{
		this.sb = new StringBuffer();
		
		/* Generate a string with the appropriate number of spaces for indentation */
		char[] charAry = new char[numSpacesPerIndent];
		for (int i=0; i<charAry.length; i++) {
			charAry[i] = ' ';
		}
		indentSpaces = new String(charAry);
	}
	
	public IndentingStringBuffer()
	{
		this(NUM_SPACES_PER_INDENT_DEFAULT);
	}
	
	public void append(String string)
	{
		sb.append(string);
	}

	public void appendIndent(int numIndents, String string)
	{
		for (int i=0; i<numIndents; i++) {
			sb.append(indentSpaces);
		}
		sb.append(string);
	}
	
	public String toString()
	{
		return sb.toString();
	}

}
