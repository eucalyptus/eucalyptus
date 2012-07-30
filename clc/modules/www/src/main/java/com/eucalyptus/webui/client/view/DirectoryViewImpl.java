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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.eucalyptus.webui.client.service.QuickLink;
import com.eucalyptus.webui.client.service.QuickLinkTag;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.CellTree.Resources;
import com.google.gwt.user.cellview.client.CellTree.Style;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;


public class DirectoryViewImpl extends Composite implements DirectoryView {
  
  public static final int TREENODE_SIZE = 50;
  
  private static final Logger LOG = Logger.getLogger( DirectoryViewImpl.class.getName( ) );
  
  private static DirectoryViewImplUiBinder uiBinder = GWT.create( DirectoryViewImplUiBinder.class );
  interface DirectoryViewImplUiBinder extends UiBinder<Widget, DirectoryViewImpl> {}
  
  public static interface TreeResources extends Resources {
    @Source( "CategoryTreeStyle.css" )
    Style cellTreeStyle( );
  }
  
  @UiField
  ScrollPanel treePanel;

  private Presenter presenter;
  
  private final SingleSelectionModel<QuickLink> selectionModel = new SingleSelectionModel<QuickLink>( QuickLink.KEY_PROVIDER );
  
  public DirectoryViewImpl( ) {
    initWidget( uiBinder.createAndBindUi( this ) );
  }

  @Override
  public void buildTree( ArrayList<QuickLinkTag> data ) {
    if ( data == null || data.size( ) < 1 ) {
      LOG.log( Level.WARNING, "Can not build category tree: data is empty." );
      return;
    }
    
    selectionModel.addSelectionChangeHandler( new SelectionChangeEvent.Handler( ) {
      
      @Override
      public void onSelectionChange( SelectionChangeEvent event ) {
        QuickLink selected = selectionModel.getSelectedObject( );
        if ( selected != null ) {
          LOG.log( Level.INFO, "Selected: " + selected.getName( ) );
          presenter.switchQuickLink( selected.getQuery( ) );
        }
      }
    } );
    
    CellTree.Resources resource = GWT.create( TreeResources.class );
    
    CellTree tree = new CellTree( new QuickLinkTreeModel( data, selectionModel ), null, resource );
    tree.setKeyboardSelectionPolicy( KeyboardSelectionPolicy.DISABLED );
    tree.setAnimationEnabled( false );
    tree.setDefaultNodeSize( TREENODE_SIZE );
    openAllNode( tree.getRootTreeNode( ) );
    
    treePanel.clear( );
    treePanel.add( tree );
  }

  private void openAllNode( TreeNode root ) {
    for ( int i = 0; i < root.getChildCount( ); i++ ) {
      root.setChildOpen( i, true );
    }
  }

  @Override
  public void setPresenter( Presenter presenter ) {
    this.presenter = presenter;
  }

  @Override
  public void changeSelection( QuickLink link ) {
    if ( link != null ) {
      selectionModel.setSelected( link, true );
    } else {
      // deselect current
      selectionModel.setSelected( selectionModel.getSelectedObject( ), false );
    }
  }
}
