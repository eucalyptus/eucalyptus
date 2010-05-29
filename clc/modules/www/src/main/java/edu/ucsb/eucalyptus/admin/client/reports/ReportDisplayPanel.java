package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.EucaImageButton;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class ReportDisplayPanel extends VerticalPanel implements Observer {
  private final AccountingControl controller;
  private HorizontalPanel         actionBar;
  private final Frame             report;
  private final HorizontalPanel   exportPanel;
  private final HorizontalPanel   pagingPanel;
  private final HorizontalPanel   topPanel;
  private final DateRange         dateRange;
  
  ReportDisplayPanel( AccountingControl controller ) {
    this.ensureDebugId( "ReportDisplayPanel" );
    this.setStyleName( AccountingControl.RESOURCES.DISPLAY_PANEL_STYLE );
    this.controller = controller;
    this.topPanel = new HorizontalPanel( );
    this.topPanel.setStyleName( AccountingControl.RESOURCES.REPORT_BAR_STYLE );
    this.pagingPanel = new HorizontalPanel( );
    this.pagingPanel.setHorizontalAlignment( ALIGN_LEFT );
    this.dateRange = new DateRange( controller );
    this.exportPanel = new HorizontalPanel( );
    this.exportPanel.setHorizontalAlignment( ALIGN_RIGHT );
    this.report = new Frame( );
    this.report.setStyleName( AccountingControl.RESOURCES.REPORT_FRAME_STYLE );
  }
  
  public void update( ) {
    if ( this.controller.isReady( ) ) {
      this.report.setUrl( this.controller.getCurrentUrl( ReportType.HTML ) );
      if ( this.actionBar != null ) {
        for ( Widget w : this.actionBar ) {
          if ( w instanceof Observer ) {
            ( ( Observer ) w ).update( );
          }
        }
      }
    }
  }
  
  public void redraw( ) {
    this.clear( );
    this.add( this.makeTopPanel( ) );
    this.add( this.report );
    this.update( );
  }
  
  private HorizontalPanel makeTopPanel( ) {
    this.topPanel.clear( );
//  this.topPanel.add( this.dateRange );
//    this.dateRange.redraw( );
    this.topPanel.add( this.makePagingPanel( ) );
    this.topPanel.add( this.makeExportPanel( ) );
    return this.topPanel;
  }
  
  private HorizontalPanel makeExportPanel( ) {
    this.exportPanel.clear( );
    for ( final ReportType r : ReportType.values( ) ) {
      EucaImageButton button = r.makeImageButton( ReportDisplayPanel.this.controller );
      this.exportPanel.add( button );
    }
    return this.exportPanel;
  }
    
  private HorizontalPanel makePagingPanel( ) {
    this.pagingPanel.clear( );
    for ( final ReportAction a : ReportAction.values( ) ) {
      this.pagingPanel.add( a.makeImageButton( ReportDisplayPanel.this.controller ) );
    }
    return this.pagingPanel;
  }
  
}
