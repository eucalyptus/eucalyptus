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

import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class EucaLoggingOutputStream extends OutputStream {
	private static final int DEFAULT_BUFFER_LENGTH = 2048;
	private boolean          hasBeenClosed         = false;
	private byte[]           buf;
	private int              count;
	private int              curBufLength;
	private final Logger     log;
	private final Level      level;

	public EucaLoggingOutputStream( final Logger log, final Level level ) throws IllegalArgumentException {
		if ( ( log == null ) || ( level == null ) ) {
			throw new IllegalArgumentException( "Logger or log level must be not null" );
		}
		this.log = log;
		this.level = level;
		this.curBufLength = DEFAULT_BUFFER_LENGTH;
		this.buf = new byte[this.curBufLength];
		this.count = 0;
	}

	public void write( final int b ) throws IOException {
		if ( this.hasBeenClosed ) {
			throw new IOException( "The stream has been closed." );
		}
		// don't log nulls
		if ( b == 0 ) {
			return;
		}
		// would this be writing past the buffer?
		if ( this.count == this.curBufLength ) {
			// grow the buffer
			final int newBufLength = this.curBufLength +
					DEFAULT_BUFFER_LENGTH;
			final byte[] newBuf = new byte[newBufLength];
			System.arraycopy( this.buf, 0, newBuf, 0, this.curBufLength );
			this.buf = newBuf;
			this.curBufLength = newBufLength;
		}

		this.buf[this.count] = ( byte ) b;
		this.count++;
	}

	public void flush( ) {
		if ( this.count == 0 ) {
			return;
		}
		final byte[] bytes = new byte[this.count];
		System.arraycopy( this.buf, 0, bytes, 0, this.count );
		final String str = new String( bytes );
		this.log.log( this.level, str );
		this.count = 0;
	}

	public void close( ) {
		this.flush( );
		this.hasBeenClosed = true;
	}
}

