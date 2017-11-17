/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.util.dns;

import java.util.stream.Stream;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 *
 */
public class SetResponsesTest {

  private static final Logger logger = Logger.getLogger( SetResponsesTest.class );

  @Test
  public void testOfType( ) {
    Stream.of( SetResponses.SetResponseType.values( ) )
        .map( SetResponses::ofType )
        .forEach( logger::info );
  }

  @Test
  public void testNewInstance( ) {
    Stream.of( SetResponses.SetResponseType.values( ) )
        .map( type -> SetResponses.newInstance( type, null ) )
        .forEach( logger::info );
  }

  @Test
  public void testAddRrSet( ) {
    Stream.of( SetResponses.SetResponseType.values( ) )
        .map( type -> SetResponses.newInstance( type, null ) )
        .map( response -> { SetResponses.addRRset( response, null ); return response; } )
        .forEach( logger::info );
  }
}
