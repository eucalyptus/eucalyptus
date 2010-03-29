package com.eucalyptus.configurable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javassist.Modifier;
import javax.persistence.Entity;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import com.eucalyptus.sysinfo.SubDirectory;
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
    try {
      props.load( new FileReader( propsFile ) );
    } catch ( FileNotFoundException e ) {
      LOG.debug( e, e );
    } catch ( IOException e ) {
      LOG.debug( e, e );
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
      try {
        props.save( new FileOutputStream( propsFile ), PropertyDirectory.getEntrySetDescription( entrySetName ) );
      } catch ( FileNotFoundException e ) {
        LOG.warn( "Failed to save property set: " + entrySetName, e );
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
    try {
      props.load( new FileReader( propsFile ) );
    } catch ( Exception e1 ) {
    }
    props.clear( );
    for( final ConfigurableProperty p : PropertyDirectory.getPropertyEntrySet( entrySetName ) ) {
      if( !( p instanceof SingletonDatabasePropertyEntry ) ) {
        props.setProperty( p.getFieldName( ), p.getValue( ) );
      }
    }
    if( !props.isEmpty( ) ) {
      try {
        props.store( new FileWriter( propsFile ), PropertyDirectory.getEntrySetDescription( entrySetName ) );
      } catch ( IOException e ) {
        LOG.warn( e, e );
      }
    }
  }
    
  public static void doConfiguration( ) {
    for( String entrySet : PropertyDirectory.getPropertyEntrySetNames( ) ) {
      ConfigurationProperties.configure( entrySet );
    }
  }
  
}
