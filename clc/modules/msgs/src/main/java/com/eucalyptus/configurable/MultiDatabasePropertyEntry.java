package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import javax.persistence.Transient;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Entity;

public class MultiDatabasePropertyEntry extends AbstractConfigurableProperty implements ConfigurableProperty {
  private static Logger LOG = Logger.getLogger( MultiDatabasePropertyEntry.class );
  protected final Method      setIdentifier;
  protected Field       identifierField;
  protected String      identifierValue;
  
  public MultiDatabasePropertyEntry( Class definingClass, String entrySetName, Field field, Field identifierField, String description, String defaultValue,
                                     PropertyTypeParser typeParser,
                                     Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias, String identifierValue ) {
    super( definingClass, entrySetName, field, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias );
    this.identifierField = identifierField;
    String identifiedMethodName = identifierField.getName( ).substring( 0, 1 ).toUpperCase( ) + identifierField.getName( ).substring( 1 );
    this.setIdentifier = lookupSetIdentifierMethod( identifierField, identifiedMethodName );
    this.identifierValue = identifierValue;
  }

  private Method lookupSetIdentifierMethod( Field identifierField, String identifiedMethodName ) {
    try {
      return this.getDefiningClass( ).getMethod( identifiedMethodName, identifierField.getType( ) );
    } catch ( Exception ex ) {
      throw new RuntimeException( "Failed to obtain reference to method for setting the identifier field: " + this.identifierField.getName( ) + " in type " + this.getDefiningClass( ).getSimpleName( ) );
    }
  }
  
  protected Object getQueryObject( ) throws Exception {
    Object queryObject = super.getDefiningClass( ).newInstance( );
    try {
      setIdentifier.invoke( queryObject, identifierValue );
    } catch ( Exception e1 ) {
      try {
        this.lookupSetIdentifierMethod( identifierField, identifierValue ).invoke( queryObject, identifierValue );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
        return ex.getMessage( );
      }
    }
    return queryObject;
  }

  public static class DatabasePropertyBuilder implements ConfigurablePropertyBuilder {
    private static Logger LOG = Logger.getLogger( MultiDatabasePropertyEntry.DatabasePropertyBuilder.class );
    
    @Override
    public ConfigurableProperty buildProperty( Class c, Field f ) throws ConfigurablePropertyException {
      if ( c.isAnnotationPresent( Entity.class ) &&
           !( ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class ) ).singleton( ) ) {
        ConfigurableClass classAnnote = ( ConfigurableClass ) c.getAnnotation( ConfigurableClass.class );
        Field identifierField = null;
        for ( Field field : c.getDeclaredFields( ) ) {
          if ( field.isAnnotationPresent( ConfigurableIdentifier.class ) ) {
            identifierField = field;
            break;
          }
        }
        if ( identifierField == null ) {
          return null;
        }
        if ( f.isAnnotationPresent( ConfigurableField.class ) ) {
          LOG.debug( "Checking field: " + c.getName( ) + "." + f.getName( ) );
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
          } catch ( Throwable e ) {
            LOG.debug( e, e );
            return null;
          }
        }
      } else {
        return null;
      }
      return null;
    }
    
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
  
  public MultiDatabasePropertyEntry getClone( String identifierValue ) {
    return new MultiDatabasePropertyEntry( this.getDefiningClass( ), this.getEntrySetName( ), this.getField( ), identifierField, this.getDescription( ), this.getDefaultValue( ), this.getTypeParser( ), this.getReadOnly( ), 
                                           this.getDisplayName( ), this.getWidgetType( ), this.getAlias( ), identifierValue );
  }
}
