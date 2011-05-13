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
    @Source( "image/document_stroke_12x12_gray.png" )
    ImageResource def( );
    
    @Source( "image/tag_fill_12x12_gray.png" )
    ImageResource tag( );
    
    @Source( "image/cog_alt_12x12_gray.png" )
    ImageResource config( );
    @Source( "image/user_12x16_gray.png" )
    ImageResource user( );
    @Source( "image/group_12x11_gray.png" )
    ImageResource group( );    
    @Source( "image/article_12x12_gray.png" )
    ImageResource report( );
    @Source( "image/home_12x12_gray.png" )
    ImageResource home( );
    @Source( "image/image_12x12_gray.png" )
    ImageResource image( );
    @Source( "image/tag_stroke_12x12_gray.png" )
    ImageResource type( );
    @Source( "image/arrow_down_12x12_gray.png" )
    ImageResource down( );
    

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
      imageMap.put( images.config( ).getName( ), AbstractImagePrototype.create( images.config( ) ).getHTML( ) );
      imageMap.put( images.user( ).getName( ), AbstractImagePrototype.create( images.user( ) ).getHTML( ) );
      imageMap.put( images.report( ).getName( ), AbstractImagePrototype.create( images.report( ) ).getHTML( ) );
      imageMap.put( images.home( ).getName( ), AbstractImagePrototype.create( images.home( ) ).getHTML( ) );
      imageMap.put( images.image( ).getName( ), AbstractImagePrototype.create( images.image( ) ).getHTML( ) );
      imageMap.put( images.type( ).getName( ), AbstractImagePrototype.create( images.type( ) ).getHTML( ) );
      imageMap.put( images.group( ).getName( ), AbstractImagePrototype.create( images.group( ) ).getHTML( ) );
      imageMap.put( images.down( ).getName( ), AbstractImagePrototype.create( images.down( ) ).getHTML( ) );
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