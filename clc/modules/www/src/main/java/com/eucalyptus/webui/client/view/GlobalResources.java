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

package com.eucalyptus.webui.client.view;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.ImageResource;

public interface GlobalResources extends ClientBundle {
  
  public interface EucaButtonStyle extends CssResource {
    String button( );
    String icon( );
    String pill( );

    String active( );
    
    String big( );
    String small( );
    
    String primary( );

    String positive( );
    String negative( );

    String left( );
    String middle( );
    String right( );
    
    String minus( );
    String plus( );
    String floppy( );
    String report( );
    String x( );
    String check( );
    String user( );
    String group( );
    String lock( );
    String key( );
    String sun( );
  }
  
  @Source( "EucaButton.css" )
  EucaButtonStyle buttonCss( );
  
  @Source( "image/plus_12x12_gray.png" )
  ImageResource plusGray( );
  @Source( "image/plus_12x12_white.png" )
  ImageResource plusWhite( );
  
  @Source( "image/minus_12x12_gray.png" )
  ImageResource minusGray( );
  @Source( "image/minus_12x12_white.png" )
  ImageResource minusWhite( );

  @Source( "image/floppy_12x12_gray.png" )
  ImageResource floppyGray( );
  @Source( "image/floppy_12x12_white.png" )
  ImageResource floppyWhite( );

  @Source( "image/article_12x12_gray.png" )
  ImageResource reportGray( );
  @Source( "image/article_12x12_white.png" )
  ImageResource reportWhite( );

  @Source( "image/check_12x12_gray.png" )
  ImageResource checkGray( );
  @Source( "image/check_12x12_white.png" )
  ImageResource checkWhite( );

  @Source( "image/x_12x12_gray.png" )
  ImageResource xGray( );
  @Source( "image/x_12x12_white.png" )
  ImageResource xWhite( );

  @Source( "image/user_12x12_gray.png" )
  ImageResource userGray( );
  @Source( "image/user_12x12_white.png" )
  ImageResource userWhite( );

  @Source( "image/group_12x12_gray.png" )
  ImageResource groupGray( );
  @Source( "image/group_12x12_white.png" )
  ImageResource groupWhite( );

  @Source( "image/lock_fill_12x12_gray.png" )
  ImageResource lockGray( );
  @Source( "image/lock_fill_12x12_white.png" )
  ImageResource lockWhite( );

  @Source( "image/key_fill_12x12_gray.png" )
  ImageResource keyGray( );
  @Source( "image/key_fill_12x12_white.png" )
  ImageResource keyWhite( );
  
  @Source( "image/sun_12x12_gray.png" )
  ImageResource sunGray( );
  @Source( "image/sun_12x12_white.png" )
  ImageResource sunWhite( );
    
}
