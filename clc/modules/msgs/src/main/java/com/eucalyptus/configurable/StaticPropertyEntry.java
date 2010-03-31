package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;

public class StaticPropertyEntry extends AbstractConfigurableProperty {
  static Logger LOG = Logger.getLogger( StaticPropertyEntry.class );
  private Field         field;
  public StaticPropertyEntry( Class definingClass, String entrySetName, Field field, String description, String defaultValue, PropertyTypeParser typeParser, Boolean readOnly, String displayName, String widgetType ) {
    super( definingClass, entrySetName, field.getName( ), defaultValue, description, typeParser, readOnly, displayName, widgetType );
    this.field = field;
  }
  public Field getField( ) {
    return this.field;
  }
  @Override
  public String getValue( ) {
    try {
      return ""+this.field.get( null );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      return super.getDefaultValue();
    }
  }
  @Override
  public String setValue( String s ) {
    try {
      Object o = super.getTypeParser( ).parse( s );
      this.field.set( null, o );
      LOG.info( "Set configurable property: " + super.getQualifiedName( ) + " to " + s );
    } catch ( Throwable t ) {
      LOG.warn( "Failed to set property: " + super.getQualifiedName( ) + " because of " + t.getMessage( ) );
      LOG.debug( t, t );
    }
    return this.getValue( );
  }

  public static class StaticPropertyBuilder implements ConfigurablePropertyBuilder {
    private static String qualifiedName( Class c, Field f ) {
      ConfigurableClass annote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
      return annote.alias( ) + "." + f.getName( ).toLowerCase( );
    }

    @Override
    public ConfigurableProperty buildProperty( Class c, Field field ) throws ConfigurablePropertyException {
      if( c.isAnnotationPresent( ConfigurableClass.class ) && field.isAnnotationPresent( ConfigurableField.class ) ) {
        ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
        ConfigurableField annote = ( ConfigurableField ) field.getAnnotation( ConfigurableField.class );
        String description = annote.description( );
        String defaultValue = annote.initial( );
        String fq = qualifiedName( c, field );
        String fqPrefix = fq.replaceAll( "\\..*", "" );
        PropertyTypeParser p = PropertyTypeParser.get( field.getType( ) );
        ConfigurableProperty entry = null;
        int modifiers = field.getModifiers( );
        if ( Modifier.isPublic( modifiers ) && Modifier.isStatic( modifiers ) ) {
          entry = new StaticPropertyEntry( c, fqPrefix, field, description, defaultValue, p, annote.readonly( ), annote.displayName(), annote.type().toString() );
          entry.setValue( defaultValue );
          return entry;
        } 
      } 
      return null;
    }
  }
}
