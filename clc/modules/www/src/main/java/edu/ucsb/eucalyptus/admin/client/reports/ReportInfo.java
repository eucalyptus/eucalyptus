package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.IsSerializable;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaButton;

public class ReportInfo implements IsSerializable {
  private static final ReportInfo BOGUS = new ReportInfo( "System Log", "system-log", 0 );

  private transient AccountingControl controller;
  private transient EucaButton        button;
  private Integer           length;
  private String            name;
  private String            fileName;
  
  public ReportInfo( ) {
    this( "Loading", "Loading", 0 );
    this.controller = null;
    this.button = null;
  }
  
  public ReportInfo( String name, String fileName, Integer length ) {
    this.length = length;
    this.name = name;
    this.fileName = fileName;
  }

  public String getUrl( ReportType type ) {
    if( this.controller != null ) {
      return "/reports?name=" + this.controller.getCurrentFileName( ) + "&type=" + type.name( ).toLowerCase( ) + "&session="
           + this.controller.getSessionid( ) + "&page=" + this.controller.getCurrentPage( ) + "&flush=" + this.controller.getForceFlush();
    } else {
      return BOGUS.getUrl( type );
    }
  }
  
  public Integer getLength( ) {
    return this.length;
  }

  public String getName( ) {
    return this.name;
  }

  public String getFileName( ) {
    return this.fileName;
  }

  public void setParent( AccountingControl parent ) {
    this.controller = parent;
    this.button = new EucaButton( this.getName( ), "View " + this.getName( ) + " Report.", AccountingControl.RESOURCES.ACCT_REPORT_BUTTON, new ClickHandler( ) {
      @Override
      public void onClick( ClickEvent arg0 ) {
        ReportInfo.this.controller.setCurrentReport( ReportInfo.this );
      }
    } );
  }

  public EucaButton getButton( ) {
    return this.button;
  }

  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = 1;
    result = prime * result + ( ( this.name == null ) ? 0 : this.name.hashCode( ) );
    return result;
  }

  @Override
  public boolean equals( Object obj ) {
    if ( this == obj ) return true;
    if ( obj == null ) return false;
    if ( getClass( ) != obj.getClass( ) ) return false;
    ReportInfo other = ( ReportInfo ) obj;
    if ( this.name == null ) {
      if ( other.name != null ) return false;
    } else if ( !this.name.equals( other.name ) ) return false;
    return true;
  }

}
