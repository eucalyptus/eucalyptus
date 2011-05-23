package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
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

  @UiHandler( "xlsButton" )
  void handleXlsButtonClick( ClickEvent e ) {
    this.presenter.downloadXls( );
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
  public void init( Date fromDate, Date toDate, List<String> criteriaList, List<String> groupByList, List<String> typeList ) {
    //this.contentPanel.clear( );
    initDate( this.fromDate, fromDate );
    initDate( this.toDate, toDate );
    initList( this.criteria, criteriaList );
    initList( this.groupBy, groupByList );
    initList( this.type, typeList );
  }

  private void initDate( DateBox w, Date date ) {
    if ( date != null ) {
      w.setValue( date );
    }    
  }
  
  private void initList( ListBox l, List<String> list ) {
    l.clear( );
    if ( list == null || list.size( ) < 1 ) {
      l.addItem( "None" );
    } else {
      for ( String item : list ) {
        l.addItem( item );
      }
    }
  }
  
}
