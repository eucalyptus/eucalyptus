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
  $.widget('eucalyptus.eucatable', {
    options : { 
      id : '', // user of this widget should customize these options
      dt_arg : null,
      text : {
        header_title : '',
        search_refresh : '',
        create_resource : '',
        resource_found : '',
        action : '',
      },
      menu_actions : null, // e.g., TODO: add help
    },
    table : null, // jQuery object to the table
    actionMenu : null,
    _init : function() {
      thisObj = this; // 
      // add draw call back
      this.options.dt_arg['fnDrawCallback'] = function( oSettings ) { 
        try{
          thisObj._drawCallback(oSettings);
        }catch(e){
          // for some reason, dom to look for data is changed between calls (TODO: figure out why) 
          // exception handler is to catch the case
          var obj= thisObj.element.children().first().data('eucatable');
          if(obj)
            obj._drawCallback(oSettings);
        }
      };
      this.table = this.element.find('table').dataTable(this.options.dt_arg);
      var $header = this._decorateHeader();
      this._decorateSearchBar();
      this._decorateTopBar();
      this._decorateActionMenu();
      this._addActions(this.actionMenu);
    },

    _create : function() {
    },

    _destroy : function() {
    },

    _drawCallback : function(oSettings) {
      thisObj = this;
      $('#table_' + this.options.id + '_count').html(oSettings.fnRecordsDisplay());
      this.element.find('table tbody').find('tr').each(function(index, tr) {
        // add custom td handlers
        $currentRow = $(tr);
        if (thisObj.options.td_hover_actions) {
          $.each(thisObj.options.td_hover_actions, function (key, value) {
            $td = $currentRow.find('td:eq(' + value[0] +')');
            // first check if there is anything there
            if ($td.html() != '') {
              $td.hover( function(e) {
                value[1].call(this, e);
              });
              $td.click( function(e) {
                e.stopPropagation();
              });
            }
          });
        };
        // add generic row handler
        $currentRow.click( function (e) {
          // checked/uncheck on checkbox
          $rowCheckbox = $(e.target).parents('tr').find(':input[type="checkbox"]');
          $rowCheckbox.attr('checked', !$rowCheckbox.is(':checked'));
          e.stopPropagation();
          thisObj._trigger('row_click', e);
        });
        $currentRow.find(':input[type="checkbox"]').click( function (e) {
          e.stopPropagation();
          thisObj._trigger('row_click', e);
        });
        if (thisObj.options.context_menu) {
          contextMenuParams = thisObj.options.context_menu;
          rID = 'ri-'+S4()+S4();
          $currentRow.attr('id', rID);
          // context menu
          $.contextMenu({
            selector: '#'+rID,
            build: function(trigger, e) {
              rowId = $(trigger).attr('id');
              // TODO : thisObj.table sometime is undef :(
              if (!thisObj.table) {
                $table = $($(trigger).parents('table')[0]);
                thisObj.table = $table.dataTable();
              }

              nNotes = thisObj.table.fnGetNodes();
              inx = 0;
              for ( i in nNotes ){
                if ( rowId == $(nNotes[i]).attr('id') ) {
                  inx = i;
                  break;
                }
              };
              var itemsList = contextMenuParams.build_callback( 
                thisObj.table.fnGetData(inx) // entire row is given to callback
              );
              return {
                items: itemsList,
              };
            }
          });
        }
      });      
    },

    reDrawTable : function() {
      this.table.fnDraw();
    },

    refreshTable : function() {
      this.table.fnReloadAjax();
    },

    getTable : function() {
      return this.table;
    },

    activateMenu : function() {
      $menu = $('#more-actions-'+this.options.id);
      $menu.removeClass("inactive-menu");
      $menu.contextMenu(true);
    },

    deactivateMenu : function() {
      $menu = $('#more-actions-'+this.options.id);
      $menu.addClass("inactive-menu");
      $menu.contextMenu(false);
    },

    // args.title = title in the header (e.g.,'Manage key pairs');
    _decorateHeader : function(args) {
      var thisObj = this;
      $header = this.element.find('.table_' + this.options.id + '_header');
      $header.addClass('euca-table-header');
      $header.append(
        $('<span>').text(thisObj.options.text.header_title).append(
          $('<div>').addClass('help-link').append(
            $('<a>').attr('href','#').text('?').click( function(evt){
              thisObj._trigger('help_click', evt);
            }))));
      return $header;
    },

    // args.refresh = text 'Refresh'
    _decorateSearchBar : function(args) {
      var thisObj = this; // ref to widget instance
      var $searchBar = this.element.find('#'+this.options.id+'_filter');
      var refresh = this.options.text.search_refresh ? this.options.text.search_refresh : search_refresh;
      $searchBar.append(
        $('<a>').addClass('table-refresh').attr('href','#').text(refresh).click(function(){
          thisObj.refreshTable();
        }));
      return $searchBar;
    },   

    // args.txt_create (e.g., Create new key)
    // args.txt_found ('e.g., 12 keys found)
    _decorateTopBar : function(args) {
      var thisObj = this; // ref to widget instance
      $tableTop = this.element.find('.table_' + this.options.id + '_top');
      $tableTop.addClass('euca-table-length');
      $tableTop.append(
        $('<div>').addClass('euca-table-add').append(
          $('<a>').attr('id','table-'+this.options.id+'-new').addClass('add-resource').attr('href','#').text(thisObj.options.text.create_resource)),
        $('<div>').addClass('euca-table-action actionmenu'),
        $('<div>').addClass('euca-table-size').append(
          $('<span>').attr('id','table_' + this.options.id + '_count'),
          $('<span>').attr('id','tbl_txt_found').addClass('resources-found').html('&nbsp; '+thisObj.options.text.resource_found),
          'Showing&nbsp;',
          $('<span>').addClass('show selected').text('10'),
          '&nbsp;|&nbsp;',
          $('<span>').addClass('show').text('25'),
          '&nbsp;|&nbsp;',
          $('<span>').addClass('show').text('50'),
          '&nbsp;|&nbsp;',
          $('<span>').addClass('show').text('all')));

      $tableTop.find('span.show').click(function () {
        $(this).parent().children('span').each( function() {
          $(this).removeClass('selected');
        });
        
        if ($(this).text() == 'all')
          thisObj.table.fnSettings()._iDisplayLength = -1;
        else
          thisObj.table.fnSettings()._iDisplayLength = parseInt($(this).text().replace('|',''));
        thisObj.table.fnDraw();
        $(this).addClass('selected');
      });

      // add action to create new
      this.element.find('#table-' + this.options.id + '-new').click(function() {
        thisObj._trigger('menu_click_create'); // users of the table should listen to
      });
      return $tableTop;
    },

    /*
      args.text : text (e.g., More actions)
      args.action : { action_key : [Text, click Callback] }
                   e.g., { delete : [Delete, function () { dialog('open'); } ] }
    */
    _decorateActionMenu : function (args){
      var thisObj = this; // ref to widget object
      var $menuDiv = this.element.find('div.euca-table-action');
      if ($menuDiv === undefined)
        return undefined;
      if (!this.options.menu_actions)
        return undefined;
      var txt_action = this.options.text.action ? this.options.text.action : table_menu_main_action;
      $menuDiv.append($('<span>').attr('id','more-actions-'+this.options.id).
                      addClass("inactive-menu").text(txt_action));
      var actions = thisObj.options.menu_actions;
      $.contextMenu({
            selector: '#more-actions-'+this.options.id,
            trigger: "left",
            build: function(trigger, e) {
              var itemsList = actions.call();
              return {
                items: itemsList,
              };
            }
          });
      // TODO: init menu in deactive state
      $('#more-actions-'+this.options.id).contextMenu(false);

      return $menuDiv;
    },

    _addActions : function (actionMenu) {
      var thisObj = this;
      thisTable = this.table;
      // add select/deselect all action
      $checkbox = this.element.find('#' + this.options.id + '-check-all');
      $checkbox.change(function() {
        var rows = thisTable.fnGetVisibleTrNodes();
        if(this.checked) {
          for ( i = 0; i<rows.length; i++ ) {
            cb = rows[i].firstChild.firstChild;
            if ( cb != null ) cb.checked = true;
          }
         // activate action menu
          thisObj.activateMenu();
        } else {
          for ( i = 0; i<rows.length; i++ ) {
            cb = rows[i].firstChild.firstChild;
            if ( cb != null ) cb.checked = false;
          }
          // deactivate action menu
          thisObj.deactivateMenu();
        }
      });
      //TODO: add hover to select all
      $checkbox.parent().hover( function () {
        //TODO: add action here
      });
    },

    countSelectedRows : function () {
      var dataTable = this.table;
      if ( !dataTable )
        return 0;
      var rows = dataTable.fnGetVisibleTrNodes();
      var selectedRows = 0;
      for ( i = 0; i<rows.length; i++ ) {
        cb = rows[i].firstChild.firstChild;
        if ( cb != null && cb.checked == true )
          selectedRows = selectedRows + 1;
      }
      return selectedRows;
    },

    getValueForSelectedRows : function (columnIdx) {
      var dataTable = this.table;
      if ( !dataTable )
        return [];
      var rows = dataTable.fnGetVisibleTrNodes();
      var selectedRows = [];
      for ( i = 0; i<rows.length; i++ ) {
        cb = rows[i].firstChild.firstChild;
        if ( cb != null && cb.checked == true ) {
          selectedRows.push(dataTable.fnGetData(rows[i], columnIdx));
        }
      }
      return selectedRows;
    },
    //TODO: re-use  getContentForSelectedRows ?
    getAllSelectedRows : function (idIndex) {
      var dataTable = this.table;
      if ( !dataTable )
        return [];
      idIndex = idIndex || 1;
      var rows = dataTable.fnGetVisibleTrNodes();
      var selectedRows = [];
      for ( i = 0; i<rows.length; i++ ) {
        cb = rows[i].firstChild.firstChild;
        if ( cb != null && cb.checked == true ) {
          if ( rows[i].childNodes[idIndex] != null )
            selectedRows.push(rows[i].childNodes[idIndex].firstChild.nodeValue);
        }
      }
      return selectedRows;
    },

    getContentForSelectedRows : function () {
      var dataTable = this.table;
      if ( !dataTable )
        return [];
      var rows = dataTable.fnGetVisibleTrNodes();
      var selectedRows = [];
      for ( i = 0; i<rows.length; i++ ) {
        cb = rows[i].firstChild.firstChild;
        if ( cb != null && cb.checked == true ) {
          selectedRows.push(rows[i]);
        }
      }
      return selectedRows;
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
