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
      context_menu_actions : null,
      draw_cell_callback : null,  // if we want to customize how the cell is drawn (e.g., map EMI to manifest)
                                  // {column: 3, callback: function(){ } }
      filters : null, // e.g., [{name: "volume_state", options: ["available","attached","attaching"], filter_col: 8, alias: {"detached":"available" }}]
      legend : null, // e.g., ['available', 'attaching', 'attached', ...] 
    },

    table : null, // jQuery object to the table

    _init : function() {
      var thisObj = this; // 
      // add draw call back
      var dtArg = this._getTableParam();
      this.table = this.element.find('table').dataTable(dtArg);
      var $header = this._decorateHeader();
      this._decorateSearchBar();
      this._decorateTopBar();
      this._decorateActionMenu();
      this._decorateLegend();
      this._addActions();
    },

    _create : function() {
    },

    _destroy : function() {
    },

    _getTableParam : function(args){
      var thisObj = this;
      var dt_arg = this.options.dt_arg;      
      dt_arg['fnDrawCallback'] = function( oSettings ) {
      try{
          thisObj._drawCallback(oSettings);
        }catch(e){
          // for some reason, dom to look for data is changed between calls (TODO: figure out why) 
          // exception handler is to catch the case
          var obj= thisObj.element.children().first().data('eucatable');
          if(obj)
            obj._drawCallback(oSettings);
        }
      }

    //  "sDom": '<"table_volumes_header"><"table-volume-filter">f<"clear"><"table_volumes_top">rt<"table-volumes-legend">p<"clear">',
      var sDom = '<"table_'+ this.options.id + '_header">';
      if(thisObj.options.filters){
        $.each(thisObj.options.filters, function(idx, filter){
          var name = filter['name']+'-filter';
          sDom += '<"#'+name+'">';
        });
      }
      sDom += 'f<"clear"><"table_'+thisObj.options.id+'_top">rt';
      //if(thisObj.options.legend)
       // sDom += '<"#'+thisObj.options.id+'-legend">';
      sDom += 'p<"clear">';
      dt_arg['sDom'] = sDom;  
      return dt_arg;
    },

    _drawCallback : function(oSettings) {
      var thisObj = this;
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
          thisObj._onRowClick();
          thisObj._trigger('row_click', e);
        });

        $currentRow.find(':input[type="checkbox"]').click( function (e) {
          e.stopPropagation();
          thisObj._onRowClick();
          thisObj._trigger('row_click', e);
        });

        if (thisObj.options.context_menu_actions) {
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
              var itemsList = thisObj.options.context_menu_actions( 
                thisObj.table.fnGetData(inx) // entire row is given to callback
              );
              return {
                items: itemsList,
              };
            }
          });
        }
      });    

      if(thisObj.options.draw_cell_callback){
        this.element.find('table tbody').find('td').each(function(index, td) { 
          var pos = thisObj.table.fnGetPosition(td);
          var oldVal = $(td).html();
          var newVal = thisObj.options.draw_cell_callback(pos[0], pos[1], $(td).html());
          if(oldVal !== newVal)
            $(td).html(newVal);
        });
      }
    },

    _onRowClick : function() {
      if ( this._countSelectedRows() === 0 )
        this._deactivateMenu();
      else
        this._activateMenu();
    },

    _activateMenu : function() {
      $menu = $('#more-actions-'+this.options.id);
      $menu.removeClass("inactive-menu");
      $menu.contextMenu(true);
    },

    _deactivateMenu : function() {
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
  // filters : null, // e.g., [{name: "volume_state", options: ["available","attached","attaching"], filter_col: 8, alias: {"detached":"available" }}]
      if(thisObj.options.filters){
        $.each(thisObj.options.filters, function (idx, filter){
          var $filter = thisObj.element.find('#'+filter['name']+'-filter');
          $filter.addClass('euca-table-filter');
          $filter.append(
            $('<span>').addClass('filter-label').html(table_filter_label),
            $('<select>').attr('id',filter['name']+'-selector'));
          var $selector = $filter.find('#'+filter['name']+'-selector');
          $.each(filter.options, function(idx, option){
            var text = $.i18n.map[filter['name']+'_selector_'+option] ? $.i18n.map[filter['name']+'_selector_'+option] : option; 
            $selector.append($('<option>').val(option).text(text));
          });
         
          if(filter['filter_col'] && filter['alias']){
            var aliasTbl = filter['alias'];
            $.fn.dataTableExt.afnFiltering.push(
	      function( oSettings, aData, iDataIndex ) {
                if (oSettings.sInstance !== thisObj.options.id)
                  return true;
                var selectorVal =  $selector.val();
                if(aliasTbl[selectorVal]){
                  return aliasTbl[selectorVal] == aData[filter['filter_col']];
                }
                return true;
            });
          }
          $selector.change( function() { thisObj.table.fnDraw(); } );
        });
      }      

      var $searchBar = this.element.find('#'+this.options.id+'_filter');
      var refresh = this.options.text.search_refresh ? this.options.text.search_refresh : search_refresh;
      $searchBar.append(
        $('<a>').addClass('table-refresh').attr('href','#').text(refresh).click(function(){
          thisObj.refreshTable();
        }));
      
      var filterArr = [];
      thisObj.element.find('.euca-table-filter').each(function(){ filterArr.push($(this));});
      thisObj.element.find('.dataTables_filter').each(function(){ filterArr.push($(this));});
      var $wrapper = $('<div class="table-filter-wrapper"/>');
      $(filterArr).each(function(){$wrapper.append($(this).clone(true));}); 
      $wrapper.insertAfter(filterArr[filterArr.length-1]);
      $(filterArr).each(function(){$(this).remove();});
    },   

    // args.txt_create (e.g., Create new key)
    // args.txt_found ('e.g., 12 keys found)
    _decorateTopBar : function(args) {
      var thisObj = this; // ref to widget instance
      $tableTop = this.element.find('.table_' + this.options.id + '_top');
      $tableTop.addClass('euca-table-length');
      $tableTop.append(
        $('<div>').addClass('euca-table-add').append(
          $('<a>').attr('id','table-'+this.options.id+'-new').addClass('button').attr('href','#').text(thisObj.options.text.create_resource)),
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

      //var itemsList = thisObj.options.menu_actions();
      $.contextMenu({
            selector: '#more-actions-'+this.options.id,
            trigger: "left",
            build: function(trigger, e) {
                var itemsList = thisObj.options.menu_actions();
              return {
                items: itemsList,
              };
            }
          });
      // TODO: init menu in deactive state
      $('#more-actions-'+this.options.id).contextMenu(false);

      return $menuDiv;
    },

    _decorateLegend : function (args) {
      var thisObj = this;

      if(!thisObj.options.legend)
        return;

      $legend = $('<div>').attr('id',thisObj.options.id+'-legend'); 

      //$legend = thisObj.element.find('#'+thisObj.options.id+'-legend');
      $legend.addClass('table-legend');
      $legend.append($('<span>').html(legend_label));

      $.each(thisObj.options.legend, function(idx, val){
        var domid = 'legend-'+thisObj.options.id +'-'+val;
        var text = $.i18n.map[thisObj.options.id+'_legend_'+val] ? $.i18n.map[thisObj.options.id+'_legend_'+val] : val;
        $legend.append($('<span>').addClass('table-legend-item').attr('id',domid).html(text));
      });

      thisObj.element.append($legend);
    },

    _addActions : function (args) {
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
          thisObj._activateMenu();
        } else {
          for ( i = 0; i<rows.length; i++ ) {
            cb = rows[i].firstChild.firstChild;
            if ( cb != null ) cb.checked = false;
          }
          // deactivate action menu
          thisObj._deactivateMenu();
        }
      });
      //TODO: add hover to select all
      $checkbox.parent().hover( function () {
        //TODO: add action here
      });
    },

    _countSelectedRows : function () {
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

/**** Public Methods ****/
    // this reloads data and refresh table
    refreshTable : function() {
      this.table.fnReloadAjax();
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
    },

/**** End of Public Methods ****/ 
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
