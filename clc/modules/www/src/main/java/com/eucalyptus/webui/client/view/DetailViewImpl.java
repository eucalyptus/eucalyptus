package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.datepicker.client.DateBox;

public class DetailViewImpl extends Composite implements DetailView {
  
  private static Logger LOG = Logger.getLogger( DetailViewImpl.class.getName( ) );
  
  private static DetailViewImplUiBinder uiBinder = GWT.create( DetailViewImplUiBinder.class );
  
  interface DetailViewImplUiBinder extends UiBinder<Widget, DetailViewImpl> {}
  
  interface GridStyle extends CssResource {
    String grid( );
  }
  
  static interface ActionHandler {
    void act( );  
  }
  
  static class HiddenValue implements HasValueWidget {
    
    private String value; 
    
    public HiddenValue( String value ) {
      this.value = value;
    }
    
    @Override
    public String getValue( ) {
      return this.value;
    }

    @Override
    public Widget getWidget( ) {
      return null;
    }
    
  }
  
  static class TextBoxValue implements HasValueWidget {
    
    private TextBox textBox;
    
    public TextBoxValue( String value, boolean enabled, ValueChangeHandler<String> changeHandler ) {
      this.textBox = new TextBox( );
      this.textBox.setReadOnly( !enabled );
      this.textBox.setValue( value == null ? "" : value );
      this.textBox.addValueChangeHandler( changeHandler );
    }
    
    @Override
    public String getValue( ) {
      return textBox.getValue( );
    }

    @Override
    public Widget getWidget( ) {
      return textBox;
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
    
  }
  
  static class RevealingTextBoxValue implements HasValueWidget {
    
    public static final String HINT = "Mouse over to see";
    
    private TextBox textBox;
    private String value;
    
    public RevealingTextBoxValue( final String value ) {
      this.value = value;
      this.textBox = new TextBox( );
      this.textBox.setReadOnly( true );
      this.textBox.setValue( HINT );
      this.textBox.addMouseOverHandler( new MouseOverHandler( ) {
        @Override
        public void onMouseOver( MouseOverEvent event ) {
          textBox.setValue( value == null ? "" : value );
        }
      } );
      this.textBox.addMouseOutHandler( new MouseOutHandler( ) {
        @Override
        public void onMouseOut( MouseOutEvent arg0 ) {
          textBox.setValue( HINT );
        }
      } );
    }
    
    @Override
    public String getValue( ) {
      return this.value;
    }

    @Override
    public Widget getWidget( ) {
      return textBox;
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
    
  }
    
  static class PasswordTextBoxValue implements HasValueWidget {
    
    private PasswordTextBox textBox;
    
    public PasswordTextBoxValue( String value, boolean enabled, ValueChangeHandler<String> changeHandler ) {
      this.textBox = new PasswordTextBox( );
      this.textBox.setReadOnly( !enabled );
      this.textBox.setValue( value == null ? "" : value );
      this.textBox.addValueChangeHandler( changeHandler );
    }
    
    @Override
    public String getValue( ) {
      return textBox.getValue( );
    }

    @Override
    public Widget getWidget( ) {
      return textBox;
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
    
  }
  
  static class CheckBoxValue implements HasValueWidget {
    
    private CheckBox checkBox;
    
    public CheckBoxValue( String value, boolean enabled, ValueChangeHandler<Boolean> changeHandler  ) {
      this.checkBox = new CheckBox( );
      this.checkBox.setEnabled( enabled );
      if ( value != null && "true".equalsIgnoreCase( value ) ) {
        this.checkBox.setValue( true );
      } else {
        this.checkBox.setValue( false );
      }
      this.checkBox.addValueChangeHandler( changeHandler );
    }
    
    @Override
    public String getValue( ) {
      return checkBox.getValue( ).toString( );
    }

    @Override
    public Widget getWidget( ) {
      return checkBox;
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
    
  }
  
  static class HyperLinkValue implements HasValueWidget {

    private IconButton link;
    
    public HyperLinkValue( String url ) {
      this.link = new IconButton( );
      this.link.setType( IconButton.Type.show );
      this.link.setHref( url );
    }
    
    @Override
    public String getValue( ) {
      return this.link.getHref( );
    }

    @Override
    public Widget getWidget( ) {
      return this.link;
    }
  
