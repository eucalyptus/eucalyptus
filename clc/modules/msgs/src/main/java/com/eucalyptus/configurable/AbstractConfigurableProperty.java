package com.eucalyptus.configurable;

import org.apache.log4j.Logger;
import com.eucalyptus.configurable.PropertyDirectory.NoopEventListener;
import com.eucalyptus.event.PassiveEventListener;

public abstract class AbstractConfigurableProperty implements ConfigurableProperty {

  private static Logger LOG = Logger.getLogger( AbstractConfigurableProperty.class );
  protected String entrySetName;
  private String fieldName;
  protected String qualifiedName;
  protected String description;
  protected PropertyTypeParser typeParser;
  protected String defaultValue;
  protected Class definingClass;
  protected Boolean readOnly;
  protected String displayName;
  protected ConfigurableFieldType widgetType;
  protected String alias;
  private PassiveEventListener<ConfigurableProperty> changeListener;
  
  public AbstractConfigurableProperty( Class definingClass, String entrySetName, String propertyName, String defaultValue, String description, PropertyTypeParser typeParser, Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias, PassiveEventListener changeListener ) {
    this( definingClass, entrySetName, propertyName, defaultValue, description, typeParser, readOnly, displayName, widgetType, alias );
    this.changeListener = changeListener;
  }
  public AbstractConfigurableProperty( Class definingClass, String entrySetName, String propertyName, String defaultValue, String description, PropertyTypeParser typeParser, Boolean readOnly, String displayName, ConfigurableFieldType widgetType, String alias ) {
    this.definingClass = definingClass;
    this.entrySetName = entrySetName.toLowerCase( );
    this.fieldName = propertyName.toLowerCase( );
    this.qualifiedName = this.entrySetName + "." + this.fieldName;
    this.description = description;
    this.typeParser = typeParser;
    this.defaultValue = defaultValue;
    this.readOnly = readOnly;
    this.displayName = displayName;
    this.widgetType = widgetType;
    this.alias = alias;
    this.changeListener = NoopEventListener.NOOP;
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
  
  public String getDisplayName( ) {
	return this.displayName;
  }
  
  public ConfigurableFieldType getWidgetType( ) {
	return this.widgetType;
  }
  
  public String getAlias() {
	return this.alias;
  }
  
  public void fireChange() {
    if( !NoopEventListener.class.equals( this.changeListener.getClass( ) ) ) {
      this.changeListener.firingEvent( this );
    }
  }
}