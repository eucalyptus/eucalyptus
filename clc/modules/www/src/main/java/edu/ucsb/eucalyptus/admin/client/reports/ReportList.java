package edu.ucsb.eucalyptus.admin.client.reports;

import java.util.HashMap;
import java.util.Map;
import com.google.gwt.user.client.ui.DecoratedStackPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;
import edu.ucsb.eucalyptus.admin.client.util.XHTML;

public class ReportList extends DecoratedStackPanel implements Observer {
  private final AccountingControl    controller;
  private VerticalPanel              systemReports;
  private VerticalPanel              resourceReports;
  private VerticalPanel              zoneReports;
  private Map<String, VerticalPanel> groupMap;
  
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
    this.groupMap = new HashMap<String, VerticalPanel>( ) {{
      put( "system", ReportList.this.systemReports );
      put( "user", ReportList.this.resourceReports );
      put( "service", ReportList.this.zoneReports );
    }};
  }
  
  public void redraw( ) {
    this.clear( );
    this.add( this.systemReports, XHTML.headerWithImage( "System Events", AccountingControl.RESOURCES.systemReports( ),
                                                         AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
    this.add( this.resourceReports, XHTML.headerWithImage( "Users, Groups & Resources", AccountingControl.RESOURCES.resourceReports( ),
                                                           AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
    this.add( this.zoneReports, XHTML.headerWithImage( "Service Status & Logs", AccountingControl.RESOURCES.serviceReports( ),
                                                       AccountingControl.RESOURCES.REPORT_BAR_STYLE ), true );
  }
  
  @Override
  public void update( ) {
    for( final String group : this.controller.getGroupMap( ).keySet( ) ) {
      if( !this.groupMap.containsKey( group ) ) {
        this.groupMap.put( group, new VerticalPanel( ){
          {
            ensureDebugId( group + "Reports" );
          }
        } );
      }
      VerticalPanel groupPanel = this.groupMap.get( group );
      groupPanel.clear( );
      groupPanel.setStyleName( AccountingControl.RESOURCES.REPORT_BAR_STYLE );
      for ( ReportInfo info : this.controller.getGroupMap( ).get( group ) ) {
        if ( info.equals( this.controller.getCurrentReport( ) ) ) {
          info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_CURRENT_BUTTON );
        } else {
          info.getButton( ).setStyleName( AccountingControl.RESOURCES.ACCT_REPORT_BUTTON );
        }
        groupPanel.add( info.getButton( ) );
      }

    }
  }
  
}
