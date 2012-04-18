package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.view.InputField.ValueType;
import com.eucalyptus.webui.shared.checker.InvalidValueException;
import com.eucalyptus.webui.shared.checker.ValueChecker;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class InputViewImpl extends DialogBox implements InputView {
  
  private static final Logger LOG = Logger.getLogger( InputViewImpl.class.getName( ) );
  
  private static InputViewImplUiBinder uiBinder = GWT.create( InputViewImplUiBinder.class );
  
  interface InputViewImplUiBinder extends UiBinder<Widget, InputViewImpl> {}
  
  interface GridStyle extends CssResource {
    String grid( );
    
    String passwordWeak( );
    String passwordMedium( );
    String passwordStrong( );
    String passwordStronger( );
  }
  
  interface Resources extends ClientBundle {
    @Source( "image/denied_8x8_red.png" )
    ImageResource denied( );
  }
  
  private static final String TITLE_WIDTH = "160px";
  private static final String INPUT_WIDTH = "280px";
  private static final String INDICATOR_WIDTH = "12px";
  
  private static final String PASSWORDS_NOT_MATCH = "Passwords do not match";

  protected static final int TEXT_AREA_LINES = 10;
  
  @UiField
  Label subject;
  
  @UiField
  Label error;
  
  @UiField
  SimplePanel contentPanel;
  
  @UiField
  GridStyle gridStyle;
  
  Resources resources;
  
  private Presenter presenter;
  
  private Grid grid;

  private ArrayList<HasValueWidget> inputs;
  
  private ArrayList<InputField> fields;
  
  public InputViewImpl( ) {
    super( );
    setWidget( uiBinder.createAndBindUi( this ) );
    setGlassEnabled( true );
    resources = GWT.create( Resources.class );
  }

  @UiHandler( "ok" )
  void handleOkClickEvent( ClickEvent event ) {
    ArrayList<String> values = checkValues( );
    if ( values != null ) {
      hide( );
      this.presenter.process( subject.getText( ), values );
    }
  }

  @UiHandler( "cancel" )
  void handleCancelClickEvent( ClickEvent event ) {
    hide( );
    this.presenter.cancel( subject.getText( ) );
  }
  
  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void display( String caption, String subject, ArrayList<InputField> fields ) {
    setText( caption );
    this.error.setText( "" );
    this.subject.setText( subject );
    
    displayFields( fields );
    
    center( );
    show( );

    setFocusTofirstInput( );
  }

  private void setFocusTofirstInput( ) {
    for ( HasValueWidget valWidget : inputs ) {
      if ( valWidget != null ) {
        Widget widget = valWidget.getWidget( );
        if ( widget != null ) {
          if ( widget instanceof FocusWidget ) {
            ( ( FocusWidget ) widget ).setFocus( true );
            break;
          }
        }
      }
    }
  }

  private void displayFields( ArrayList<InputField> fields ) {
    if ( fields == null || fields.size( ) < 1 ) {
      return;
    }
    this.fields = fields;
    this.inputs = Lists.newArrayList( );
    
    grid = new Grid( fields.size( ), 3 );
    grid.addStyleName( gridStyle.grid( ) );
    grid.getColumnFormatter( ).setWidth( 0, TITLE_WIDTH );
    grid.getColumnFormatter( ).setWidth( 1, INPUT_WIDTH );
    grid.getColumnFormatter( ).setWidth( 1, INDICATOR_WIDTH );
    for ( int i = 0; i < fields.size( ); i++ ) {
      InputField field = fields.get( i );
      if ( field == null ) {
        continue;
      }
      grid.setText( i, 0, field.getTitle( ) );
      
      HasValueWidget widget = getHasValueWidget( field.getType( ), field.getChecker( ) );
      if ( widget == null ) {
        LOG.log( Level.WARNING, "Invalid field type: " + field.getType( ) );
        continue;
      }
      inputs.add( widget );
      grid.setWidget( i, 1, widget.getWidget( ) );
      
      grid.setHTML( i, 2, "<div style='width=20px;'>&nbsp;</div>" );
    }
    
    this.contentPanel.setWidget( grid );
  }

  private HasValueWidget getHasValueWidget( ValueType type, final ValueChecker checker ) {
    switch ( type ) {
      case TEXT:
        return new HasValueWidget( ) {

          private TextBox input = new TextBox( );
          
          @Override
          public Widget getWidget( ) {
            return input;
          }

          @Override
          public String getValue( ) {
            return this.input.getValue( );
          }
          
        };
      case TEXTAREA:
        return new HasValueWidget( ) {

          private TextArea input = getTextArea( );
          
          protected final TextArea getTextArea( ) {
            TextArea w = new TextArea( );
            w.setVisibleLines( TEXT_AREA_LINES );
            return w;
          }
          
          @Override
          public Widget getWidget( ) {
            return input;
          }

          @Override
          public String getValue( ) {
            return this.input.getValue( );
          }
          
        };
      case PASSWORD:
        return new HasValueWidget( ) {
          
          private PasswordTextBox input = new PasswordTextBox( );

          @Override
          public Widget getWidget( ) {
            return input;
          }

          @Override
          public String getValue( ) {
            return this.input.getValue( );
          }
          
        };
      case NEWPASSWORD:
        return new HasValueWidget( ) {

          private PasswordTextBox input = getPasswordInput( );
          
          protected final PasswordTextBox getPasswordInput( ) {
            final PasswordTextBox box = new PasswordTextBox( );
            //TODO(wenye): disable strength indication for now. The security Tsar said it is confusing.
            /*
            box.addKeyPressHandler( new KeyPressHandler( ) {

              @Override
              public void onKeyPress( KeyPressEvent arg0 ) {
                String strength = ValueChecker.WEAK;
                try {
                  strength = checker.check( box.getValue( ) );
                } catch ( InvalidValueException e ) { }
                LOG.log( Level.INFO, "Password strength: " + strength );
                box.setStyleName( getPasswordStrengthStyleName( strength ) );
              }

            } );
            */
            return box;
          }
          
          @Override
          public Widget getWidget( ) {
            return input;
          }

          @Override
          public String getValue( ) {
            return this.input.getValue( );
          }
          
        };
      default:
        return null;
    }
  }

  private ArrayList<String> checkValues( ) {
    ArrayList<String> values = Lists.newArrayList( );
    // clear up first
    for ( int i = 0; i < inputs.size( ); i++ ) {
      grid.clearCell( i, 2 );
    }
    for ( int i = 0; i < inputs.size( ); i++ ) {
      HasValueWidget input = inputs.get( i );
      if ( input != null ) {
        try {
          InputField field = fields.get( i );
          String value = input.getValue( );
          ValueChecker checker = field.getChecker( );
          if ( checker != null ) {
            checker.check( value );            
          }
          values.add( value );
          if ( ValueType.NEWPASSWORD.equals( field.getType( ) ) ) {
            // If it is a new password input, check the next field/row
            // to make sure the two password inputs match.
            InputField nextField = fields.get( i + 1 );
            if ( ValueType.PASSWORD.equals( nextField.getType( ) ) ) {
              if ( !input.getValue( ).equals( inputs.get( i + 1 ).getValue( ) ) ) {
                grid.setWidget( i + 1, 2, new Image( resources.denied( ) ) );
                this.error.setText( PASSWORDS_NOT_MATCH );
                return null;
              }
            }
          }
        } catch ( InvalidValueException e ) {
          grid.setWidget( i, 2, new Image( resources.denied( ) ) );
          this.error.setText( e.getMessage( ) );
          return null;
        }
      } else {
        values.add( null );
      }
    }
    return values;
  }
  
  private String getPasswordStrengthStyleName( String strength ) {
    String style = gridStyle.passwordWeak( );
    if ( ValueChecker.WEAK.equals( strength ) ) {
      style = gridStyle.passwordWeak( );
    } else if ( ValueChecker.MEDIUM.equals( strength ) ) {
      style = gridStyle.passwordMedium( );
    } else if ( ValueChecker.STRONG.equals( strength ) ) {
      style = gridStyle.passwordStrong( );
    } else if ( ValueChecker.STRONGER.equals( strength ) ) {
      style = gridStyle.passwordStronger( );
    }
    return style;
  }
  
}
