package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class ReportDisplayPanel extends VerticalPanel implements Observer {
  private final AccountingControl controller;
  private HorizontalPanel         actionBar;
  private final Frame             report;
  
  ReportDisplayPanel( AccountingControl controller ) {
    this.ensureDebugId( "ReportDisplayPanel" );
    this.setStyleName( AccountingControl.RESOURCES.DISPLAY_PANEL_STYLE );
    this.controller = controller;
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
    this.add( this.report );
    this.update( );
  }
  
}
