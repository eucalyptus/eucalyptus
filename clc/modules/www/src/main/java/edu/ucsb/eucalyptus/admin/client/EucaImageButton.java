/*******************************************************************************
 *Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class EucaImageButton extends com.google.gwt.user.client.ui.PushButton {
  
  private static final String MAIN_STYLE_NAME       = "euca-Button";
  private static final String MOUSE_OVER_STYLE_NAME = "-over";
  private static final String MOUSE_DOWN_STYLE_NAME = "-down";
  
  private String              tooltip;
  private String              baseStyleName;
  
  public EucaImageButton( String html, String tooltip, String baseStyleName, String imageFileName, ClickHandler handler ) {
    super( new Image( "themes/active/img/" + imageFileName ), handler );
    this.tooltip = tooltip;
    this.baseStyleName = baseStyleName;
    this.addStyleName( this.baseStyleName );
    setEventHandler( );
  }
  
  private void setEventHandler( ) {
    sinkEvents( Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONMOUSEMOVE | Event.ONMOUSEUP | Event.ONMOUSEDOWN );
    this.addHandler( new MouseOutHandler( ) {
      public void onMouseOut( MouseOutEvent event ) {
        removeStyleName( EucaImageButton.this.baseStyleName + MOUSE_OVER_STYLE_NAME );
        removeStyleName( EucaImageButton.this.baseStyleName + MOUSE_DOWN_STYLE_NAME );
        Tooltip.getInstance( ).hide( );
      }
    }, MouseOutEvent.getType( ) );
    this.addHandler( new MouseOverHandler( ) {
      public void onMouseOver( MouseOverEvent event ) {
        addStyleName( EucaImageButton.this.baseStyleName + MOUSE_OVER_STYLE_NAME );
      }
    }, MouseOverEvent.getType( ) );
    this.addHandler( new MouseUpHandler( ) {
      public void onMouseUp( MouseUpEvent event ) {
        removeStyleName( EucaImageButton.this.baseStyleName + MOUSE_DOWN_STYLE_NAME );
      }
    }, MouseUpEvent.getType( ) );
    this.addHandler( new MouseDownHandler( ) {
      public void onMouseDown( MouseDownEvent event ) {
        addStyleName( EucaImageButton.this.baseStyleName + MOUSE_DOWN_STYLE_NAME );
      }
    }, MouseDownEvent.getType( ) );
    if ( this.tooltip != null ) {
      this.addHandler( new MouseMoveHandler( ) {
        public void onMouseMove( MouseMoveEvent event ) {
          int x = ( ( Widget ) event.getSource( ) ).getAbsoluteLeft( ) + event.getX( ) + 10;
          int y = ( ( Widget ) event.getSource( ) ).getAbsoluteTop( ) + event.getY( ) + 10;
          Tooltip.getInstance( ).delayedShow( x, y, 0, tooltip );
        }
      }, MouseMoveEvent.getType( ) );
    }
  }
}
