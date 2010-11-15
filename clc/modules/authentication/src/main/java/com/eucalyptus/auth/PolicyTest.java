package com.eucalyptus.auth;

import java.io.FileInputStream;
import java.io.InputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.entities.StatementEntity;
import com.eucalyptus.auth.policy.PolicyParser;

public class PolicyTest {

  public static void main( String[] args ) throws Exception {
    if ( args.length < 1 ) {
      System.err.println( "Requires input policy file" );
      System.exit( 1 ); 
    }
    InputStream input = new FileInputStream( args[0] );
    
    String policy = readInputAsString( input );
    
    PolicyEntity parsed = PolicyParser.getInstance( ).parse( policy );
    
    printPolicy( parsed );
  }
  
  private static String readInputAsString( InputStream in ) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream( );
    
    byte[] buf = new byte[512];
    while ( in.read( buf ) >= 0 ) {
      baos.write( buf );
    }
    
    return new String( baos.toByteArray( ), "UTF-8" );
  }
  
  private static void printPolicy( PolicyEntity parsed ) {
    System.out.println( "Policy:\n" + parsed.getPolicyText( ) + "\n" + "Version = " + parsed.getPolicyVersion( ) );
    for ( StatementEntity statement : parsed.getStatements( ) ) {
      System.out.println( "Statement: " + statement.getSid( ) );
      for ( AuthorizationEntity auth : statement.getAuthorizations( ) ) {
        System.out.println( "Authorization: " + auth.getEffect( ) + " " + auth.getActionPattern( ) + " " + auth.isNotAction( ) + " "
                            + auth.getResourcePattern( ) + " " + auth.getResourceType( ) + " " + auth.isNotResource( ) );
      }
      for ( ConditionEntity cond : statement.getConditions( ) ) {
        System.out.println( "Condition: " + cond.getType( ) + " " + cond.getKey( ) + " " + cond.getValues( ) );
      }
    }
  }
  
}
