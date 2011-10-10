package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.Set;
import com.eucalyptus.webui.client.service.SearchResultRow;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class ConfirmationViewImpl extends DialogBox implements ConfirmationView {
  
  private static final String COL_WIDTH = "100px";

  private static ConfirmationViewImplUiBinder uiBinder = GWT.create( ConfirmationViewImplUiBinder.class );
  
  interface ConfirmationViewImplUiBinder extends UiBinder<Widget, ConfirmationViewImpl> {}

  interface GridStyle extends CssResource {
    String grid( );
    String html( );
  }
  
  @UiField
  Label subject;
  
  @UiField
  ScrollPanel contentPanel;
  
  @UiField
  GridStyle gridStyle;
  
  private Presenter presenter;
  
  public ConfirmationViewImpl( ) {
    super( );
    setWidget( uiBinder.createAndBindUi( this ) );
    setGlassEnabled( true );
  }
  
  @UiHandler( "ok" )
  void handleOkClickEvent( ClickEvent event ) {
    hide( );
    this.presenter.confirm( subject.getText( ) );
  }

  @UiHandler( "cancel" )
  void handleCancelClickEvent( ClickEvent event ) {
    hide( );
  }

  @Override
  public void display( String caption, String subject, Set<SearchResultRow> list, ArrayList<Integer> fields ) {
    this.setText( caption );
    contentPanel.clear( );
    this.subject.setText( subject );
    
    if ( list.size( ) > 0 && fields.size( ) > 0 ) {
      Grid grid = new Grid( list.size( ), fields.size( ) );
      grid.addStyleName( gridStyle.grid( ) );
      grid.getColumnFormatter( ).setWidth( 0, COL_WIDTH );
      grid.getColumnFormatter( ).setWidth( 1, COL_WIDTH );
      int row = 0;
      for ( SearchResultRow rowData : list ) {
        int col = 0;
        for ( Integer field : fields ) {
          String text = rowData.getField( field );
          grid.setText( row, col++, text == null ? "" : text );
        }
        row++;
      }
      contentPanel.setWidget( grid );
    }
    center( );
    show( );
  }

  @Override
  public void display( String caption, String subject, SafeHtml html ) {
    this.setText( caption );
    contentPanel.clear( );
    this.subject.setText( subject );
    
    HTML widget = new HTML( html );
    widget.setStyleName( gridStyle.html( ) );
    contentPanel.setWidget( widget );
    
    center( );
    show( );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
}