    @Override
    public String toString( ) {
      return getValue( );
    }
  }
  
  static class TextAreaValue implements HasValueWidget {
    
    private TextArea textArea;
    
    public TextAreaValue( String value, boolean enabled, ValueChangeHandler<String> changeHandler ) {
      this.textArea = new TextArea( );
      this.textArea.setReadOnly( !enabled );
      this.textArea.setVisibleLines( ARTICLE_LINES );
      this.textArea.setValue( value == null ? "" : value );
      this.textArea.addValueChangeHandler( changeHandler );
    }

    @Override
    public String getValue( ) {
      return this.textArea.getValue( );
    }

    @Override
    public Widget getWidget( ) {
      return this.textArea;
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
    
  }
  
  static class DateBoxValue implements HasValueWidget {
    
    private DateBox dateBox;
    
    public DateBoxValue( String date, boolean enabled, ValueChangeHandler<Date> changeHandler ) {
      this.dateBox = new DateBox( );
      this.dateBox.setEnabled( enabled );
      if ( date != null && !"".equals( date ) ) {
        Long value = Long.parseLong( date );
        this.dateBox.setValue( new Date( value ) );
      }
      this.dateBox.addValueChangeHandler( changeHandler );
    }

    @Override
    public String getValue( ) {
      return Long.toString( this.dateBox.getValue( ).getTime( ) );
    }

    @Override
    public Widget getWidget( ) {
      return this.dateBox;
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
    
  }
  
  static class ActionValue implements HasValueWidget {

    private IconButton button;
    private IconButton.Type type;
    
    public ActionValue( String val, final ActionHandler action ) {
      this.type = getType( val );
      this.button = new IconButton( );
      this.button.setType( this.type );
      this.button.addClickHandler( new ClickHandler( ) {
        @Override
        public void onClick( ClickEvent event ) {
          action.act( );
        }
      } );
    }
    
    private static IconButton.Type getType( String typeVal ) {
      try {
        return IconButton.Type.valueOf( typeVal );
      } catch ( Exception e ) {
        return IconButton.Type.modify;
      }
    }
    
    @Override
    public Widget getWidget( ) {
      return this.button;
    }

    @Override
    public String getValue( ) {
      return this.type.name( );
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
  }
  
  static class RemovableValue implements HasValueWidget {

    private InputWithButton input;
    
    public RemovableValue( String val, ValueChangeHandler<String> changeHandler, final ActionHandler actionHandler ) {
      this.input = new InputWithButton( );
      this.input.setValue( val );
      this.input.setType( IconButton.Type.remove );
      this.input.addValueChangeHandler( changeHandler );
      this.input.addClickHandler( new ClickHandler( ) {
        @Override
        public void onClick( ClickEvent event ) {
          actionHandler.act( );
        }
      } );
    }
    
    @Override
    public Widget getWidget( ) {
      return this.input;
    }

    @Override
    public String getValue( ) {
      return this.input.getValue( );
    }
    
    @Override
    public String toString( ) {
      return this.getValue( );
    }
    
  }
  
  public static final String ANCHOR = "Show";
  public static final int ARTICLE_LINES = 8;
  
  private static final String LABEL_WIDTH = "36%";
  private static final String NEW_KEY = "enter new info";
  
  private final ValueChangeHandler<String> STRING_VALUE_CHANGE_HANDLER = new ValueChangeHandler<String>( ) {
    @Override
    public void onValueChange( ValueChangeEvent<String> event ) {
      showSaveButton( );
    }
  };

  private final ValueChangeHandler<Boolean> BOOLEAN_VALUE_CHANGE_HANDLER = new ValueChangeHandler<Boolean>( ) {
    @Override
    public void onValueChange( ValueChangeEvent<Boolean> event ) {
      showSaveButton( );
    }
  };

  private final ValueChangeHandler<Date> DATE_VALUE_CHANGE_HANDLER = new ValueChangeHandler<Date>( ) {
    @Override
    public void onValueChange( ValueChangeEvent<Date> event ) {
      showSaveButton( );
    }
  };
  
  @UiField
  GridStyle gridStyle;
  
  @UiField
  SpanElement title;
  
  @UiField
  Button save;
  
  @UiField
  ScrollPanel gridPanel;
  
