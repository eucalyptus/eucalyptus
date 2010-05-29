package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.ui.DecoratedStackPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;
import edu.ucsb.eucalyptus.admin.client.util.XHTML;

public class ReportList extends DecoratedStackPanel implements Observer {
  private final AccountingControl controller;
  private VerticalPanel           systemReports;
  private VerticalPanel           resourceReports;
  private VerticalPanel           zoneReports;
  private HorizontalPanel pagingPanel;
  private HorizontalPanel exportPanel;
  
  public ReportList( AccountingControl controller ) {
    this.ensureDebugId( "ReportList" );
    this.controller = controller;
    this.systemReports = new VerticalPanel( ) {
      {
        ensureDebugId( "systemReports" );
      }
    };
    this.resourceReports = new VerticalPanel( ) {
      {
        ensureDebugId( "resourceReports" );
      }
    };
    this.zoneReports = new VerticalPanel( ) {
      {
        ensureDebugId( "zoneReports" );
      }
    };
  }
  
  public void redraw( ) {
    this.clear( );
    this.setWidth( "200px" );
    this.add( this.exportPanel );
    this.add( this.pagingPanel );
    this.add( this.systemReports, XHTML.headerWithImage( "System Events", AccountingControl.RESOURCES.systemReports( ), AccountingControl.RESOURCES.REPORT_BAR_STYLE ),
              true );
    this.add( this.resourceReports, XHTML.headerWithImage( "Users, Groups & Resources", AccountingControl.RESOURCES.resourceReports( ),
                                                           AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
    this.add( this.zoneReports, XHTML.headerWithImage( "Service Status & Logs", AccountingControl.RESOURCES.serviceReports( ),
                                                         AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
  }

  
  @Override
  public void update( ) {
    this.updateSystemReports( );
    this.updateZoneReports( );
    this.updateResourceReports( );
  }
  
  private void updateZoneReports( ) {
    this.zoneReports.clear( );
    this.zoneReports.setStyleName( AccountingControl.RESOURCES.REPORT_BAR_STYLE );
    for ( ReportInfo info : ReportList.this.controller.getZoneReports( ) ) {
      if( info.equals( this.controller.getCurrentReport( ) ) ) {
        info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_CURRENT_BUTTON );
      } else {
        info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_BUTTON );
      }
      this.zoneReports.add( info.getButton( ) );
    }
  }
  
  private void updateResourceReports( ) {
    this.resourceReports.clear( );
    this.resourceReports.setStyleName( AccountingControl.RESOURCES.REPORT_BAR_STYLE );
    for ( ReportInfo info : ReportList.this.controller.getResourceReports( ) ) {
      if( info.equals( this.controller.getCurrentReport( ) ) ) {
        info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_CURRENT_BUTTON );
      } else {
        info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_BUTTON );
      }
      this.resourceReports.add( info.getButton( ) );
    }
  }
  
  private void updateSystemReports( ) {
    this.systemReports.clear( );
    this.systemReports.setStyleName( AccountingControl.RESOURCES.REPORT_BAR_STYLE );
    for ( ReportInfo info : ReportList.this.controller.getSystemReports( ) ) {
      if( info.equals( this.controller.getCurrentReport( ) ) ) {
        info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_CURRENT_BUTTON );
      } else {
        info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_BUTTON );
      }
      this.systemReports.add( info.getButton( ) );
    }
  }
  
}
