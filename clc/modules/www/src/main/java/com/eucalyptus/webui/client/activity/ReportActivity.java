package com.eucalyptus.webui.client.activity;

import java.util.Arrays;
import java.util.Date;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ReportPlace;
import com.eucalyptus.webui.client.view.ReportView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class ReportActivity extends AbstractActivity implements ReportView.Presenter {
  
  public static final String TITLE = "USAGE REPORT";
  
  private ReportPlace place;
  private ClientFactory clientFactory;
  
  public ReportActivity( ReportPlace place, ClientFactory clientFactory ) {
    this.place = place;
    this.clientFactory = clientFactory;
  }
  
  @Override
  public void start( AcceptsOneWidget container, EventBus eventBus ) {
    this.clientFactory.getShellView( ).getContentView( ).setContentTitle( TITLE );
    ReportView reportView = this.clientFactory.getReportView( );
    reportView.setPresenter( this );

    
    reportView.init( new Date( ),
                     new Date( ),
                     new String[] {"User","Account","Cluster","Availability Zone"},
                     new String[] {"None","Account","Cluster","Availability Zone"},
                     new String[] {"Instance","Storage","S3"});
    container.setWidget( reportView );
  }

  @Override
  public void downloadPdf( ) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void downloadCsv( ) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void downloadXls( ) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void downloadHtml( ) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void generateReport( Date fromDate, Date toDate, String criteria, String groupBy, String type ) {
    // TODO Fill in real report logic
    Timer t = new Timer( ) {

      @Override
      public void run( ) {
        clientFactory.getReportView( ).loadReport( "http://www.google.com" );
      }
      
    };
    t.schedule( 2000 );
  }
  
}
