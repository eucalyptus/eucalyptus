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

(function($, eucalyptus) {
  $.widget('eucalyptus.snapshot', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #snapshotTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_snapshot)));
      var $snapshotTable = $wrapper.children().first();
      var $snapshotHelp = $wrapper.children().last();
      this.baseTable = $snapshotTable;
      this.tableWrapper = $snapshotTable.eucatable_bb({
        id : 'snapshots', // user of this widget should customize these options,
        data_deps: ['snapshots', 'tags', 'images'],
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'snapshot',
        },
        text : {
          header_title : snapshot_h_title,
          create_resource : snapshot_create,
          resource_found : 'snapshot_found',
          resource_search : snapshot_search,
          resource_plural : snapshot_plural,
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        menu_click_create : function (args) { 
            thisObj._createSnapshotAction();   // BACKBONE INTEGRATED DIALOG --- Kyo 041013
        },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $snapshotHelp, url: help_snapshot.landing_content_url});
        },
        filters : [{name:"snap_state", options: ['all','in-progress','completed'], text: [snap_state_selector_all, snap_state_selector_in_progress, snap_state_selector_completed], filter_col:7, alias: {'in-progress':'pending','completed':'completed'}} ],
        legend : ['pending', 'completed', 'error'],
      });
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      var thisObj = this;
      var selectedSnapshots = thisObj.baseTable.eucatable_bb('getSelectedRows', 7); // 7th column=status (this is snapshot's knowledge)
      var itemsList = {};
      (function(){
        itemsList['delete_snapshot'] = { "name": snapshot_action_delete, callback: function(key, opt) {;}, disabled: function(){ return true;} }  // Backbone Integration --- Kyo 040813
        itemsList['create_volume'] = { "name": snapshot_action_create_volume, callback: function(key, opt) {;}, disabled: function(){ return true;} }  // Backbone Integration --- Kyo 040813
        itemsList['register_snapshot'] = { "name": snapshot_action_register, callback: function(key, opt) {;}, disabled: function(){ return true;} }  // Backbone Integration --- Kyo 040813
        itemsList['tag'] = { "name": table_menu_edit_tags_action, callback: function(key, opt) {;}, disabled: function(){ return true;} }
      })();

      if ( selectedSnapshots.length > 0 && selectedSnapshots.indexOf('pending') == -1 ){
        itemsList['delete_snapshot'] = { "name": snapshot_action_delete, callback: function(key, opt) { thisObj._deleteSnapshotAction(); } }
      }
      
      if ( selectedSnapshots.length === 1 && onlyInArray('completed', selectedSnapshots)){
        itemsList['tag'] = {"name": table_menu_edit_tags_action, callback: function(key, opt){ thisObj._tagResourceAction(); }}
        itemsList['register_snapshot'] = { "name": snapshot_action_register, callback: function(key, opt) { thisObj._registerSnapshotAction(); } }
        itemsList['create_volume'] = { "name": snapshot_action_create_volume, callback: function(key, opt) { thisObj._createVolumeAction(); } }
      }

      return itemsList;
    },

    _tagResourceAction : function(){
      var selected = this.tableWrapper.eucatable_bb('getSelectedRows', 10);
      if ( selected.length > 0 ) {
        require(['app'], function(app) {
           app.dialog('edittags', app.data.snapshot.get(selected[0]));
        });
       }
    },

    _deleteSnapshotAction : function(){
      var dialog = 'delete_snapshot_dialog';
      var selected = this.tableWrapper.eucatable_bb('getSelectedRows', 10);
      require(['views/dialogs/' + dialog], function( dialog) {
        new dialog({items: selected});
      });
    },

    _createVolumeAction : function(){
      var dialog = 'create_volume_dialog';
      var selected = this.tableWrapper.eucatable_bb('getSelectedRows', 10);
      require(['views/dialogs/' + dialog], function( dialog) {
        new dialog({snapshot_id: selected});
      });
    },

    _registerSnapshotAction : function(){
      var dialog = 'register_snapshot_dialog';
      var selected = this.tableWrapper.eucatable_bb('getSelectedRows', 10);
      require(['views/dialogs/' + dialog], function( dialog) {
        new dialog({item: selected});
      });
    },

    _createSnapshotAction : function(){
      var dialog = 'create_snapshot_dialog';
      require(['app'], function(app) {
        app.dialog(dialog);
      });
    },

/**** Public Methods ****/
    close: function() {
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },

/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
