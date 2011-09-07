package com.eucalyptus.webui.client.service;

import java.io.Serializable;
import com.google.gwt.view.client.ProvidesKey;

public class QuickLink implements Serializable {

  private static final long serialVersionUID = 1L;

  public static final ProvidesKey<QuickLink> KEY_PROVIDER = new ProvidesKey<QuickLink>( ) {

    @Override
    public Object getKey( QuickLink item ) {
      if ( item != null ) {
        return item.getName( );
      }
      return null;
    }
    
  };
  
  private String name;
  private String desc;
  private String image;
  private String query;
  
  public QuickLink( ) {
  }
  
  public QuickLink( String name, String desc, String image, String query ) {
    this.setName( name );
    this.setDesc( desc );
    this.setImage( image );
    this.setQuery( query );
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getName( ) {
    return name;
  }

  public void setDesc( String desc ) {
    this.desc = desc;
  }

  public String getDesc( ) {
    return desc;
  }

  public void setImage( String image ) {
    this.image = image;
  }

  public String getImage( ) {
    return image;
  }

  public void setQuery( String query ) {
    this.query = query;
  }

  public String getQuery( ) {
    return query;
  }
  
}
