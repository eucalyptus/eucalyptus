package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaImageButton;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class AccountingPanel extends DockPanel implements Observer {
  private final AccountingControl  controller;
  private final ReportList         reportList;
  private final ReportDisplayPanel reportPanel;
  private HorizontalPanel pagingPanel;
  private HorizontalPanel exportPanel;
  
  public AccountingPanel( AccountingControl controller ) {
    this.ensureDebugId( "AccountingPanel" );
    this.controller = controller;
    this.pagingPanel = new HorizontalPanel( );    
    this.pagingPanel.setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
    this.exportPanel = new HorizontalPanel( );    
    this.exportPanel.setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
    this.reportList = new ReportList( this.controller );
    this.reportPanel = new ReportDisplayPanel( this.controller );
  }
  
  public void update( ) {
    this.updatePagingPanel( );
    this.updateExportPanel( );
    this.reportList.update( );
    this.reportPanel.update( );
  }
  
  private void updateExportPanel( ) {
    this.exportPanel.clear( );
    for ( final ReportType r : ReportType.values( ) ) {
      EucaImageButton button = r.makeImageButton( this.controller );
      this.exportPanel.add( button );
    }
  }

  private void updatePagingPanel( ) {
    this.pagingPanel.clear( );
    for ( final ReportAction a : ReportAction.values( ) ) {
      this.pagingPanel.add( a.makeImageButton( this.controller ) );
    }
  }

  
  public void redraw( ) {
    this.clear( );
    this.setSpacing( 8 );
    this.setHorizontalAlignment( DockPanel.ALIGN_CENTER );
    this.setVerticalAlignment( DockPanel.ALIGN_TOP );
    this.setStyleName( AccountingControl.RESOURCES.ROOT_PANEL_STYLE );
    VerticalPanel leftPane;
    leftPane = new VerticalPanel( ) {{
      this.add( AccountingPanel.this.pagingPanel );
      this.add( AccountingPanel.this.exportPanel );
      this.add( AccountingPanel.this.reportList);      
    }};
    this.add( leftPane, DockPanel.WEST );
    this.setCellWidth( leftPane, "15%" );
    this.add( this.reportPanel, DockPanel.CENTER );
    this.setCellWidth( this.reportPanel, "85%" );
    this.reportList.redraw( );
    this.reportPanel.redraw( );
    this.update( );
  }
  
}
