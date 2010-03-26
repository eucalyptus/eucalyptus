package com.eucalyptus.configurable;

import org.apache.log4j.Logger;

public abstract class AbstractConfigurableProperty implements ConfigurableProperty {

  private static Logger LOG = Logger.getLogger( AbstractConfigurableProperty.class );
  private String entrySetName;
  private String fieldName;
  private String qualifiedName;
  private String description;
  private PropertyTypeParser typeParser;
  private String defaultValue;
  private Class definingClass;
  private Boolean readOnly;

  public AbstractConfigurableProperty( Class definingClass, String entrySetName, String propertyName, String defaultValue, String description, PropertyTypeParser typeParser, Boolean readOnly ) {
    this.definingClass = definingClass;
    this.entrySetName = entrySetName.toLowerCase( );
    this.fieldName = propertyName.toLowerCase( );
    this.qualifiedName = this.entrySetName + "." + this.fieldName;
    this.description = description;
    this.typeParser = typeParser;
    this.defaultValue = defaultValue;
    this.readOnly = readOnly;
  }

  public String getFieldName( ) {
    return this.fieldName;
  }

  public String getEntrySetName( ) {
    return this.entrySetName;
  }

  public String getQualifiedName( ) {
    return this.qualifiedName;
  }

  public String getDescription( ) {
    return this.description;
  }

  public PropertyTypeParser getTypeParser( ) {
    return this.typeParser;
  }

  public String getDefaultValue( ) {
    return this.defaultValue;
  }

  public abstract String getValue( );

  public abstract String setValue( String s );

  public void resetValue( ) {
    this.setValue( this.defaultValue );
  }

  public Class getDefiningClass( ) {
    return this.definingClass;
  }
  
}