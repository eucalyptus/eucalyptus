package com.eucalyptus.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Hmacs;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.ServiceProcess;
import com.google.common.collect.Lists;

public class SlapdResource {
  protected static final String CONFIG_FILE = SubDirectory.LDAP.toString( ) + File.separator + "etc" + File.separator + "slapd.conf";
  private static Logger         LOG         = Logger.getLogger( SlapdResource.class );
  private ServiceProcess        process;
  
  public SlapdResource( ) {}
  
  public void start( String name, Map<String, String> dbOpts ) {
    List<String> cmd = Lists.newArrayList( "./bin/slapd.32" );
    for ( String key : dbOpts.keySet( ) ) {
      cmd.add( key );
      if ( dbOpts.get( key ) != null ) {
        cmd.add( dbOpts.get( key ) );
      }
    }
    LOG.info( "Preparing to start slapd: " + cmd.toString( ) );
    this.process = ServiceProcess.exec( name, SubDirectory.LDAP.getFile( ), cmd.toArray( new String[] {} ), null );
    Runtime.getRuntime( ).addShutdownHook( new Thread( ) {
      @Override
      public void run( ) {
        LOG.info( "Shutting down managed service resource: " + this.getClass( ).getCanonicalName( ) );
        SlapdResource.this.shutdown( );
      }
      
    } );
  }
  
  public void initialize( ) throws IOException {
    if ( !new File( CONFIG_FILE ).exists( ) && new File( CONFIG_FILE ).getParentFile( ).mkdirs( ) ) {
      File conf = new File( SlapdResource.CONFIG_FILE );
      FileOutputStream confOut = new FileOutputStream( conf );
      try {
        confOut.write( String.format( SlapdResource.initialConfig, SubDirectory.LDAP.toString( ), Hmacs.generateSystemSignature( ) ).getBytes( ) );
        confOut.flush( );
      } finally {
        confOut.close( );
      }
      this.expandResourceJarInner( SubDirectory.LDAP.getFile( ), "data.jar" );
    }
  }
  
  public void shutdown( ) {
    if ( process != null ) {
      this.process.kill( );
    }
  }
  
  //  TLSVerifyClient
  //  TLSCertificateKeyFile
  //  TLSCACertificateFile
  //  TLSCertificateFile
  //  password-hash {MD5}
  //  logfile /path/to/ldap/log/file
  //  # file must exist prior to starting OpenLDAP
  //  touch /path/to/ldap/log/file
  //#  chown ldap:ldap /path/to/ldap/log/file
  private static String initialConfig = "require authc\n" + "include    %1$s/etc/schema/core.schema\n" + "include   %1$s/etc/schema/cosine.schema\n"
                                        + "include   %1$s/etc/schema/inetorgperson.schema\n" + "include   %1$s/etc/schema/nis.schema\n"
                                        + "include   %1$s/etc/schema/misc.schema\n" + "pidfile   %1$s/var/run/slapd.pid\n"
                                        + "argsfile  %1$s/var/run/slapd.args\n" + "database  bdb\n" + "suffix    \"dc=eucalyptus,dc=com\"\n"
                                        + "rootdn    \"cn=EucalyptusManager,dc=eucalyptus,dc=com\"\n" + "rootpw    %2$s\n"
                                        + "directory %1$s/var/openldap-data\n" + "index objectClass eq\n";
  
  private void expandResourceJarInner( File outputDir, String name ) throws IOException {
    InputStream is = this.getClass( ).getResourceAsStream( name );//TODO: change the way the class is looked up
    is = ( is == null ) ? this.getClass( ).getResourceAsStream( "/" + name ) : is;
    if ( is == null ) {
      throw new MissingResourceException( "Resource '" + name + "' not found", this.getClass( ).getName( ), name );
    }
    JarInputStream jis = new JarInputStream( is );
    try {
      JarEntry entry = null;
      while ( ( entry = jis.getNextJarEntry( ) ) != null ) {
        File file = new File( outputDir, entry.getName( ) );
        if ( !file.exists( ) ) {
          if ( entry.isDirectory( ) ) {
            LOG.info( "Creating directory: " + file.getCanonicalPath( ) );
            file.mkdirs( );
          } else if ( file.getParentFile( ).exists( ) || file.getParentFile( ).mkdirs( ) ) {
            LOG.info( "Creating entry:     " + file.getCanonicalPath( ) );
            FileOutputStream to = new FileOutputStream( file );
            try {
              byte[] buf = new byte[8192];
              for ( int i = 0; ( i = jis.read( buf ) ) != -1; to.write( buf, 0, i ) );
              to.flush( );
            } catch ( IOException e ) {
              LOG.error( e, e );
              throw e;
            } catch ( Exception e ) {
              LOG.error( e, e );
              throw new RuntimeException( e );
            } finally {
              to.close( );
            }
            if ( entry.getName( ).matches( "([^/]|\\./)*bin/[^/]*" ) ) {
              LOG.info( "Marking executable: " + file.getCanonicalPath( ) );
              file.setExecutable( true );
            }
          } else {
            LOG.warn( "Failed to create directory: " + file.getParentFile( ).getAbsolutePath( ) + " for resource: " + entry.getName( ) );
          }
        }
      } while ( entry != null );
    } finally {
      jis.close( );
      is.close( );
    }
  }
  
  public boolean isRunning( ) {
    return true;
  }
  
  public boolean isReady( ) {
    return true;
  }
  
}
