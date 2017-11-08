/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/
package com.eucalyptus.component.fault;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;


public class Fault implements Cloneable {
	private int id;
	private FaultMessage message;
	private Map<String, Common> commonMap;
	private Map<FaultFieldName, FaultField> faultFieldMap;
	private String stringRepresentation;
	private static final String INNER_FAULT_PREFIX = "  "; // "| ";
	private static final String UNKNOWN = "unknown"; 
	public synchronized Fault withVar(String key, String value) {
		toString();
		String realKey = "${" + key + "}";
		stringRepresentation = stringRepresentation.replace(realKey, value);
		return this;
	}
	
	@Override
	public synchronized String toString() {
		if (stringRepresentation != null) {
			return stringRepresentation;
		}
		StringWriter sWriter = new StringWriter();
		PrintWriter out = new PrintWriter(sWriter);

		// Fault looks like
		//***********************************************************************
		//  ${err_id} ${timestamp} ${fault_msg}
		// 
		//  ${condition}: ${condition_msg}
		//  ${cause}: ${cause_msg}
		//  ${initiator}: ${initiator_msg} 
		//  ${location}: ${location_msg}
		//  ${resolution}: 
		// 
		//  $resolution_msg
		//***********************************************************************
		String lineOfAsterisks = stringFill(72, '*');
		out.println(lineOfAsterisks);
		out.println(INNER_FAULT_PREFIX + headerLine());
		out.println();
		// the fields need to be right justified.  Figure out how many characters you need
		int fieldWidth = 0;
		for (FaultFieldName name: FaultFieldName.values()) {
			String nameStr = name.toString();
			if (commonMap.get(nameStr) != null && commonMap.get(nameStr).getEffectiveValue() != null) {
				nameStr = commonMap.get(nameStr).getEffectiveValue();
			}
			fieldWidth = Math.max(fieldWidth, nameStr.length());
		}
		for (FaultFieldName name: FaultFieldName.values()) {
			String nameStr = name.toString();
			if (commonMap.get(nameStr) != null && commonMap.get(nameStr).getEffectiveValue() != null) {
				nameStr = commonMap.get(nameStr).getEffectiveValue();
			}
			FaultField faultField = faultFieldMap.get(name);
			String faultFieldValue = getEffectiveValueOrUnknown(faultField);
			// pad the faultFieldKey
			out.println(INNER_FAULT_PREFIX + lpad(nameStr, fieldWidth) + ": " + faultFieldValue);
		}
		out.println(lineOfAsterisks);
		out.close();
		stringRepresentation =  sWriter.toString();
//		Commented out.  C representation does not use common vars for general purpose replacement
//		// fill in all common vars...
//		for (String commonKey: commonMap.keySet()) {
//			String realKey = "${" + commonKey + "}";
//			Common valueCommon = commonMap.get(commonKey);
//			if (valueCommon != null && valueCommon.getEffectiveValue() != null) {
//				stringRepresentation = stringRepresentation.replace(realKey, valueCommon.getEffectiveValue());
//			}
//		}
		return stringRepresentation;	
	}

	private String lpad(String s, int fieldWidth) {
		String paddedStringTotal = stringFill(fieldWidth, ' ') + s;
		return paddedStringTotal.substring(paddedStringTotal.length() - fieldWidth);
	}

	private String getEffectiveValueOrUnknown(EffectiveValue effectiveValue) {
		Common unknownCommon = commonMap.get(UNKNOWN);
		String localizedUnknown = UNKNOWN;
		if (unknownCommon != null && unknownCommon.getEffectiveValue() != null) {
			localizedUnknown = unknownCommon.getEffectiveValue();
		}
		if (effectiveValue != null && effectiveValue.getEffectiveValue() != null) {
			return effectiveValue.getEffectiveValue();
		}
		return localizedUnknown;
	}
	private String headerLine() {
		DecimalFormat df = new DecimalFormat("0000");
		StringBuilder builder = new StringBuilder();
		builder.append("ERR-" + df.format(id));
		builder.append(" ");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// Remove UTC until logs match
		//sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		builder.append(sdf.format(new Date()));
		builder.append(" ");
		builder.append(getEffectiveValueOrUnknown(message));
		return builder.toString();
	}

	private String stringFill(int length, char c) {
		char[] buff = new char[length];
		Arrays.fill(buff, c);
		return new String(buff);
	}

	FaultMessage getMessage() {
		return message;
	}

	void setMessage(FaultMessage message) {
		this.message = message;
	}

	Map<FaultFieldName, FaultField> getFaultFieldMap() {
		return faultFieldMap;
	}

	void setFaultFieldMap(Map<FaultFieldName, FaultField> faultFieldMap) {
		this.faultFieldMap = faultFieldMap;
	}

	@Override
	protected Object clone() {
		Fault clone = new Fault();
		clone.id = this.id;
		clone.faultFieldMap = this.faultFieldMap;
		clone.commonMap = this.commonMap;
		clone.message = this.message;
		return clone;
	}

	int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}

	Map<String, Common> getCommonMap() {
		return commonMap;
	}

	void setCommonMap(Map<String, Common> commonMap) {
		this.commonMap = commonMap;
	}
}