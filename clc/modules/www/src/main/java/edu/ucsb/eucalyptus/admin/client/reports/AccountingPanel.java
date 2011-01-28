package edu.ucsb.eucalyptus.admin.client.reports;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import edu.ucsb.eucalyptus.admin.client.AccountingControl;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class AccountingPanel extends DockPanel implements Observer {
  private final AccountingControl  controller;
  private final ReportList         reportList;
  private final ReportDisplayPanel reportPanel;
//  private HorizontalPanel          pagingPanel;
  private HorizontalPanel          exportPanel;
  private CriteriaPickerPanel      criteriaPickerPanel;
  private DateRange                dateRange;
  
  public AccountingPanel( AccountingControl controller ) {
    this.ensureDebugId( "AccountingPanel" );
    this.setStyleName( "acct-AccountingPanel" );
    this.controller = controller;
//    this.pagingPanel = new HorizontalPanel( ) {
//      {
//        ensureDebugId( "pagingPanel" );
//        setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
//        setVerticalAlignment( HorizontalPanel.ALIGN_MIDDLE );
//        setStyleName( "acct-ReportControl" );
//      }
//    };
    this.dateRange = new DateRange( controller ) {
      {
        ensureDebugId( "dateRange" );
        setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
        setVerticalAlignment( HorizontalPanel.ALIGN_MIDDLE );
        setStyleName( "acct-ReportControl" );
      }
    };
    this.exportPanel = new HorizontalPanel( ) {
      {
        ensureDebugId( "exportPanel" );
        setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
        setVerticalAlignment( HorizontalPanel.ALIGN_MIDDLE );
        setStyleName( "acct-ReportControl" );
      }
    };
    this.criteriaPickerPanel = new CriteriaPickerPanel( controller ) {
        {
          ensureDebugId( "dateRange" );
          setHorizontalAlignment( HorizontalPanel.ALIGN_CENTER );
          setVerticalAlignment( HorizontalPanel.ALIGN_MIDDLE );
          setStyleName( "acct-ReportControl" );
        }
      };
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
      IconButton button = r.makeImageButton( this.controller );
      this.exportPanel.add( button );
    }
  }
  
  private void updatePagingPanel( ) {
//    this.pagingPanel.clear( );
//    for ( final ReportAction a : ReportAction.values( ) ) {
//      this.pagingPanel.add( a.makeImageButton( this.controller ) );
//    }
  }
  
  public void redraw( ) {
    this.clear( );
    this.setSpacing( 0 );
    this.setHorizontalAlignment( DockPanel.ALIGN_LEFT );
    this.setVerticalAlignment( DockPanel.ALIGN_TOP );
    this.setStyleName( AccountingControl.RESOURCES.ROOT_PANEL_STYLE );
    VerticalPanel leftPane = new VerticalPanel( ) {
      {
//        setVerticalAlignment( VerticalPanel.ALIGN_TOP );
//        add( new HTML( "Change Page" ) {{
//          setStyleName( "acct-ReportControlHeader" );
//        }} );
//        add( AccountingPanel.this.pagingPanel );
        add( new HTML( "Export Report" ) {{
          setStyleName( "acct-ReportControlHeader" );
        }} );
        add( AccountingPanel.this.exportPanel );
        add( new HTML( "Change Time Period" ) {{
          setStyleName( "acct-ReportControlHeader" );
        }} );
        add( AccountingPanel.this.dateRange );
        add( new HTML( "Change Report Criteria" ) {{
            setStyleName( "acct-ReportControlHeader" );
          }} );
        add( AccountingPanel.this.criteriaPickerPanel );
        add( AccountingPanel.this.reportList );
        setCellHeight( AccountingPanel.this.reportList, "100%" );
      }
    };
    this.dateRange.redraw( );
    this.add( leftPane, DockPanel.WEST );
    this.setCellWidth( leftPane, "15%" );
    this.setCellHeight( leftPane, "100%" );
    this.add( this.reportPanel, DockPanel.CENTER );
    this.setCellWidth( this.reportPanel, "85%" );
    this.reportList.redraw( );
    this.reportPanel.redraw( );
    this.criteriaPickerPanel.redraw();
    this.update( );
  }
  
}
