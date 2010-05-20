package com.eucalyptus.configurable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public interface ConfigurableProperty {
  
  public abstract String getFieldName( );
  
  public abstract Class getDefiningClass( );
  
  public abstract String getEntrySetName( );
  
  public abstract String getQualifiedName( );
  
  public abstract String getDescription( );
  
  public abstract String getDisplayName( );
  
  public abstract ConfigurableFieldType getWidgetType( );
  
  public abstract String getAlias();
  
  public abstract PropertyTypeParser getTypeParser( );
  
  public abstract String getDefaultValue( );
  
  public abstract String getValue( );
  
  public abstract String setValue( String s );
  
  public abstract void resetValue( );
  
}