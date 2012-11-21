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

import java.util.Enumeration;
import java.util.ResourceBundle;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;

public class NullEucaLogger extends EucaLogger {
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
		return super.getEffectiveLevel( );
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
		return super.isDebugEnabled( );
	}

	@Override
	public boolean isEnabledFor( final Priority level ) {
		return super.isEnabledFor( level );
	}

	@Override
	public boolean isInfoEnabled( ) {
		return super.isInfoEnabled( );
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

