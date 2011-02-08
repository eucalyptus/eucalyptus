package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.rpc.IsSerializable;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaButton;

public class ReportInfo implements IsSerializable {
  public static final ReportInfo      BOGUS  = new ReportInfo( "system", "System Log", "system", 0 );
  
  private transient AccountingControl controller;
  private transient EucaButton        button;
  private String                      group;
  
  private Integer                     length;
  private String                      name;
  private String                      fileName;
  private String                      component;
  private String                      clusterName;
  private String                      hostName;
  private Boolean                     remote = Boolean.FALSE;
  
  public ReportInfo( ) {
    this( "Loading", "Loading", "Loading", 0 );
    this.controller = null;
    this.button = null;
  }
  
  public ReportInfo( String group, String name, String fileName, Integer length ) {
    this.group = group;
    this.length = length;
    this.name = name;
    this.fileName = fileName;
    this.component = "";
    this.clusterName = "";
    this.hostName = "";
  }
  
  public ReportInfo( String group, String name, String fileName, Integer length, String service, String clusterName, String hostName ) {
    this( group, name, fileName, length );
    this.component = service;
    this.clusterName = clusterName;
    this.hostName = hostName;
    this.remote = Boolean.TRUE;
  }
  
  public String getUrl( ReportType type ) {
    return "/reports?name=" + this.controller.getCurrentFileName( ) + "&type=" + type.name( ).toLowerCase( ) + "&session=" + this.controller.getSessionid( )
           + "&page=" + this.controller.getCurrentPage( ) + "&flush=" + this.controller.getForceFlush( ) + "&start=" + this.controller.getStartMillis( )
           + "&end=" + this.controller.getEndMillis( ) + "&criterionId=" + this.controller.getCriterionInd() + "&groupById=" + this.controller.getGroupByInd()
           + ( this.remote ? "&component=" + this.component + "&cluster=" + this.clusterName + "&host=" + this.hostName: "" );
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
  
  public String getGroup( ) {
    return this.group;
  }
  
  public void setGroup( String group ) {
    this.group = group;
  }
  
  public void setLength( Integer length ) {
    this.length = length;
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

  public String getComponent( ) {
    return this.component;
  }

  public String getClusterName( ) {
    return this.clusterName;
  }

  public String getHostName( ) {
    return this.hostName;
  }

  public String getDisplayName( ) {
    return this.component + "@" + this.hostName;
  }
  
  public Boolean isRemote( ) {
    return this.remote;
  }
  
}
