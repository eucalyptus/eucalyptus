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

package com.eucalyptus.troubleshooting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.eucalyptus.system.BaseDirectory;

public class Fault {
	Fault() {}
	private static final Logger LOG = Logger.getLogger(Fault.class);
	private static final File faultLogFile = BaseDirectory.LOG.getChildFile("cloud-faults.log");
	private static final String ASTERISK_LINE = makeString(72, '*');
	private static final String EMPTY_LINE = "";
	private static final String INNER_PREFIX = "| ";
	private String faultLine = "${err_id} ${timestamp} ${fault_msg}";
	private String conditionLine = "${condition}: ${condition_msg}";
	private String causeLine = "${cause}: ${cause_msg}";
	private String initiatorLine = "${initiator}: ${initiator_msg}";
	private String locationLine = "${location}: ${location_msg}";
	private String resolutionLine = "${resolution}:";
	private String resolutionMessageLines = "${resolution_msg}";
	
	// Fault looks like
	//***********************************************************************
	//| ${err_id} ${timestamp} ${fault_msg}
	//|
	//| ${condition}: ${condition_msg}
	//| ${cause}: ${cause_msg}
	//| ${initiator}: ${initiator_msg} 
	//| ${location}: ${location_msg}
	//| ${resolution}: 
	//|
	//| $resolution_msg
	//***********************************************************************
	
	private static String prefixLines(String prefix, String... lines) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		for (String line:lines) {
			BufferedReader bReader = new BufferedReader(new StringReader(line));
			String innerLine = null;
			try {
				while ((innerLine = bReader.readLine()) != null) {
					out.println(prefix + innerLine);
				}
				bReader.close();
			} catch (IOException e) {
				// TODO something else
				e.printStackTrace();
			}
		}
		out.close();
		return stringWriter.toString();
	}
	
	
	private static String makeString(int i, char c) {
		char[] stringArray = new char[i];
		Arrays.fill(stringArray, c);
		return new String(stringArray);
	}

	public Fault withVar(String key, String value) {
		String realKey = "${" + key + "}";
		Fault retVal = new Fault();
		retVal.faultLine = this.faultLine.replace(realKey, value);
		retVal.conditionLine = this.conditionLine.replace(realKey, value);
		retVal.causeLine = this.causeLine.replace(realKey, value);
		retVal.initiatorLine = this.initiatorLine.replace(realKey, value);
		retVal.locationLine = this.locationLine.replace(realKey, value);
		retVal.resolutionLine = this.resolutionLine.replace(realKey, value);
		retVal.resolutionMessageLines = this.resolutionMessageLines.replace(realKey, value);
		return retVal;
	}

	public void log(Writer writer) throws IOException {
		writer.write(toString());
	}
	
	public void log() {
		synchronized(Fault.class) {
			if (!faultLogFile.getParentFile().isDirectory()) {
			}

			PrintWriter out = null;
			try {
				out = new PrintWriter(new FileWriter(faultLogFile, true));
				out.println(toString());
			} catch (IOException ex) {
				LOG.error("Can not open " + faultLogFile.getAbsolutePath() + " for writing.");
			} finally {
				if (out != null) {
					out.close();
				}
			}
		}
	}

	public String toString() {
		// TODO: make this configurable
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		String[] linesToCenter = new String[] {
				conditionLine, 
				causeLine,
				initiatorLine,
				locationLine,
				resolutionLine,
		};
		// TODO: make this cleaner
		linesToCenter = center(linesToCenter,':');
		conditionLine = linesToCenter[0];
		causeLine = linesToCenter[1];
		initiatorLine = linesToCenter[2];
		locationLine = linesToCenter[3];
		resolutionLine = linesToCenter[4]; 
		out.println();
		out.println(ASTERISK_LINE);
		out.println(prefixLines(INNER_PREFIX, 
							faultLine, 
							EMPTY_LINE, 
							conditionLine, 
							causeLine,
							initiatorLine,
							locationLine,
							resolutionLine,
							EMPTY_LINE, 
							resolutionMessageLines)
					);
		out.println(ASTERISK_LINE);
		out.close();
		return stringWriter.toString();
	}


	private static String[] center(String[] linesToCenter, char c) {
		if (linesToCenter == null) return null;
		String[] retVal = new String[linesToCenter.length];
		int[] charPos = new int[linesToCenter.length];
		int maxCharPos = -1;
		for (int i=0;i<linesToCenter.length; i++) {
			if (linesToCenter[i] == null) return linesToCenter;
			charPos[i] = linesToCenter[i].indexOf(c);
			if (charPos[i] == -1) return linesToCenter;
			if (maxCharPos < charPos[i]) {
				maxCharPos = charPos[i];
			}
		}
		for (int i=0;i<linesToCenter.length; i++) {
			retVal[i] = makeString(maxCharPos - charPos[i], ' ')+ linesToCenter[i];
		}
		return retVal;
	}
	

}
