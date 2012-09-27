package com.eucalyptus.system.log;

import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
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
		default:
			super.finalizeConverter(c);
			return;
		}
		addConverter(pc);
	}

	private static class ThreadIdPatternConverter extends PatternConverter {
		int type;

		ThreadIdPatternConverter(FormattingInfo formattingInfo) {
			super(formattingInfo);
		}
		@Override
		public String convert(LoggingEvent event) {
			if (event instanceof EucaLoggingEvent) {
				return "" + ((EucaLoggingEvent) event).getThreadId();
			} else {
				return "unknown";
			}
		}
	}
}