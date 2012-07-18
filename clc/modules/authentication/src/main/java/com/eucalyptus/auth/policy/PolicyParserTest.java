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

package com.eucalyptus.auth.policy;

import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.StatementEntity;

public class PolicyParserTest {

  public static void main( String[] args ) throws Exception {
    if ( args.length < 1 ) {
      System.err.println( "Requires input policy file" );
      System.exit( 1 ); 
    }
    InputStream input = new FileInputStream( args[0] );
    
    String policy = readInputAsString( input );
        
    PolicyEntity parsed = PolicyParser.getInstance( ).parse( policy );
    
    printPolicy( parsed );

    input.close();
  }
  
  private static String readInputAsString( InputStream in ) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream( );
    
    byte[] buf = new byte[512];
    int nRead = 0;
    while ( ( nRead = in.read( buf ) ) >= 0 ) {
      baos.write( buf, 0, nRead );
    }
    
    String string = new String( baos.toByteArray( ), "UTF-8" );
    baos.close();
    return string;
  }
  
  private static void printPolicy( PolicyEntity parsed ) {
    System.out.println( "Policy:\n" + parsed.getText( ) + "\n" + "Version = " + parsed.getPolicyVersion( ) );
    for ( StatementEntity statement : parsed.getStatements( ) ) {
      System.out.println( "Statement: " + statement.getSid( ) );
      for ( AuthorizationEntity auth : statement.getAuthorizations( ) ) {
        System.out.println( "Authorization: " + auth );
      }
      for ( ConditionEntity cond : statement.getConditions( ) ) {
        System.out.println( "Condition: " + cond );
      }
    }
  }
  
}
