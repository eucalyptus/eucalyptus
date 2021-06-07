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

package com.eucalyptus.system;

import org.apache.log4j.spi.LoggingEvent;
import org.hibernate.exception.GenericJDBCException;

import com.eucalyptus.records.Logs;
import com.eucalyptus.system.log.EucaPatternLayout;

public class EucaLayout extends EucaPatternLayout {
	
    private static final String DEFAULT_LOG_PATTERN     = "%d{yyyy-MM-dd HH:mm:ss} %5.5p | %m%n";
	private static final String DEBUG_LOG_PATTERN       = "%d{yyyy-MM-dd HH:mm:ss} %5.5p %9.9i %-24.24c{1} | %m%n";
	private static final String EXTREME_LOG_PATTERN     = "%d{yyyy-MM-dd HH:mm:ss} %5.9p %9.9i %-24.24c{1} %-33.33f | %m%n";
	private String CONTINUATION = "%m%n";
	private EucaPatternLayout continuation = null;
	private final EucaPatternLayout extremeLayout;
	private final EucaPatternLayout debugLayout;
	
	public EucaLayout( ) {
		super( DEFAULT_LOG_PATTERN );
		this.debugLayout = new EucaPatternLayout(DEBUG_LOG_PATTERN);
		this.extremeLayout = new EucaPatternLayout(EXTREME_LOG_PATTERN);
	}

	private String mainFormat(LoggingEvent e) {
		if (Logs.isExtrrreeeme()) 
			return extremeLayout.format(e);
		else if (Logs.isDebug())
			return debugLayout.format(e);
		else 
			return super.format(e);
	}

	@Override
	public String format( LoggingEvent event ) {
		try {
			if( event.getThrowableInformation( ) != null ) {
				Throwable t = event.getThrowableInformation( ).getThrowable( );
				if( t != null && t instanceof GenericJDBCException ) {
					return "";
				}
			} else if ( event.getFQNOfLoggerClass( ).matches(".*JDBCExceptionReporter.*") ) {
				return "";
			}
			String renderedMessage = event.getRenderedMessage( );

			if(renderedMessage != null) {
				String[] messages = renderedMessage.split( "\n" );
				StringBuffer sb = new StringBuffer( );
				boolean con = false;
				for( int i = 0; i < messages.length; i++ ) {
					String substring= messages[i];
					LoggingEvent n = new LoggingEvent( event.getFQNOfLoggerClass( ), event.getLogger( ),
							event.getTimeStamp( ), event.getLevel( ), 
							substring, event.getThreadName( ), 
							event.getThrowableInformation( ), null, null, null );
					sb.append( (!con)?mainFormat( n ):continuation.format( n ) );
					if(continuation==null) {
						continuation = new EucaPatternLayout(sb.toString( ).split( "\\|" )[0].replaceAll( ".", " " )+"| "+CONTINUATION);
					}
					con = true;        
				}    
				return sb.toString( );
			}
		} catch ( Exception ex ) {
			ex.printStackTrace( );
		}
		return null;
	}

	@Override
	public boolean ignoresThrowable( ) {
		return true;
	}
}
