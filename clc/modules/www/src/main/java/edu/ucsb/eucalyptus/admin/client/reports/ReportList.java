package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.ui.DecoratedStackPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;
import edu.ucsb.eucalyptus.admin.client.util.XHTML;

public class ReportList extends DecoratedStackPanel implements Observer {
  private final AccountingControl controller;
  private VerticalPanel           reports;
  
  public ReportList( AccountingControl controller ) {
    this.ensureDebugId( "ReportList" );
    this.controller = controller;
    this.reports = new VerticalPanel( );
  }
  
  public void redraw( ) {
    this.clear( );
    this.setWidth( "200px" );
    this.add( this.reports,  XHTML.headerWithImage( "REPORTS", AccountingControl.RESOURCES.test( ), AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
  }
  
  @Override
  public void update( ) {
    this.reports.clear( );
    this.reports.setStyleName( AccountingControl.RESOURCES.REPORT_BAR_STYLE );
    for ( ReportInfo info : ReportList.this.controller.getReports( ) ) {
      this.reports.add( info.getButton( ) );
    }
  }
  
}
