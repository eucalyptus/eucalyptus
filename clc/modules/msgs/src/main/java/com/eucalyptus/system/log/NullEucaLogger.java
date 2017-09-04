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

import java.util.Enumeration;
import java.util.ResourceBundle;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;

public class NullEucaLogger extends Logger {
	public NullEucaLogger() {
		super("/dev/null");
	}

	@Override
	public boolean isTraceEnabled( ) {
		return false;
	}

	@Override
	public void trace( final Object message, final Throwable t ) {}

	@Override
	public void trace( final Object message ) {}

	@Override
	public synchronized void addAppender( final Appender newAppender ) {}

	@Override
	public void assertLog( final boolean assertion, final String msg ) {}

	@Override
	public void callAppenders( final LoggingEvent arg0 ) {}

	@Override
	public void debug( final Object message, final Throwable t ) {}

	@Override
	public void debug( final Object message ) {}

	@Override
	public void error( final Object message, final Throwable t ) {}

	@Override
	public void error( final Object message ) {}

	@Override
	public void fatal( final Object message, final Throwable t ) {}

	@Override
	public void fatal( final Object message ) {}

	@Override
	protected void forcedLog( final String fqcn, final Priority level, final Object message, final Throwable t ) {}

	@Override
	public boolean getAdditivity( ) {
		return false;
	}

	@Override
	public synchronized Enumeration getAllAppenders( ) {
		return super.getAllAppenders( );
	}

	@Override
	public synchronized Appender getAppender( final String name ) {
		return super.getAppender( name );
	}

	@Override
	public Priority getChainedPriority( ) {
		return super.getChainedPriority( );
	}

	@Override
	public Level getEffectiveLevel( ) {
		return Level.OFF;
	}

	@Override
	public LoggerRepository getHierarchy( ) {
		return super.getHierarchy( );
	}

	@Override
	public LoggerRepository getLoggerRepository( ) {
		return super.getLoggerRepository( );
	}

	@Override
	public ResourceBundle getResourceBundle( ) {
		return super.getResourceBundle( );
	}

	@Override
	protected String getResourceBundleString( final String arg0 ) {
		return super.getResourceBundleString( arg0 );
	}

	@Override
	public void info( final Object message, final Throwable t ) {}

	@Override
	public void info( final Object message ) {}

	@Override
	public boolean isAttached( final Appender appender ) {
		return super.isAttached( appender );
	}

	@Override
	public boolean isDebugEnabled( ) {
		return false;
	}

	@Override
	public boolean isEnabledFor( final Priority level ) {
		return false;
	}

	@Override
	public boolean isInfoEnabled( ) {
		return false;
	}

	@Override
	public void l7dlog( final Priority arg0, final String arg1, final Object[] arg2, final Throwable arg3 ) {}

	@Override
	public void l7dlog( final Priority arg0, final String arg1, final Throwable arg2 ) {}

	@Override
	public void log( final Priority priority, final Object message, final Throwable t ) {}

	@Override
	public void log( final Priority priority, final Object message ) {}

	@Override
	public void log( final String callerFQCN, final Priority level, final Object message, final Throwable t ) {}

	@Override
	public synchronized void removeAllAppenders( ) {}

	@Override
	public synchronized void removeAppender( final Appender appender ) {}

	@Override
	public synchronized void removeAppender( final String name ) {}

	@Override
	public void setAdditivity( final boolean additive ) {}

	@Override
	public void setLevel( final Level level ) {}

	@Override
	public void setPriority( final Priority priority ) {}

	@Override
	public void setResourceBundle( final ResourceBundle bundle ) {}

	@Override
	public void warn( final Object message, final Throwable t ) {}

	@Override
	public void warn( final Object message ) {}

}

