package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc;
import com.eucalyptus.webui.client.service.SearchResultFieldDesc.Type;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
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
import com.google.gwt.user.client.ui.LayoutPanel;
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
  
  class HiddenValue implements HasValueWidget {
    
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
  
  class TextBoxValue implements HasValueWidget {
    
    private TextBox textBox;
    
    public TextBoxValue( String value, boolean enabled, ValueChangeHandler<String> changeHandler ) {
      this.textBox = new TextBox( );
      this.textBox.setEnabled( enabled );
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
  
  class PasswordTextBoxValue implements HasValueWidget {
    
    private PasswordTextBox textBox;
    
    public PasswordTextBoxValue( String value, boolean enabled, ValueChangeHandler<String> changeHandler ) {
      this.textBox = new PasswordTextBox( );
      this.textBox.setEnabled( enabled );
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
  
  class CheckBoxValue implements HasValueWidget {
    
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
  
  class HyperLinkValue implements HasValueWidget {

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
  
  class TextAreaValue implements HasValueWidget {
    
    private TextArea textArea;
    
    public TextAreaValue( String value, boolean enabled, ValueChangeHandler<String> changeHandler ) {
      this.textArea = new TextArea( );
      this.textArea.setEnabled( enabled );
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
  
  class DateBoxValue implements HasValueWidget {
    
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
  
  private ArrayList<HasValueWidget> gridValues = new ArrayList<HasValueWidget>( );
  
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
  
  @Override
  public void showData( ArrayList<SearchResultFieldDesc> descs, ArrayList<String> gridValues ) {
    LOG.log( Level.INFO, "Show data" );
    this.gridValues.clear( );
    this.save.setVisible( false );
    Grid grid = createGrid( descs, gridValues );
    if ( grid != null ) {
      gridPanel.setWidget( grid );
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
        HasValueWidget widget = getWidget( desc, val );
        if ( widget != null ) {
          gridValues.add( widget );
          grid.setWidget( row, 0, new Label( desc.getTitle( ) ) );
          grid.setWidget( row, 1, widget.getWidget( ) );
          row++;
          continue;
        }
      }
      gridValues.add( new HiddenValue( val ) );
    }
    return grid;
  }
  
  private HasValueWidget getWidget( SearchResultFieldDesc desc, String val ) {
    switch ( desc.getType( ) ) {
      case TEXT:
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
    }
    return null;
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
