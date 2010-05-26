package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaButton;

public class ReportInfo {
  private AccountingControl parent;
  private Integer           length;
  private EucaButton        button;
  private String            name;
  private String            fileName;
  
  public ReportInfo( ) {}
  
  public ReportInfo( String name, String fileName, Integer length ) {
    super( );
    this.length = length;
    this.name = name;
    this.fileName = fileName;
  }

  public String getUrl( ReportType type ) {
    return "/reports?name=" + this.parent.getCurrentReport( ).fileName + "&type=" + type.name( ).toLowerCase( ) + "&session="
           + this.parent.getSessionid( ) + "&page=" + this.parent.getCurrentPage( );
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

  public AccountingControl getParent( ) {
    return this.parent;
  }

  public void setParent( AccountingControl parent ) {
    this.parent = parent;
    this.button = new EucaButton( this.getName( ), "View " + this.getName( ) + " Report.", AccountingControl.ACCT_REPORT_BUTTON, new ClickHandler( ) {
      @Override
      public void onClick( ClickEvent arg0 ) {
        ReportInfo.this.parent.setCurrentReport( ReportInfo.this );
      }
    } );
  }

  public EucaButton getButton( ) {
    return this.button;
  }


}
