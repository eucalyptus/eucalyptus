/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.webui.client.activity;

import java.util.Date;

import com.eucalyptus.webui.client.ClientFactory;
import com.eucalyptus.webui.client.place.ReportPlace;
import com.eucalyptus.webui.client.view.ReportView;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
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
    
    ActivityUtil.updateDirectorySelection( clientFactory );
  }

  
  private void downloadReport(String format) {
	  if (this.sessionId == null) {
	    return;
	  }

	  final String reportUrl =
		    "/reportservlet"
			+ "?session=" + sessionId
			+ "&type=" + type
			+ "&format=" + format
			+ "&start="	+ fromDate.getTime()
			+ "&end=" + (toDate.getTime()+(1000*60*60*24))  //Add one day because UI says "start" and "THRU"
			+ "&criterion=" + criteria
			+ "&groupByCriterion=" + groupBy;

	    Window.open( reportUrl, "_self", "" );
  }
  
  @Override
  public void downloadPdf( ) {
	  downloadReport("PDF");
  }

  @Override
  public void downloadCsv( ) {
	  downloadReport("CSV");
  }

  @Override
  public void downloadXls( ) {
	  downloadReport("XLS");
  }

  @Override
  public void downloadHtml( ) {
	  downloadReport("HTML");    
  }

  private String sessionId = null;
  private Date fromDate;
  private Date toDate;
  private String criteria;
  private String groupBy;
  private String type;
  
  @Override
  public void generateReport( Date fromDate, Date toDate, String criteria, String groupBy, String type ) {
    String sessionId = clientFactory.getLocalSession().getSession().getId();
    final String reportUrl =
      "/reportservlet"
      + "?session=" + sessionId
      + "&type=" + type
      + "&format=HTML" 
      + "&start="	+ fromDate.getTime()
      + "&end=" + (toDate.getTime()+(1000*60*60*24))  //Add one day because UI says "start" and "THRU
      + "&criterion=" + criteria
      + "&groupByCriterion=" + groupBy;

    clientFactory.getReportView( ).loadReport( reportUrl );

    this.sessionId = sessionId;
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.criteria = criteria;
    this.groupBy = groupBy;
    this.type = type;
  }
  
}
