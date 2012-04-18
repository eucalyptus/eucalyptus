package com.eucalyptus.auth.ldap;

import java.io.FileInputStream;
import java.io.InputStream;
import javax.security.auth.login.LoginContext;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class LicParserTest {
  
  public static void main( String[] args ) throws Exception {
    if ( args.length < 1 ) {
      System.err.println( "Requires input lic file" );
      System.exit( 1 ); 
    }
    InputStream input = new FileInputStream( args[0] );
    
    String licText = readInputAsString( input );
    
    LdapIntegrationConfiguration lic = LicParser.getInstance( ).parse( licText );

    System.out.println( lic );
  }
  
  private static String readInputAsString( InputStream in ) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream( );
    
    byte[] buf = new byte[512];
    int nRead = 0;
    while ( ( nRead = in.read( buf ) ) >= 0 ) {
      baos.write( buf, 0, nRead );
    }
  
    return new String( baos.toByteArray( ), "UTF-8" );
  }
  
}
