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