  private Controller controller;
  private Presenter presenter;
  
  private ArrayList<HasValueWidget> gridValues = Lists.newArrayList( );
  private ArrayList<String> gridKeys = Lists.newArrayList( );
  private ArrayList<Integer> gridRows = Lists.newArrayList( );
  
  private Grid currentGrid;
  
  public DetailViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }
  
  @UiHandler( "close" )
  void handleCloseEvent( ClickEvent e ) {
    closeSelf( );
  }
  
  @UiHandler( "save" )
  void handleSave( ClickEvent e ) {
    LOG.log( Level.INFO, "Save!" );
    presenter.saveValue( gridKeys, gridValues );
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }
  
  @Override
  public void setTitle( String title ) {
    this.title.setInnerText( title );
  }
  
  private void closeSelf( ) {
    // Ask ShellView to close me
    this.controller.hideDetail( );
    // Tell the content view to clear selection
    this.presenter.onHide( );
  }
  
  private void clearRows( ) {
    this.gridValues.clear( );
    this.gridKeys.clear( );
    this.gridRows.clear( );
    this.currentGrid = null;
  }
  
  @Override
  public void showData( ArrayList<SearchResultFieldDesc> descs, ArrayList<String> values ) {
    LOG.log( Level.INFO, "Show data: " + descs + " | " + values );
    clearRows( );
    this.save.setEnabled( false );
    createGrid( descs, values );
    if ( this.currentGrid != null ) {
      gridPanel.setWidget( this.currentGrid );
    }
  }
  
  private int getGridSize( ArrayList<SearchResultFieldDesc> descs ) {
    int i = 0;
    for ( SearchResultFieldDesc d : descs ) {
      if ( d != null && !d.getHidden( ) ) i++;
    }
    return i;
  }
  
  private void createGrid( ArrayList<SearchResultFieldDesc> descs, ArrayList<String> vals ) {
    if ( descs == null || descs.size( ) < 1 || vals == null || vals.size( ) < 1 ) {
      LOG.log( Level.WARNING, "Empty or partial input" );
      return;
    }
    int size = getGridSize( descs );
    this.currentGrid = new Grid( size, 2 );
    this.currentGrid.addStyleName( gridStyle.grid( ) );
    this.currentGrid.getColumnFormatter( ).setWidth( 0, LABEL_WIDTH );
    int row = 0;
    for ( int i = 0; i < descs.size( ); i++ ) {
      SearchResultFieldDesc desc = descs.get( i );
      if ( i >= vals.size( ) ) {
        LOG.log( Level.WARNING, "Search result row column size does not match value size" );
        break;
      }
      String val = vals.get( i );
      if ( desc != null && !desc.getHidden( ) ) {
        if ( desc.getType( ).equals( Type.NEWKEYVAL ) ) {
          // Add the new value input row at the end
          addNewKeyValRow( row );
          // This should be the last row
          break;
        } else {
          HasValueWidget widget = getContentWidget( desc.getType( ), desc.getName( ), val, desc.getEditable( ) );
          Widget label = getLabelWidget( desc.getType( ), desc.getTitle( ), val );
          if ( label != null && widget != null ) {
            addRow( desc.getName( ), label , widget, row++ );
            continue;
          }
        }
      }
      // Hidden fields
      addRow( desc != null ? desc.getName( ) : "", null/*keyWidget*/, new HiddenValue( val ), null/*rowIndex*/ );
    }
  }
  
  private Widget getLabelWidget( Type type, String title, String val ) {
    if ( Type.LINK.equals( type ) ) {
      return new Anchor( title, val );
    } else {
      return new Label( title );
    }
  }
  
  private void addNewKeyValRow( final int rowIndex ) {
    LOG.log( Level.INFO, "Adding NEWKEYVAL row." );
    final TextBox keyInput = new TextBox( );
    keyInput.setValue( NEW_KEY );
    final InputWithButton valueInput = new InputWithButton( );
    valueInput.setType( IconButton.Type.add );
    valueInput.addClickHandler( new ClickHandler( ) {
      @Override
      public void onClick( ClickEvent event ) {
        HasValueWidget widget = getContentWidget( Type.KEYVAL, keyInput.getValue( ), valueInput.getValue( ), true );
        if ( widget == null ) {
          // impossible, just to pass static analysis
          LOG.log( Level.WARNING, "Failed to get content widget" );
          return;
        }
        // Always append to the end, but before the new value input row.
        addRow( keyInput.getValue( ), new Label( keyInput.getValue( ) ), widget, currentGrid.getRowCount( ) - 1 );
        keyInput.setValue( NEW_KEY );
        valueInput.setValue( "" );
        showSaveButton( );
      }
    } );
    currentGrid.setWidget( rowIndex, 0, keyInput );
    currentGrid.setWidget( rowIndex, 1, valueInput );
  }

