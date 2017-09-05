/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
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
