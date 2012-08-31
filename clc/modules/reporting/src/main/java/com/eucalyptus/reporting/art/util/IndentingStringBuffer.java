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
