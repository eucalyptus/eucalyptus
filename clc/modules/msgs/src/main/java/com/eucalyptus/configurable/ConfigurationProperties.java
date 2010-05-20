package com.eucalyptus.configurable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.eucalyptus.system.SubDirectory;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class ConfigurationProperties {
  private static Logger LOG = Logger.getLogger( ConfigurationProperties.class );

  private static Multimap<String,Class> fileToClassMap = Multimaps.newHashMultimap( );
  private static Multimap<Class,String> classToFieldMap = Multimaps.newHashMultimap( );
  private static Map<String,PropertyTypeParser> fieldTypeMap = Maps.newHashMap( );
    
  @SuppressWarnings( "deprecation" )
  public static void configure( String entrySetName ) {
    File propsFile = getPropertyFile( entrySetName );
    if( !propsFile.exists( ) ) {
      ConfigurationProperties.store( entrySetName );
    }
    Properties props = new Properties( );
    FileReader fileReader = null;
    try {
      fileReader = new FileReader( propsFile );
	  props.load( fileReader );
    } catch ( FileNotFoundException e ) {
      LOG.trace( e, e );
    } catch ( IOException e ) {
      LOG.trace( e, e );
    } finally {
      if( fileReader != null ) {
    	try {
          fileReader.close();
    	} catch(IOException e) {
          LOG.error(e);
    	}
      }
    }
    List<ConfigurableProperty> prefixProps = PropertyDirectory.getPropertyEntrySet( entrySetName );
    Map<String,String> properties = Maps.fromProperties( props );
    props.clear( );
    for( final ConfigurableProperty p : prefixProps ) {
      if( p instanceof StaticPropertyEntry ) {
        boolean hasProp = Iterables.any( properties.keySet( ), new Predicate<String>() {
          @Override
          public boolean apply( String arg0 ) {
            return p.getFieldName( ).equals( arg0.toLowerCase( ) );
          }
        } );
        if( hasProp ) {
          p.setValue( properties.get( p.getFieldName( ) ) );
        } else {
          properties.put( p.getFieldName( ), p.getValue( ) );
        }
      }
    }
    if( !properties.isEmpty( ) ) {
      props.putAll( properties );
      FileOutputStream fileOutputStream = null;
      try {
        fileOutputStream = new FileOutputStream( propsFile );
		props.save( fileOutputStream, PropertyDirectory.getEntrySetDescription( entrySetName ) );
      } catch ( FileNotFoundException e ) {
        LOG.warn( "Failed to save property set: " + entrySetName, e );
      } finally {
    	if ( fileOutputStream != null ) {
    	  try {
    	    fileOutputStream.close();
    	  } catch(IOException e) {
    		LOG.error(e);
    	  }
    	}
      }
    }
  }

  private static File getPropertyFile( String entrySetName ) {
    String propsFileName = SubDirectory.CONF + File.separator + entrySetName + ".properties";
    File propsFile = new File( propsFileName );
    return propsFile;
  }

  private static ConfigurableClass getAnnotation( Class c ) {
    ConfigurableClass a = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
    if( a == null ) {
      throw new RuntimeException( "Attempt to configure a class which does not declare itself Configurable: " + c.getName( ) );
    }
    return a;
  }
  
  public static void store( String entrySetName ) {
    File propsFile = getPropertyFile( entrySetName );
    Properties props = new Properties( );
    FileReader fileReader = null;
    try {
      fileReader = new FileReader( propsFile );
	  props.load( fileReader );
    } catch ( Exception e1 ) {
    } finally {
      if(fileReader != null) {
    	try {
          fileReader.close();
    	} catch(IOException e) {
          LOG.error(e);
    	}
      }
    }
    props.clear( );
    for( final ConfigurableProperty p : PropertyDirectory.getPropertyEntrySet( entrySetName ) ) {
      if( !( p instanceof SingletonDatabasePropertyEntry ) && !( p instanceof MultiDatabasePropertyEntry ) ) {
        props.setProperty( p.getFieldName( ), p.getValue( ) );
      }
    }
    if( !props.isEmpty( ) ) {
      FileWriter fileWriter = null;
      try {
        fileWriter = new FileWriter( propsFile );
		props.store( fileWriter, PropertyDirectory.getEntrySetDescription( entrySetName ) );
      } catch ( IOException e ) {
        LOG.warn( e, e );
      } finally {
    	if(fileWriter != null) {
    	  try { 
    	    fileWriter.close();
    	  } catch (IOException e) {
    	    LOG.error(e);
    	  }
    	}
      }
    }
  }
    
  public static void doConfiguration( ) {
    for( String entrySet : PropertyDirectory.getPropertyEntrySetNames( ) ) {
      ConfigurationProperties.configure( entrySet );
    }
  }
  
}
