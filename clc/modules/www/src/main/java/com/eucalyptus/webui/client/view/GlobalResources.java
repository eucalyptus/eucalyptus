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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
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
