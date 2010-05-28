package edu.ucsb.eucalyptus.admin.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.ucsb.eucalyptus.admin.client.reports.AccountingPanel;
import edu.ucsb.eucalyptus.admin.client.reports.ReportInfo;
import edu.ucsb.eucalyptus.admin.client.reports.ReportType;
import edu.ucsb.eucalyptus.admin.client.util.Buttons;
import edu.ucsb.eucalyptus.admin.client.util.Observer;

public class AccountingControl extends VerticalPanel implements ContentControl, Observer {
  public final static ResourceBundle RESOURCES = GWT.create( ResourceBundle.class );
  
  public interface ResourceBundle extends ClientBundle {
    public final static String TAB_ROOT_STYLE     = "acct-root";
    public final static String ROOT_PANEL_STYLE   = "acct-AccountingPanel";
    public final static String DISPLAY_PANEL_STYLE   = "acct-ReportDisplay";
    public final static String LIST_PANEL_STYLE   = "acct-ReportList";
    public final static String BUTTON_STYLE       = "acct-Button ";
    public final static String STACK_HEADER_STYLE = "acct-StackPanelHeader";
    public final static String REPORT_BAR_STYLE   = "acct-report-bar";
    public final static String REPORT_FRAME_STYLE     = "acct-Frame";
    
    @NotStrict
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/accounting.css" )
    public CssResource css( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/go-previous.png" )
    public ImageResource test( );
    
    @Source( "edu/ucsb/eucalyptus/admin/public/themes/active/img/go-next.png" )
    public ImageResource test2( );
  }
  
  private final String           sessionId;
  private final List<ReportInfo> reports       = new ArrayList<ReportInfo>( );
  private final AccountingPanel  accountingPanel;
  private Integer                currentPage   = 0;
  private Boolean                ready         = Boolean.FALSE;
  private Boolean                forceFlush    = Boolean.FALSE;
  private ReportInfo             currentReport = null;
  private EucaButton             errorButton   = null;
  public final ReportInfo bogus; 
  public AccountingControl( String sessionId ) {
    this.ensureDebugId( "AccountingControl" );
    RESOURCES.css( ).ensureInjected( );
    GWT.setUncaughtExceptionHandler( new GWT.UncaughtExceptionHandler( ) {
      @Override
      public void onUncaughtException( Throwable arg0 ) {
        AccountingControl.this.displayError( arg0 );
      }
    });
    this.sessionId = sessionId;
    this.accountingPanel = new AccountingPanel( this );
    this.errorButton = Buttons.HIDDEN;
    this.bogus = new ReportInfo( "System Log", "system-log", 0 ) {{
      setParent( AccountingControl.this );
    }};    
    this.currentReport = this.bogus;
    EucalyptusWebBackend.App.getInstance( ).getReports( this.getSessionid( ), new AsyncCallback<List<ReportInfo>>( ) {
      @Override
      public void onSuccess( List<ReportInfo> result ) {
        AccountingControl.this.setReports( result );
      }
      
      @Override
      public void onFailure( Throwable arg0 ) {
        AccountingControl.this.displayError( arg0 );
      }
    } );
  }
  
  protected void displayError( Throwable arg0 ) {
    this.errorButton = Buttons.errorButton( arg0 );
    this.redraw( );
  }

  protected void setReports( List<ReportInfo> result ) {
    this.reports.clear( );
    this.reports.addAll( result );
    if( !this.reports.isEmpty( ) ) {
      for( ReportInfo r : this.reports ) {
        r.setParent( this );
      }
      this.setCurrentReport( this.reports.get( 0 ) );
    } else {
      this.setCurrentReport( this.bogus );
    }
    this.redraw( );
  }
  
  @Override
  public Widget getRootWidget( ) {
    return this;
  }
  
  @Override
  public void update( ) {
    this.accountingPanel.update( );
  }

  @Override
  public void display( ) {
    this.update( );
  }

  @Override
  public void redraw( ) {
    this.clear( );
    this.addStyleName( RESOURCES.ROOT_PANEL_STYLE );
    this.add( this.errorButton );
    this.add( this.accountingPanel );
    this.accountingPanel.redraw( );
    this.display( );
  }
  

  public Boolean isReady( ) {
    return this.currentReport != null;
  }
  
  public void setCurrentReport( ReportInfo newReport ) {
    if( newReport != null && !newReport.equals( this.currentReport ) ) {
      this.currentPage = 0;
      this.currentReport = newReport;
      this.display( );
    }
  }
  
  public ReportInfo getCurrentReport( ) {
    return this.currentReport;
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
  
  public List<ReportInfo> getReports( ) {
    return this.reports;
  }
  
  public Integer changePage( Integer delta ) {
    if( this.currentPage + delta < 0 ) {
      this.currentPage = 0;
    } else if( this.currentReport != null && (this.currentPage >= this.currentReport.getLength( ))) {
      this.currentPage = this.currentReport.getLength( );
    } else {
      this.currentPage += delta;
    }
    this.update( );
    return this.currentPage;
  }

  public Integer lastPage( ) {
    if( this.currentReport != null ) {
      return this.currentReport.getLength( );
    } else {
      return 0;
    }
  }

  public String getCurrentUrl( ReportType html ) {
    if( this.currentReport != null ) {
      return this.currentReport.getUrl( html );
    } else {
      return "#";
    }
  }

  public String getCurrentFileName( ) {
    if( this.currentReport != null ) {
      return this.currentReport.getFileName( );
    } else {
      return "#";
    }
  }

}
