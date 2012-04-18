package com.eucalyptus.webui.client.view;

import java.util.Date;

import com.google.gwt.user.client.ui.IsWidget;

public interface ReportView extends IsWidget {

  void init( Date fromDate, Date toDate, String[] criteriaList, String[] groupByList, String[] typeList );
  
  void loadReport( String url );
  
  void setPresenter( Presenter presenter );
  
  interface Presenter {
    
    void downloadPdf( );
    void downloadCsv( );
    void downloadXls( );
    void downloadHtml( );
    
    void generateReport( Date fromDate, Date toDate, String criteria, String groupBy, String type );
    
  }
  
}
