package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Entity;
import org.hibernate.annotations.NaturalId;
import com.eucalyptus.util.Classes;

public class MultiDatabasePropertyEntry extends AbstractConfigurableProperty implements ConfigurableProperty {
  private static Logger LOG = Logger.getLogger( MultiDatabasePropertyEntry.class );
  private Method        setIdentifier;
  private Field         identifierField;
  private String        identifierValue;
  private String        identifiedMethodName;
  
  public MultiDatabasePropertyEntry( Class definingClass, String entrySetName, Field field, Field identifierField, String description, String defaultValue,
                                     PropertyTypeParser typeParser,
                                     Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias, String identifierValue ) {
    super( definingClass, entrySetName, field, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias );
    this.identifierField = identifierField;
    this.identifiedMethodName = identifierField.getName( ).substring( 0, 1 ).toUpperCase( ) + identifierField.getName( ).substring( 1 );
    this.identifierValue = identifierValue;
  }
  
  private Method lookupSetIdentifierMethod( ) {
    try {
      Method setMethod = this.getDefiningClass( ).getMethod( "set" + this.identifiedMethodName, this.identifierField.getType( ) );
      setMethod.setAccessible( true );
      return setMethod;
    } catch ( Exception ex ) {
      throw new RuntimeException( "Failed to obtain reference to method for setting the identifier field: " + this.identifierField.getName( ) + "/"
                                  + this.identifiedMethodName + " in type " + this.getDefiningClass( ).getSimpleName( ) );
    }
  }
  
  protected Object getQueryObject( ) throws Exception {
    Object queryObject = super.getDefiningClass( ).newInstance( );
    try {
      setIdentifier = ( setIdentifier != null )
        ? setIdentifier
          : this.lookupSetIdentifierMethod( );
      setIdentifier.invoke( queryObject, identifierValue );
    } catch ( Exception e1 ) {
      try {
        this.lookupSetIdentifierMethod( ).invoke( queryObject, identifierValue );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        return ex.getMessage( );
      }
    }
    return queryObject;
  }
  
  public static class DatabasePropertyBuilder implements ConfigurablePropertyBuilder {
    
    @Override
    public ConfigurableProperty buildProperty( Class c, Field f ) throws ConfigurablePropertyException {
      if ( c.isAnnotationPresent( Entity.class ) &&
           !( ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class ) ).singleton( ) ) {
        ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
        Field identifierField = findIdentifierField( c );//GRZE:NOTE: had to refactor to also look in class hierarchy
        if ( identifierField == null ) {
          return null;
        }
        if ( f.isAnnotationPresent( ConfigurableField.class ) ) {
          LOG.trace( "Checking field: " + c.getName( ) + "." + f.getName( ) );
          ConfigurableField annote = f.getAnnotation( ConfigurableField.class );
          String fqPrefix = classAnnote.root( );
          String alias = classAnnote.alias( );
          String description = annote.description( );
          String defaultValue = annote.initial( );
          PropertyTypeParser p = PropertyTypeParser.get( f.getType( ) );
          try {
            if ( !Modifier.isStatic( f.getModifiers( ) ) && !f.isAnnotationPresent( Transient.class ) ) {
              ConfigurableProperty prop = new MultiDatabasePropertyEntry( c, fqPrefix, f, identifierField, description, defaultValue, p, annote.readonly( ),
                                                                          annote.displayName( ), annote.type( ), alias, null );
              return prop;
            }
          } catch ( Exception e ) {
            LOG.debug( e, e );
            return null;
          }
        }
      } else {
        return null;
      }
      return null;
    }

    private Field findIdentifierField( Class c ) throws SecurityException {
      for ( Class ancestor : Classes.classAncestors( c ) ) {
        for ( Field field : ancestor.getDeclaredFields( ) ) {
          if ( field.isAnnotationPresent( ConfigurableIdentifier.class ) ) {
            return field;
          }
        }
        for ( Field field : c.getFields( ) ) {
          if ( field.isAnnotationPresent( NaturalId.class ) ) {
            return field;
          }
        }
      }
      return null;
    }
    
  }
  
  public String getIdentifiedMethodName( ) {
    return this.identifiedMethodName;
  }
  
  public void setIdentifiedMethodName( String identifiedMethodName ) {
    this.identifiedMethodName = identifiedMethodName;
  }
  
  public void setIdentifierValue( String value ) {
    identifierValue = value;
  }
  
  @Override
  public String getEntrySetName( ) {
    if ( identifierValue != null )
      return identifierValue + "." + super.getEntrySetName( );
    else return super.getEntrySetName( );
  }
  
  @Override
  public String getQualifiedName( ) {
    if ( identifierValue != null )
      return identifierValue + "." + super.getQualifiedName( );
    else return super.getQualifiedName( );
  }
  
  public MultiDatabasePropertyEntry getClone( String idValue ) {
    return new MultiDatabasePropertyEntry( this.getDefiningClass( ), this.getEntrySetName( ), this.getField( ), identifierField, this.getDescription( ),
                                           this.getDefaultValue( ), this.getTypeParser( ), this.getReadOnly( ),
                                           this.getDisplayName( ), this.getWidgetType( ), this.getAlias( ), idValue );
  }
  
  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo( ConfigurableProperty that ) {
    return this.getQualifiedName( ) != null
      ? this.getQualifiedName( ).compareTo( that.getQualifiedName( ) )
        : ( that.getQualifiedName( ) == null
            ? 0
                : -1 );
  }
  
  @Override
  public int hashCode( ) {
    return getQualifiedName( ).hashCode( );
  }
  
  @Override
  public boolean equals( Object obj ) {
    if ( this == obj )
      return true;
    if ( obj == null )
      return false;
    if ( getClass( ) != obj.getClass( ) )
      return false;
    MultiDatabasePropertyEntry other = ( MultiDatabasePropertyEntry ) obj;
    return this.compareTo( other ) == 0;
  }
  
}
