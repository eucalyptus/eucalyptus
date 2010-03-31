package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.PersistenceContext;
import org.apache.log4j.Logger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class PropertyDirectory {
  private static Logger                                 LOG         = Logger.getLogger( PropertyDirectory.class );
  private static Map<String, ConfigurableProperty>      fqMap       = Maps.newHashMap( );
  private static Multimap<String, ConfigurableProperty> fqPrefixMap = Multimaps.newHashMultimap( );
  
  private static List<ConfigurablePropertyBuilder>      builders    = Lists.newArrayList( new StaticPropertyEntry.StaticPropertyBuilder( ),
                                                                                          new SingletonDatabasePropertyEntry.DatabasePropertyBuilder( ) ); //FIXME: make this dynamic kkthx.
                                                                                                                                                  
  @SuppressWarnings( { "unchecked" } )
  public static ConfigurableProperty buildPropertyEntry( Class c, Field field ) {
    for ( ConfigurablePropertyBuilder b : builders ) {
      try {
        ConfigurableProperty prop = b.buildProperty( c, field );
        if ( prop != null ) {
          if ( !fqMap.containsKey( prop.getQualifiedName( ) ) ) {
            fqMap.put( prop.getQualifiedName( ), prop );
            fqPrefixMap.put( prop.getEntrySetName( ), prop );
            return prop;
          } else {
            RuntimeException r = new RuntimeException( "Duplicate configurable field in same config file: \n" + "-> "
                                                       + fqMap.get( prop.getQualifiedName( ) ).getDefiningClass( ).getCanonicalName( ) + "." + field.getName( )
                                                       + "\n" + "-> " + c.getCanonicalName( ) + "." + field.getName( ) + "\n" );
            LOG.fatal( r, r );
            System.exit( 1 );
            throw r;
          }
        }
      } catch ( ConfigurablePropertyException e ) {
        LOG.debug( e, e );
      }
    }
    return null;
  }
  
  public static List<String> getPropertyEntrySetNames( ) {
    return Lists.newArrayList( fqPrefixMap.keySet( ) );
  }
  
  public static List<ConfigurableProperty> getPropertyEntrySet( ) {
    List<ConfigurableProperty> props = Lists.newArrayList( );
    for ( String fqPrefix : fqPrefixMap.keySet( ) ) {
      props.addAll( getPropertyEntrySet( fqPrefix ) );
    }
    return props;
  }
  
  public static List<ConfigurableProperty> getPropertyEntrySet( String prefix ) {
    List<ConfigurableProperty> props = Lists.newArrayList( );
    for ( ConfigurableProperty fq : fqPrefixMap.get( prefix ) ) {
      props.add( fq );
    }
    return props;
  }
  
  public static ConfigurableProperty getPropertyEntry( String fq ) throws IllegalAccessException {
    if ( !fqMap.containsKey( fq ) ) {
      throw new IllegalAccessException( "No such property: " + fq );
    } else {
      return fqMap.get( fq );
    }
  }
  
  public static String getEntrySetDescription( String entrySetName ) {
    return "Temporary description";
  }
  
  public static List<ComponentProperty> getComponentPropertySet( String prefix ) {
	  List<ComponentProperty> componentProps = Lists.newArrayList();
	  List<ConfigurableProperty> props = getPropertyEntrySet( prefix );
	  for (ConfigurableProperty prop : props) {
		  componentProps.add(new ComponentProperty(prop.getWidgetType(), prop.getDisplayName(), prop.getValue(), prop.getQualifiedName()));
	  }
	  return componentProps;
  }
}
