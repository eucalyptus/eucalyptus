package com.eucalyptus.webui.client.view;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.datepicker.client.DateBox;


public class ReportViewImpl extends Composite implements ReportView {
  
  private static final Logger LOG = Logger.getLogger( ReportViewImpl.class.getName( ) );
  
  private static ReportViewImplUiBinder uiBinder = GWT.create( ReportViewImplUiBinder.class );
  
  interface ReportViewImplUiBinder extends UiBinder<Widget, ReportViewImpl> {}

  @UiField
  LayoutPanel contentPanel;
  
  @UiField
  DateBox fromDate;
  
  @UiField
  DateBox toDate;
  
  @UiField
  ListBox criteria;
  
  @UiField
  ListBox groupBy;
  
  @UiField
  ListBox type;
  
  private Presenter presenter;
  
  private LoadingAnimationViewImpl loadingAnimation;
  private Frame iFrame;
    
  public ReportViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
    loadingAnimation = new LoadingAnimationViewImpl( );
    iFrame = new Frame( );
    this.fromDate.setFormat( new DateBox.DefaultFormat( DateTimeFormat.getFormat( PredefinedFormat.DATE_LONG ) ) );
    this.toDate.setFormat( new DateBox.DefaultFormat( DateTimeFormat.getFormat( PredefinedFormat.DATE_LONG ) ) );
  }
  
  @UiHandler( "generateButton" )
  void handleGenerateButtonClick( ClickEvent e ) {
    this.contentPanel.clear( );
    this.contentPanel.add( loadingAnimation );
    this.presenter.generateReport( fromDate.getValue( ),
                                   toDate.getValue( ),
                                   criteria.getValue( criteria.getSelectedIndex( ) ),
                                   groupBy.getValue( groupBy.getSelectedIndex( ) ),
                                   type.getValue( type.getSelectedIndex( ) ) );
  }
  
  @UiHandler( "pdfButton" )
  void handlePdfButtonClick( ClickEvent e ) {
    this.presenter.downloadPdf( );
  }
  
  @UiHandler( "csvButton" )
  void handleCsvButtonClick( ClickEvent e ) {
    this.presenter.downloadCsv( );
  }

  @UiHandler( "htmlButton" )
  void handleHtmlButtonClick( ClickEvent e ) {
    this.presenter.downloadHtml( );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void loadReport( String url ) {
    this.contentPanel.clear( );
    this.contentPanel.add( iFrame );
    this.iFrame.setUrl( url );
    iFrame.setWidth( "100%" );
    iFrame.setHeight( "100%" );
    LOG.log( Level.INFO, "Loading: " + url );
  }

  @Override
  public void init( Date fromDate, Date toDate, String[] criteriaList, String[] groupByList, String[] typeList ) {
    //this.contentPanel.clear( );
    initDate( this.fromDate, fromDate );
    initDate( this.toDate, toDate );
    initList( this.criteria, criteriaList );
    initList( this.groupBy, groupByList );
    initList( this.type, typeList );
    type.addChangeHandler(new ChangeHandler() {

		@Override
		public void onChange(ChangeEvent arg0)
		{
			  /* NOTE: these are hard-coded Strings and do not access the
			   * various enums, because this is compiled to JS for the browser
			   * and has no access to the enums.
			   */
			  if (type.getSelectedIndex() == 2) {
				  /* S3 Reports allow only User and Account
				   */
				  initList(criteria, new String[] {"User","Account"});
				  initList(groupBy, new String[] {"None","Account"});
			  } else {		  
				  initList(criteria, new String[] {"User","Account","Cluster","Availability Zone"});
				  initList(groupBy, new String[] {"None","Account","Cluster","Availability Zone"});
			  }			
			  groupBy.setSelectedIndex(0);
			  criteria.setSelectedIndex(0);
		}
    	
    });
    
    criteria.addChangeHandler( new ChangeHandler() {

		@Override
		public void onChange(ChangeEvent arg0)
		{
			int oldGroupByInd = groupBy.getSelectedIndex();
			/* Display only groupBy selections which are appropriate for this
			 * report type and for the selected report criterion.
			 */
			if (type.getSelectedIndex() == 2) {
				String[] list =
					(criteria.getSelectedIndex() == 1)
					? new String[] {"None"}
					: new String[] {"None","Account"};					
				initList(groupBy, list);
				groupBy.setSelectedIndex( 0 );
			} else {
				String[] list = null;
				switch (criteria.getSelectedIndex()) {
					case 3:
						list = new String[] {"None"};
						break;
					case 2:
						list = new String[] {"None","Availability Zone"};
						break;
					case 1:
						list = new String[] {"None","Cluster","Availability Zone"};
						break;
					default:
						list = new String[] {"None","Account","Cluster","Availability Zone"};
						break;
				}
				initList(groupBy, list);
				groupBy.setSelectedIndex( 0 );
			}			
		}    	
    });


  }

  private void initDate( DateBox w, Date date ) {
    if ( date != null ) {
      w.setValue( date );
    }
  }
  
  private void initList( ListBox l, String[] list ) {
	    l.clear( );
	    if ( list == null || list.length < 1 ) {
	      l.addItem( "None" );
	    } else {
	      for (int i=0; i<list.length; i++) {
	        l.addItem( list[i] );
	      }
	    }
	  }
  
  
}
