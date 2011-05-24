package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Anchor;
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
  
  public static final String ANCHOR = ">>";
  public static final int ARTICLE_LINES = 8;
  
  public static final String PASSWORD_ACTION = "password";
  
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

    private Anchor anchor;
    
    public HyperLinkValue( String url ) {
      this.anchor = new Anchor( ANCHOR, url );
    }
    
    @Override
    public String getValue( ) {
      return this.anchor.getHref( );
    }

    @Override
    public Widget getWidget( ) {
      return this.anchor;
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

    private Anchor anchor;
    
    public ActionValue( String value, final ActionHandler action ) {
      this.anchor.setText( value );
      this.anchor.addClickHandler( new ClickHandler( ) {
        @Override
        public void onClick( ClickEvent event ) {
          action.act( );
        }
      } );
    }
    
    @Override
    public Widget getWidget( ) {
      return this.anchor;
    }

    @Override
    public String getValue( ) {
      return this.anchor.getText( );
    }
    
    @Override
    public String toString( ) {
      return getValue( );
    }
  }
  
  static class LabelKey implements HasValueWidget {
    
    private Label label;
    private String key;
    
    public LabelKey( String key, String title ) {
      this.label = new Label( title );
      this.key = key;
    }
    
    @Override
    public String getValue( ) {
      return this.key;
    }
    
    @Override
    public Widget getWidget( ) {
      return this.label;
    }
    
  }
  
  static class RemovableLabelKey implements HasValueWidget {

    private static final String DELETE_SYMBOL = "&nbsp;[X]&nbsp;";
    
    final private LabelWithAnchor label;
    final private String key;
    final private ActionHandler action;

    public RemovableLabelKey( String key, String title, ActionHandler action ) {
      this.key = key;
      this.action = action;
      this.label = new LabelWithAnchor( );
      this.label.setContent( title, DELETE_SYMBOL );
      this.label.addClickHandler( new ClickHandler( ) {
        @Override
        public void onClick( ClickEvent event ) {
          RemovableLabelKey.this.action.act( );
        }
      } );
    }
    
    @Override
    public Widget getWidget( ) {
      return this.label;
    }

    @Override
    public String getValue( ) {
      return this.key;
    }
    
  }
  
  private static final String LABEL_WIDTH = "36%";
  
  @UiField
  GridStyle gridStyle;
  
  @UiField
  SpanElement title;
  
  @UiField
  Anchor save;
  
  @UiField
  ScrollPanel gridPanel;
  
  private Controller controller;
  private Presenter presenter;
  
  private ArrayList<HasValueWidget> gridValues = Lists.newArrayList( );
  private ArrayList<HasValueWidget> gridKeys = Lists.newArrayList( );
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
    presenter.saveValue( gridValues );
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
    this.controller.hideDetail( );
  }
  
  private void clearRows( ) {
    this.gridValues.clear( );
    this.gridKeys.clear( );
    this.gridRows.clear( );
  }
  
  private void addRow( HasValueWidget key, HasValueWidget value, Integer rowIndex ) {
    this.gridKeys.add( key );
    this.gridValues.add( value );
    this.gridRows.add( rowIndex );
  }
  
  @Override
  public void showData( ArrayList<SearchResultFieldDesc> descs, ArrayList<String> values ) {
    LOG.log( Level.INFO, "Show data" );
    clearRows( );
    this.save.setVisible( false );
    this.currentGrid = createGrid( descs, values );
    if ( this.currentGrid != null ) {
      gridPanel.setWidget( this.currentGrid );
    }
  }
  
  private Grid createGrid( ArrayList<SearchResultFieldDesc> descs, ArrayList<String> vals ) {
    if ( descs == null || descs.size( ) < 1 || vals == null || vals.size( ) < 1 ) {
      LOG.log( Level.WARNING, "Empty or partial input" );
      return null;
    }
    int size = Math.min( descs.size( ), vals.size( ) );
    Grid grid = new Grid( size, 2 );
    grid.addStyleName( gridStyle.grid( ) );
    grid.getColumnFormatter( ).setWidth( 0, LABEL_WIDTH );
    int row = 0;
    for ( int i = 0; i < size; i++ ) {
      SearchResultFieldDesc desc = descs.get( i );
      String val = vals.get( i );
      if ( desc != null && !desc.getHidden( ) ) {
        HasValueWidget label = getLabelWidget( i, desc );
        HasValueWidget content = getContentWidget( desc, val );
        if ( label != null && content != null ) {
          grid.setWidget( row, 0, label.getWidget( ) );
          grid.setWidget( row, 1, content.getWidget( ) );
          addRow( label, content, row++ );
          continue;
        }
      }
      // Hidden fields
      addRow( new HiddenValue( desc.getName( ) ), new HiddenValue( val ), null );
    }
    return grid;
  }
  
  private void removeTableRow( int index ) {
    gridKeys.remove( index );
    gridValues.remove( index );
    currentGrid.removeRow( gridRows.get( index ) );
  }
  
  private HasValueWidget getLabelWidget( final int index, SearchResultFieldDesc desc ) {
    switch ( desc.getType( ) ) {
      case KEYVAL:
        return new RemovableLabelKey( desc.getName( ), desc.getTitle( ), new ActionHandler( ) {
          @Override
          public void act( ) {
            removeTableRow( index );
          }
        } );
      case NEWKEYVAL:
        return new TextBoxValue( desc.getName( ), desc.getEditable( ), new ValueChangeHandler<String>( ) {
          @Override
          public void onValueChange( ValueChangeEvent<String> event ) {
            showSaveButton( );
          }
        } );
      default:
        return new LabelKey( desc.getName( ), desc.getTitle( ) );
    }
  }
  
  private HasValueWidget getContentWidget( final SearchResultFieldDesc desc, String val ) {
    switch ( desc.getType( ) ) {
      case TEXT:
      case KEYVAL:
      case NEWKEYVAL:
        return new TextBoxValue( val, desc.getEditable( ), new ValueChangeHandler<String>( ) {
          @Override
          public void onValueChange( ValueChangeEvent<String> event ) {
            showSaveButton( );
          }
        } );
      case ARTICLE:
        return new TextAreaValue( val, desc.getEditable( ), new ValueChangeHandler<String>( ) {
          @Override
          public void onValueChange( ValueChangeEvent<String> event ) {
            showSaveButton( );
          }
        } );        
      case HIDDEN:
        return new PasswordTextBoxValue( val, desc.getEditable( ), new ValueChangeHandler<String>( ) {
          @Override
          public void onValueChange( ValueChangeEvent<String> event ) {
            showSaveButton( );
          }
        } );
      case BOOLEAN:
        return new CheckBoxValue( val, desc.getEditable( ), new ValueChangeHandler<Boolean>( ) {
          @Override
          public void onValueChange( ValueChangeEvent<Boolean> event ) {
            showSaveButton( );
          }
        } );
      case DATE:
        return new DateBoxValue( val, desc.getEditable( ), new ValueChangeHandler<Date>( ) {
          @Override
          public void onValueChange( ValueChangeEvent<Date> event ) {
            showSaveButton( );
          }
        } );
      case LINK:
        return new HyperLinkValue( val );
      case ACTION:
        return new ActionValue( val, new ActionHandler( ) {
          @Override
          public void act( ) {
            popupAction( desc.getName( ) );
          }
        } );
    }
    return null;
  }

  protected void popupAction( String key ) {
    if ( PASSWORD_ACTION.equals( key ) ) {
      // TODO: popup the password change dialog
    }
  }

  private void showSaveButton( ) {
    this.save.setVisible( true );
  }
  
  @Override
  public void setController( Controller controller ) {
    this.controller = controller;
  }

  @Override
  public void clear( ) {
    this.gridValues.clear( );
    this.gridPanel.clear( );
    this.save.setVisible( false );
  }

  @Override
  public void disableSave( ) {
    this.save.setVisible( false );
  }
  
}
