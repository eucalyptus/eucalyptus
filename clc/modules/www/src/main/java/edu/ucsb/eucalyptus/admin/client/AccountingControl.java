package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.reports.ReportAction;
import edu.ucsb.eucalyptus.admin.client.reports.ReportInfo;
import edu.ucsb.eucalyptus.admin.client.reports.ReportType;

public class AccountingControl implements ContentControl {
  public static final String     ACCT_REPORT_BUTTON = "acct-Button-Report";
  public static final String     ACCT_ACTION_BUTTON = "acct-Button-Action";
  public static ClickHandler     DO_NOTHING         = new ClickHandler( ) {
                                                      @Override
                                                      public void onClick( ClickEvent arg0 ) {}
                                                    };
  private String                 sessionId;
  private final List<ReportInfo> reports            = new ArrayList<ReportInfo>( );
  private Integer                currentPage;
  private Boolean                forceFlush         = Boolean.FALSE;
  private ReportInfo             currentReport      = null;
  private VerticalPanel          rootPanel;
  private HorizontalPanel        reportBar;
  private HorizontalPanel        actionBar;
  private Frame                  report;
  
  @SuppressWarnings( "deprecation" )
  public AccountingControl( String sessionId ) {
    this.sessionId = sessionId;
    this.rootPanel.addStyleName( "acct-root" );
    this.rootPanel.add( new Label( "Loading report list..." ) );
    this.currentPage = 0;
    EucalyptusWebBackend.App.getInstance( ).getReports( AccountingControl.this.sessionId, new AsyncCallback<List<ReportInfo>>( ) {
      public void onSuccess( List<ReportInfo> result ) {
        AccountingControl.this.reports.clear( );
        AccountingControl.this.rootPanel.clear( );
        AccountingControl.this.makeReportBar( result );
        AccountingControl.this.makeActionBar( );
        AccountingControl.this.rootPanel.add( AccountingControl.this.report = new Frame( ) );
        AccountingControl.this.display( );
      }
      
      public void onFailure( final Throwable caught ) {
        AccountingControl.this.rootPanel.clear( );
        AccountingControl.this.rootPanel.add( new Label( "Loading report list failed because of: " + caught.getMessage( ) ) );
      }
    } );
    
  }
  
  protected HorizontalPanel makeActionBar( ) {
    HorizontalPanel actionBar = new HorizontalPanel( );
    for ( final ReportAction a : ReportAction.values( ) ) {
      actionBar.add( a.makeImageButton( this ) );
    }
    for ( final ReportType r : ReportType.values( ) ) {
      actionBar.add( r.makeImageButton( this ) );
    }
    AccountingControl.this.actionBar = actionBar;
    AccountingControl.this.rootPanel.add( AccountingControl.this.actionBar );
    return actionBar;
  }
  
  private HorizontalPanel makeReportBar( List<ReportInfo> result ) {
    HorizontalPanel reportBar = new HorizontalPanel( );
    reportBar.setStyleName( "acct-report-bar" );
    for ( ReportInfo info : result ) {
      info.setParent( AccountingControl.this );
      if ( this.currentReport == null ) {
        this.currentReport = info;
      }
      this.reports.add( info );
      reportBar.add( info.getButton( ) );
    }
    AccountingControl.this.reportBar = reportBar;
    AccountingControl.this.rootPanel.add( AccountingControl.this.reportBar );
    AccountingControl.this.currentReport = this.reports.get( 0 );
    return reportBar;
  }
  
  public Integer setCurrentPage( String currentPage ) {
    Integer newPage;
    try {
      newPage = new Integer( currentPage );
      if ( newPage >= 0 && newPage < this.currentReport.getLength( ) ) {
        return this.setCurrentPage( newPage - 1 );
      } else if ( newPage < 0 ) {
        return this.setCurrentPage( 0 );
      } else {
        return this.setCurrentPage( this.currentReport.getLength( ) );
      }
    } catch ( NumberFormatException e ) {
      return this.currentPage;
    }
  }
  
  public Integer setCurrentPage( Integer currentPage ) {
    this.currentPage = currentPage;
    this.display( );
    return this.currentPage;
  }
  
  @Override
  public Widget getRootWidget( ) {
    return this.rootPanel;
  }
  
  @Override
  public void display( ) {
    AccountingControl.this.report.setUrl( AccountingControl.this.currentReport.getUrl( ReportType.HTML ) );
  }
  
  public void setCurrentReport( ReportInfo currentReport ) {
    this.currentPage = 0;
    this.currentReport = currentReport;
    this.display( );
  }
  
  public ReportInfo getCurrentReport( ) {
    return currentReport;
  }
  
  public String getSessionid( ) {
    return this.sessionId;
  }
  
  public Integer getCurrentPage( ) {
    return this.currentPage;
  }
  
  public Boolean getForceFlush( ) {
    return forceFlush;
  }
  
}
