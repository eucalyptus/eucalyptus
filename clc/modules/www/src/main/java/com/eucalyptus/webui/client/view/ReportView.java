package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.google.gwt.user.client.ui.IsWidget;

public interface ReportView extends IsWidget {

  void init( Date fromDate, Date toDate, List<String> criteriaList, List<String> groupByList, List<String> typeList );
  
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
