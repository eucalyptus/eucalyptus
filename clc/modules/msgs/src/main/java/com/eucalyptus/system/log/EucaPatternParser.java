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