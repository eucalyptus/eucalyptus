package com.eucalyptus.webui.client.view;

import java.util.ArrayList;
import java.util.HashMap;
import com.eucalyptus.webui.client.service.CategoryItem;
import com.eucalyptus.webui.client.service.CategoryTag;
import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;

public class CategoryTreeModel implements TreeViewModel {
  
  /*
   * Tree icons
   */
  static interface Images extends ClientBundle {
    @Source( "document_stroke_12x12.png" )
    ImageResource def( );
    
    @Source( "tag_fill_12x12.png" )
    ImageResource tag( );
    
    @Source( "cog_alt_12x12.png" )
    ImageResource service( );
    @Source( "user_12x16.png" )
    ImageResource user( );
    @Source( "article_12x12.png" )
    ImageResource report( );
    @Source( "home_12x12.png" )
    ImageResource home( );
    @Source( "image_12x12.png" )
    ImageResource image( );

  }
  
  private static class CategoryTagCell extends AbstractCell<CategoryTag> {

    public CategoryTagCell( ) {
    }
    
    @Override
    public void render( Context context, CategoryTag value, SafeHtmlBuilder sb ) {
      if ( value != null ) {
        sb.appendHtmlConstant( getImageHtml( images.tag( ).getName( ) ) )
          .appendEscaped( " " )
          .appendEscaped( value.getName( ) );
      }
    }
  }
  
  private static class CategoryItemCell extends AbstractCell<CategoryItem> {
    
    public CategoryItemCell( ) {
    }
    
    @Override
    public void render( Context context, CategoryItem value, SafeHtmlBuilder sb ) {
      if ( value != null ) {
        sb.appendHtmlConstant( getImageHtml( value.getImage( ) ) )
          .appendEscaped( " " )
          .appendEscaped( value.getName( ) );
      }
    }
  }
  
  private ArrayList<CategoryTag> tags;
  
  private SelectionModel<CategoryItem> selectionModel;
  
  private static Images images;
  private static HashMap<String, String> imageMap = new HashMap<String, String>( );
  
  public CategoryTreeModel( ArrayList<CategoryTag> tags, SelectionModel<CategoryItem> selectionModel ) {
    this.tags = tags;
    this.selectionModel = selectionModel;
    
    setupImageMap( );
  }
  
  private static void setupImageMap( ) {
    if ( images == null ) {
      images = GWT.create( Images.class );
      imageMap.put( images.def( ).getName( ), AbstractImagePrototype.create( images.def( ) ).getHTML( ) );
      imageMap.put( images.tag( ).getName( ), AbstractImagePrototype.create( images.tag( ) ).getHTML( ) );
      imageMap.put( images.service( ).getName( ), AbstractImagePrototype.create( images.service( ) ).getHTML( ) );
      imageMap.put( images.user( ).getName( ), AbstractImagePrototype.create( images.user( ) ).getHTML( ) );
      imageMap.put( images.report( ).getName( ), AbstractImagePrototype.create( images.report( ) ).getHTML( ) );
      imageMap.put( images.home( ).getName( ), AbstractImagePrototype.create( images.home( ) ).getHTML( ) );
      imageMap.put( images.image( ).getName( ), AbstractImagePrototype.create( images.image( ) ).getHTML( ) );
    }    
  }
  
  private static String getImageHtml( String name ) {
    String imageHtml = "";
    if ( name != null ) {
      imageHtml = imageMap.get( name );
    }
    if ( imageHtml == null ) {
      imageHtml = imageMap.get( images.def( ).getName( ) );
    }
    return imageHtml;
  }
  
  @Override
  public <T> NodeInfo<?> getNodeInfo( T value ) {
    if ( value == null ) {
      // Level 0
      ListDataProvider<CategoryTag> dataProvider = new ListDataProvider<CategoryTag>( this.tags );
      return new DefaultNodeInfo<CategoryTag>( dataProvider, new CategoryTagCell( ) );
    } else if ( value instanceof CategoryTag ) {
      // Level 1
      ListDataProvider<CategoryItem> dataProvider = new ListDataProvider<CategoryItem>( ( ( CategoryTag ) value ).getItems( ) );
      return new DefaultNodeInfo<CategoryItem>( dataProvider, new CategoryItemCell( ), selectionModel, null );
    }
    return null;
  }

  @Override
  public boolean isLeaf( Object value ) {
    if ( value != null && value instanceof CategoryItem ) {
      return true;
    }
    return false;
  }
  
}