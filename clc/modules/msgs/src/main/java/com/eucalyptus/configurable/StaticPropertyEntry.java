package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;
import com.eucalyptus.event.PassiveEventListener;

public class StaticPropertyEntry extends AbstractConfigurableProperty {
  static Logger LOG = Logger.getLogger( StaticPropertyEntry.class );
  private Field         field;
  public StaticPropertyEntry( Class definingClass, String entrySetName, Field field, String description, String defaultValue, PropertyTypeParser typeParser, Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias, PassiveEventListener changeListener ) {
    super( definingClass, entrySetName, field.getName( ), defaultValue, description, typeParser, readOnly, displayName, widgetType, alias, changeListener );
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
      this.fireChange( );
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
      return annote.root( ) + "." + f.getName( ).toLowerCase( );
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
        String alias = classAnnote.alias();
        PropertyTypeParser p = PropertyTypeParser.get( field.getType( ) );
        ConfigurableProperty entry = null;
        Class<? extends PassiveEventListener> changeListenerClass = annote.changeListener( );
        PassiveEventListener changeListener;
        if( !changeListenerClass.equals( NoopEventListener.class ) ) {
          try {
            changeListener = changeListenerClass.newInstance( );
          } catch ( Throwable e ) {
            changeListener = NoopEventListener.NOOP;
          }          
        } else {
          changeListener = NoopEventListener.NOOP; 
        }
        int modifiers = field.getModifiers( );
        if ( Modifier.isPublic( modifiers ) && Modifier.isStatic( modifiers ) ) {
          entry = new StaticPropertyEntry( c, fqPrefix, field, description, defaultValue, p, annote.readonly( ), annote.displayName(), annote.type(), alias, changeListener );
          entry.setValue( defaultValue );
          return entry;
        } 
      } 
      return null;
    }
  }
}
