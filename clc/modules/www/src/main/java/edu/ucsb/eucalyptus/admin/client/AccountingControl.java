package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.reports.ReportAction;
import edu.ucsb.eucalyptus.admin.client.reports.ReportInfo;
import edu.ucsb.eucalyptus.admin.client.reports.ReportType;

public class AccountingControl implements ContentControl {
  public static final String ACCT_REPORT_BUTTON = "acct-Button-Report";
  public static final String ACCT_ACTION_BUTTON = "acct-Button-Action";
  private String             sessionId;
  private ReportInfo         currentReport      = null;
  
  private final List<ReportInfo> reports = new ArrayList<ReportInfo>( );
  
  
  private VerticalPanel   root;
  private HorizontalPanel reportBar;
  private HorizontalPanel buttonBar;
  private Frame           report;
  private TextBox         currentPageText = new TextBox( );
  private Integer         currentPage;
  
  public AccountingControl( String sessionId ) {
    this.sessionId = sessionId;
    this.root = new VerticalPanel( );
    this.root.addStyleName( "acct-root" );
    this.root.add( new Label( "Loading report list..." ) );
    this.currentPage = 0;
    this.currentPageText.addValueChangeHandler( new ValueChangeHandler<String>( ) {
      @Override
      public void onValueChange( ValueChangeEvent<String> event ) {
        String newValue = event.getValue( );
        try {
          Integer newPage = new Integer( newValue );
          AccountingControl.this.setCurrentPage( newPage );
        } catch ( NumberFormatException e ) {}
      }
    } );
    EucalyptusWebBackend.App.getInstance( ).getReports( AccountingControl.this.sessionId, new AsyncCallback<List<ReportInfo>>( ) {
      public void onSuccess( List<ReportInfo> result ) {
        AccountingControl.this.reports.clear( );
        AccountingControl.this.root.clear( );
        AccountingControl.this.makeReportBar( result );
        AccountingControl.this.makeActionBar( );
        AccountingControl.this.root.add( AccountingControl.this.report = new Frame( ) );
        AccountingControl.this.display( );
      }
      
      public void onFailure( final Throwable caught ) {
        AccountingControl.this.root.clear( );
        AccountingControl.this.root.add( new Label( "Loading report list failed because of: " + caught.getMessage( ) ) );
      }
    } );
    
  }
  
  protected HorizontalPanel makeActionBar( ) {
    HorizontalPanel actionBar = new HorizontalPanel( );
    for ( final ReportAction a : ReportAction.values( ) ) {
      actionBar.add( a.makeImageButton( this ) );
    }
    AccountingControl.this.buttonBar.add( AccountingControl.this.currentPageText );
    for ( final ReportType r : ReportType.values( ) ) {
      actionBar.add( r.makeImageButton( this ) );
    }
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
    AccountingControl.this.root.add( AccountingControl.this.reportBar = reportBar );
    AccountingControl.this.currentReport = null;
    return reportBar;
  }
  
  public Integer setCurrentPage( Integer currentPage ) {
    return this.currentPage = currentPage;
  }
  
  @Override
  public Widget getRootWidget( ) {
    return this.root;
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
  
}
