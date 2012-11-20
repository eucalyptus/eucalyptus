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

package com.eucalyptus.system.log;

import java.text.DecimalFormat;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;

public class EucaPatternParser extends PatternParser {

	public EucaPatternParser(String pattern) {
		super(pattern);
	}
	@Override 
	protected void finalizeConverter(char c) {
		PatternConverter pc = null;
		switch(c) {
		case 'i':
			pc = new ThreadIdPatternConverter(formattingInfo);
			currentLiteral.setLength(0);
			break;
		case 'f':
			pc = new FileAndLineNumberPatternConverter(formattingInfo);
			currentLiteral.setLength(0);
			break;
		default:
			super.finalizeConverter(c);
			return;
		}
		addConverter(pc);
	}

	private class FileAndLineNumberPatternConverter extends PatternConverter {
		FileAndLineNumberPatternConverter(FormattingInfo formattingInfo) {
			super(formattingInfo);
		}

		public String convert(LoggingEvent event) {
			LocationInfo locationInfo = event.getLocationInformation();
			return locationInfo.getFileName() + ":" + locationInfo.getLineNumber();
		}
	}
	private static class ThreadIdPatternConverter extends PatternConverter {
		private static final DecimalFormat df = new DecimalFormat("00000000000");
		int type;

		ThreadIdPatternConverter(FormattingInfo formattingInfo) {
			super(formattingInfo);
		}
		@Override
		public String convert(LoggingEvent event) {
			if (event instanceof EucaLoggingEvent) {
				return df.format(((EucaLoggingEvent) event).getThreadId());
			} else {
				return "unknown";
			}
		}
	}
}
