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
