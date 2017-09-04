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
		case 'o':
		  pc = new CorrelationIdPatternConverter(formattingInfo);
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

		ThreadIdPatternConverter(FormattingInfo formattingInfo) {
			super(formattingInfo);
		}
		@Override
		public String convert(LoggingEvent event) {
			final Long threadId = (Long) event.getMDC( "thread-id" );
			return threadId == null ?
					"unknown" :
					df.format(threadId);
		}
	}
	private static class CorrelationIdPatternConverter extends PatternConverter {
	  CorrelationIdPatternConverter(FormattingInfo formattingInfo) {
	    super(formattingInfo);
	  }
	  
	  @Override
	  public String convert(LoggingEvent event) {
	    if(event.getProperties().containsKey("correlation-id")){
	      /// format
	      /// {req_id prefix{0:8}-msg_id_for_ordering{9:13}}
	      final String correlationId = event.getProperty("correlation-id");
	      if(correlationId!=null && correlationId.length()>=36) {
	        if(correlationId.contains("::")) {
	          String postfix = correlationId.substring(correlationId.indexOf("::")+2);
	          String postfixHex = "";
	          if(postfix.length()>13)
	            postfixHex = postfix.substring(9,13);
	          return correlationId.substring(0, 8) + "-" + postfixHex;
	        }else{
	          return correlationId.substring(0, 13);
	        }
	      }
	      else
	        return "unknown";
	    }else
	      return "unknown";
	  }
	}
}