  private int findKeyIndex( String key ) {
    int i;
    for ( i = 0; i < gridKeys.size( ); i++ ) {
      if ( key != null && key.equals( gridKeys.get( i ) ) ) {
        return i;
      }
    }
    return -1;
  }
  
  private void updateFieldRowMapping( int index ) {
    for ( int i = index; i < gridRows.size( ); i++ ) {
      Integer row = gridRows.get( i );
      if ( row != null ) {
        gridRows.set( i, row - 1 );
      }
    }
  }
  
  private void removeRow( String key ) {
    int index = findKeyIndex( key );
    if ( index < 0 ) {
      return;
    }
    //LOG.log( Level.INFO, "Removing key=" + key + " index=" + index + " row=" + gridRows.get( index ) );
    gridKeys.remove( index );
    gridValues.remove( index );
    currentGrid.removeRow( gridRows.get( index ) );
    gridRows.remove( index );
    updateFieldRowMapping( index );
    //LOG.log( Level.INFO, "Current row mapping: " + gridRows );
  }
  
  private void addRow( String key, Widget keyWidget, HasValueWidget valueWidget, Integer rowIndex ) {
    //LOG.log( Level.INFO, "Adding " + key + " to row " + ( rowIndex != null ? rowIndex : "N/A" ) );
    this.gridKeys.add( key );
    this.gridValues.add( valueWidget );
    if ( rowIndex != null && rowIndex >= 0 ) {
      this.gridRows.add( rowIndex );
      if ( rowIndex == currentGrid.getRowCount( ) - 1 ) {
        LOG.log( Level.INFO, "Insert row at " + rowIndex );
        currentGrid.insertRow( rowIndex );
      }
      currentGrid.setWidget( rowIndex, 0, keyWidget );
      currentGrid.setWidget( rowIndex, 1, valueWidget.getWidget( ) );
    } else {
      this.gridRows.add( null );
    }
  }
  
  private HasValueWidget getContentWidget( Type type, final String key, String val, boolean editable ) {
    switch ( type ) {
      case TEXT:
        return new TextBoxValue( val, editable, STRING_VALUE_CHANGE_HANDLER );
      case ARTICLE:
        return new TextAreaValue( val, editable, STRING_VALUE_CHANGE_HANDLER );
      case HIDDEN:
        return new PasswordTextBoxValue( val, editable, STRING_VALUE_CHANGE_HANDLER );
      case REVEALING:
        return new RevealingTextBoxValue( val );
      case BOOLEAN:
        return new CheckBoxValue( val, editable, BOOLEAN_VALUE_CHANGE_HANDLER );
      case DATE:
        return new DateBoxValue( val, editable, DATE_VALUE_CHANGE_HANDLER );
      case LINK:
        return new HyperLinkValue( val );
      case ACTION:
        return new ActionValue( val, new ActionHandler( ) {
          @Override
          public void act( ) {
            popupAction( key );
          }
        } );
      case KEYVAL:
        return new RemovableValue( val, STRING_VALUE_CHANGE_HANDLER, new ActionHandler( ) {
          @Override
          public void act( ) {
            removeRow( key );
            showSaveButton( );
          }
        } );
    }
    return null;
  }

  protected void popupAction( String key ) {
    this.presenter.onAction( key );
  }

  private void showSaveButton( ) {
    this.save.setEnabled( true );
  }
  
  @Override
  public void setController( Controller controller ) {
    this.controller = controller;
  }

  @Override
  public void clear( ) {
    this.gridValues.clear( );
    this.gridPanel.clear( );
    this.save.setEnabled( false );
  }

  @Override
  public void disableSave( ) {
    this.save.setEnabled( false );
  }
  
}
