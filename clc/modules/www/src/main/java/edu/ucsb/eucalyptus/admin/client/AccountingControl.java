package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class AccountingControl implements ContentControl {
  
  private VerticalPanel   root;
  private HorizontalPanel buttonBar;
  private EucaButton      pdfButton;
  private EucaButton      csvButton;
  private EucaButton      xlsButton;
  
  private Frame           report;
  
  public AccountingControl( String sessionId ) {
    this.root = new VerticalPanel( );
    this.root.addStyleName( "FIXME1" );
    this.buttonBar = new HorizontalPanel( );
    this.pdfButton = new EucaButton( "PDFs are awesome", "Download this report as a PDF.", new ClickHandler( ) {
      @Override
      public void onClick( ClickEvent clickevent ) {
        Window.Location.replace( "/reports?type=pdf" );
      }
    } );
    this.csvButton = new EucaButton( "CSVs are too!1!", "Download this report as a csv.", new ClickHandler( ) {
      @Override
      public void onClick( ClickEvent clickevent ) {
        Window.Location.replace( "/reports?type=csv" );
      }
    } );
    this.xlsButton = new EucaButton( "XLS not so much.", "Download this report as a xls.", new ClickHandler( ) {
      @Override
      public void onClick( ClickEvent clickevent ) {
        Window.Location.replace( "/reports?type=xls" );
      }
    } );
    this.buttonBar.add( this.pdfButton );
    this.buttonBar.add( this.csvButton );
    this.buttonBar.add( this.xlsButton );
    this.root.add( buttonBar );
    this.report = new Frame( );
    this.root.add( this.report );
  }
  
  @Override
  public Widget getRootWidget( ) {
    return this.root;
  }
  
  @Override
  public void display( ) {
    this.report.setUrl( "/reports" );
  }
}
