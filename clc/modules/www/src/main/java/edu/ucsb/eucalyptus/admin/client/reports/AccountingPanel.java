package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.ui.DockPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class AccountingPanel extends DockPanel implements Observer {
  private final AccountingControl  controller;
  private final ReportList         reportList;
  private final ReportDisplayPanel reportPanel;
  
  public AccountingPanel( AccountingControl controller ) {
    this.ensureDebugId( "AccountingPanel" );
    this.controller = controller;
    this.reportList = new ReportList( this.controller );
    this.reportPanel = new ReportDisplayPanel( this.controller );
  }
  
  public void update( ) {
    this.reportList.update( );
    this.reportPanel.update( );
  }
  
  public void redraw( ) {
    this.clear( );
    this.setSpacing( 8 );
    this.setHorizontalAlignment( DockPanel.ALIGN_CENTER );
    this.setVerticalAlignment( DockPanel.ALIGN_TOP );
    this.setStyleName( AccountingControl.RESOURCES.ROOT_PANEL_STYLE );
    this.add( this.reportList, DockPanel.WEST );
    this.setCellWidth( this.reportList, "15%" );
    this.add( this.reportPanel, DockPanel.CENTER );
    this.setCellWidth( this.reportPanel, "85%" );
    this.reportList.redraw( );
    this.reportPanel.redraw( );
    this.update( );
  }
  
}
