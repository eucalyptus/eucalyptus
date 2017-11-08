/*************************************************************************
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.reporting.art.util;

public class IndentingStringBuffer
{
	private static final int NUM_SPACES_PER_INDENT_DEFAULT = 3;
	
	private final StringBuffer sb;
	private final String indentSpaces;
	
	public IndentingStringBuffer(final int numSpacesPerIndent)
	{
		this.sb = new StringBuffer();
		this.indentSpaces = spaces(numSpacesPerIndent);
	}
	
	public IndentingStringBuffer()
	{
		this(NUM_SPACES_PER_INDENT_DEFAULT);
	}
	
	public void append(String string)
	{
		sb.append(string);
	}

	public void appendIndentLine(int numIndents, String string)
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
	
	public static String spaces(int numIndents)
	{
		/* Generate a string with the appropriate number of spaces for indentation */
		char[] charAry = new char[numIndents*NUM_SPACES_PER_INDENT_DEFAULT];
		for (int i=0; i<charAry.length; i++) {
			charAry[i] = ' ';
		}
		return new String(charAry);		
	}

}
