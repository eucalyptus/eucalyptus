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

package com.eucalyptus.bootstrap;

import org.apache.log4j.Logger;
import com.eucalyptus.util.Exceptions;

public class BootstrapException extends RuntimeException {
  private static Logger LOG = Logger.getLogger( BootstrapException.class );

  private BootstrapException( String message, Throwable cause ) {
    super( Bootstrap.getCurrentStage( ) + ": " + message, cause );
  }

  public BootstrapException( String message ) {
    super( Bootstrap.getCurrentStage( ) + ": " + message );
  }

  private BootstrapException( Throwable cause ) {
    super( Bootstrap.getCurrentStage( ) + ": " + cause );
  }
  
  public static BootstrapException throwError( String message ) {
    return error( message, null );
  }
  public static BootstrapException throwError( String message, Throwable t ) {
    return error( message, t );
  }
  public static BootstrapException error( String message, Throwable t ) {
    Bootstrap.Stage stage = Bootstrap.getCurrentStage( );
    BootstrapException ex = new BootstrapException( message );
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[3];
    if( t == null ) {
      Logger.getLogger( ste.getClassName( ) ).error( "Error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), ex );
    } else {
      Logger.getLogger( ste.getClassName( ) ).error( "Error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    }
    return ex;
  }

  public static BootstrapException throwFatal( String message ) {
    return fatal( message, null );
  }
  public static BootstrapException throwFatal( String message, Throwable t ) {
    return fatal( message, t );
  }
  private static BootstrapException fatal( String message, Throwable t ) {
    Bootstrap.Stage stage = Bootstrap.getCurrentStage( );
    BootstrapException ex = new BootstrapException( message, t );
    StackTraceElement ste = Thread.currentThread( ).getStackTrace( )[3];
    if( t == null ) {
      Logger.getLogger( ste.getClassName( ) ).fatal( "Fatal error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), ex );
    } else {
      Logger.getLogger( ste.getClassName( ) ).fatal( "Fatal error occured during bootstrap: " + ste.getClassName( ) + "." + ste.getMethodName( ) + ":" + ste.getLineNumber( ), t );
    }
    Exceptions.error( message, t );
    return ex;
  }

}
