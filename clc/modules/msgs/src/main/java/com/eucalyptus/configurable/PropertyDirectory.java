package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.event.PassiveEventListener;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;

public class PropertyDirectory {
  private static Logger                                 LOG         = Logger.getLogger( PropertyDirectory.class );
  private static Map<String, ConfigurableProperty>      fqMap       = Maps.newHashMap( );
  private static Multimap<String, ConfigurableProperty> fqPrefixMap = Multimaps.newHashMultimap( );
  private static Map<String, ConfigurableProperty>      fqPendingMap       = Maps.newHashMap( );
 private static Multimap<String, ConfigurableProperty> fqPendingPrefixMap = Multimaps.newHashMultimap( );

  private static List<ConfigurablePropertyBuilder>      builders    = Lists.newArrayList( new StaticPropertyEntry.StaticPropertyBuilder( ),
                                                                                          new SingletonDatabasePropertyEntry.DatabasePropertyBuilder( ),
                                                                                          new MultiDatabasePropertyEntry.DatabasePropertyBuilder( )); //FIXME: make this dynamic kkthx.
                                                                                                                                                  
  public static class NoopEventListener extends PassiveEventListener<ConfigurableProperty> {
    public static NoopEventListener NOOP = new NoopEventListener( );
    @Override
    public void firingEvent( ConfigurableProperty t ) {}
  }
  @SuppressWarnings( { "unchecked" } )
  public static ConfigurableProperty buildPropertyEntry( Class c, Field field ) {
    for ( ConfigurablePropertyBuilder b : builders ) {
      try {
        ConfigurableProperty prop = null;
        try {
          prop = b.buildProperty( c, field );
        } catch ( ConfigurablePropertyException e ) {
          throw e;
        } catch ( Throwable t ) {
          LOG.error( "Failed to prepare configurable field: " + c.getCanonicalName( ) + "." + field.getName( ) );
          System.exit( 1 );
        }
        if ( prop != null ) {
          ConfigurableClass configurableAnnot = (ConfigurableClass) c.getAnnotation(ConfigurableClass.class);
          if ( configurableAnnot.deferred() ) {
        	if ( !fqPendingMap.containsKey( prop.getQualifiedName( ) ) ) {
              fqPendingMap.put( prop.getQualifiedName( ), prop );
              fqPendingPrefixMap.put( prop.getEntrySetName( ), prop );
              return prop;
            }    
          } else {
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
 
  public static List<ConfigurableProperty> getPropertyEntrySet( String prefix, String alias ) {
	List<ConfigurableProperty> props = Lists.newArrayList( );
	for ( ConfigurableProperty fq : fqPrefixMap.get( prefix ) ) {
	if(fq.getAlias().equals(alias))
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
  
  public static List<ConfigurableProperty> getPendingPropertyEntrySet( String prefix ) {
	List<ConfigurableProperty> props = Lists.newArrayList( );
	for ( ConfigurableProperty fq : fqPendingPrefixMap.get( prefix ) ) {
	  props.add( fq );
	}
	return props;
  }

  public static String getEntrySetDescription( String entrySetName ) {
    return "Temporary description";
  }
  
  public static List<ComponentProperty> getComponentPropertySet( String prefix ) {
	  List<ComponentProperty> componentProps = Lists.newArrayList();
	  List<ConfigurableProperty> props = getPropertyEntrySet( prefix );
	  for (ConfigurableProperty prop : props) {
		  componentProps.add(new ComponentProperty(prop.getWidgetType().toString(), prop.getDisplayName(), prop.getValue(), prop.getQualifiedName()));
	  }
	  return componentProps;
  }
  
  public static void addProperty(ConfigurableProperty prop) {
  	if ( !fqMap.containsKey( prop.getQualifiedName( ) ) ) {
  	  fqMap.put( prop.getQualifiedName( ), prop );
      fqPrefixMap.put( prop.getEntrySetName( ), prop );
    }
  }
  
  public static void removeProperty(ConfigurableProperty prop) {
    if ( fqMap.containsKey( prop.getQualifiedName( ) ) ) {
      fqMap.remove( prop.getQualifiedName( ) );
	  fqPrefixMap.remove( prop.getEntrySetName( ), prop );
	}	 
  }

  public static List<ComponentProperty> getComponentPropertySet(String prefix, String alias) {
	List<ComponentProperty> componentProps = Lists.newArrayList();
	List<ConfigurableProperty> props = getPropertyEntrySet( prefix, alias );
	for (ConfigurableProperty prop : props) {
	 componentProps.add(new ComponentProperty(prop.getWidgetType().toString(), prop.getDisplayName(), prop.getValue(), prop.getQualifiedName()));
	}
	return componentProps;
  }
}
