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
      data_deps : null,
      dt_arg : null,
      text : {
        header_title : '',
        search_refresh : '',
        create_resource : '',
        extra_button : '',
        resource_found : '',
        action : '',
      },
      menu_actions : null, // e.g., TODO: add help
      context_menu_actions : null,
      draw_cell_callback : null,  // if we want to customize how the cell is drawn (e.g., map EMI to manifest)
                                  // {column: 3, callback: function(){ } }
      expand_callback : null,
      filters : null, // e.g., [{name: "volume_state", options: ["available","attached","attaching"], filter_col: 8, alias: {"detached":"available" }}]
      legend : null, // e.g., ['available', 'attaching', 'attached', ...]
      show_only : null, // e.g, {filter_value: 'machine', filter_col: 7}
    },

    table : null, // jQuery object to the table
    tableArg : null, 
    refreshCallback : null,

    _init : function() {
      var thisObj = this; // 

      if(thisObj.options['hidden']){
        return;
      }
      thisObj.$vel = $('<div class="visual_search" style="margin-top:-2px;width:90%;display:inline-block"></div>');
      // add draw call back
      $.fn.dataTableExt.afnFiltering = []; /// clear the filtering object

      var dtArg = this._getTableParam();

      thisObj.tableArg = dtArg;
      this.sAjaxSource = dtArg.sAjaxSource;
      this.table = this.element.find('table').dataTable(dtArg);
      var $header = this._decorateHeader();
      this._decorateSearchBar();
      this._decorateTopBar();
      this._decorateActionMenu();
      this._decorateLegendPagination();
      this._addActions();
      if ( thisObj.options.show_only && thisObj.options.show_only.length>0 ) {
        $.each(thisObj.options.show_only, function(idx, filter){
          $.fn.dataTableExt.afnFiltering.push(
	    function( oSettings, aData, iDataIndex ) {
              if (oSettings.sInstance !== thisObj.options.id)
                return true;
              return filter.filter_value === aData[filter.filter_col];
            });
        });
      }

// removed callback that causes the table to auto-refresh from data. Instead
// tables get updated automatically by data pushed from eucadata, in each landing page
//      thisObj.refreshCallback = runRepeat(function(){ return thisObj._refreshTableInterval();}, (TABLE_REFRESH_INTERVAL_SEC * 1000), false);
//      tableRefreshCallback = thisObj.refreshCallback;
      this._refreshTableInterval();
      $('html body').eucadata('setDataNeeds', thisObj.options.data_deps);
      require(['app','views/searches/' + dtArg.sAjaxSource, 'visualsearch'], function(app, searchConfig, VS) {
        var target = dtArg.sAjaxSource === 'scalinggrp' ? 'scalingGroups' : dtArg.sAjaxSource == 'launchconfig' ? 
            'launchConfigs' : dtArg.sAjaxSource;
        var source = app.data[target];
        if (typeof source === 'undefined') {
          throw new Error("No property '" + target + " on app.data");
        }
        thisObj.source = source;
        thisObj.searchConfig = new searchConfig(source);
        thisObj.bbdata = thisObj.searchConfig.filtered;
        thisObj.vsearch = VS.init({
            container : thisObj.$vel,
            showFacets : true,
            query     : '',
            callbacks : {
                search       : thisObj.searchConfig.search,
                facetMatches : thisObj.searchConfig.facetMatches,
                valueMatches : thisObj.searchConfig.valueMatches
            }
        });
        if(thisObj.options.filters){
          $.each(thisObj.options.filters, function(idx, filter){
            if (filter['default']) {
              thisObj.vsearch.searchBox.value(filter['name']+": "+filter['default'])
            }
          });
        }
        thisObj.bbdata.on('change add remove reset', function() {
            console.log('bbdata refresh');
            thisObj.refreshTable.call(thisObj)
        });
        thisObj.refreshTable();
      });
    },

    _create : function() {
    },

    _destroy : function() {
    },

    _getTableParam : function(args){
      var thisObj = this;
      var dt_arg = {};
      dt_arg["bProcessing"] = true;
      // This was disabling sorting. I'm not sure why this was there to start with, so
      // let's leave this like this for now with this comment. - dak
      //dt_arg["bServerSide"] = true;
      dt_arg["bAutoWidth"] = false;
      dt_arg["sPaginationType"] = "full_numbers",
      dt_arg['fnDrawCallback'] = function( oSettings ) {
        thisObj._drawCallback(oSettings);
      }
      dt_arg['sAjaxDataProp'] = 'aaData',
      /*
      dt_arg["fnServerData"] = function (sSource, aoData, fnCallback) {
	    require(['models/'+sSource], function(CollectionImpl) {
		var collection = new CollectionImpl();
		collection.on('reset', function() {
		   var data = {};
                   data.aaData = collection.toJSON();
                   console.log('DISPLAY',data.aaData);
                   data.iTotalRecords = data.aaData.length;
                   data.iTotalDisplayRecords = data.aaData.length;
		   fnCallback(data);
		});
		collection.fetch();
	    });
         }
      */
      dt_arg['fnServerData'] = function (sSource, aoData, fnCallback) {
        var data = {};
        data.aaData = thisObj.bbdata ? thisObj.bbdata.toJSON() : [];
        data.iTotalRecords = data.aaData.length;
        data.iTotalDisplayRecords = data.aaData.length;
        fnCallback(data);
      }

      dt_arg['fnInitComplete'] = function( oSettings ) {
        oSettings.oLanguage.sZeroRecords = $.i18n.prop('resource_no_records', thisObj.options.text.resource_plural);
        $emptyDataTd = thisObj.element.find('table tbody').find('.dataTables_empty');
        if( $emptyDataTd && thisObj.options.menu_click_create ){
          var $createNew = $('<a>').attr('href','#').text(thisObj.options.text.create_resource);
          $createNew.click( function() { 
            thisObj.options.menu_click_create(); 
            $('html body').trigger('click', 'create-new');
            return false;
          });
          text = $emptyDataTd.html();
          $emptyDataTd.html('');
          $emptyDataTd.append($.i18n.prop('resource_empty_data', thisObj.options.text.resource_plural), '&nbsp;', $createNew);
        }
      }
      var sDom = '<"table_'+ this.options.id + '_header">';
      if(thisObj.options.filters){
        $.each(thisObj.options.filters, function(idx, filter){
          var name = filter['name']+'-filter';
          sDom += '<"#'+name+'">';
        });
        $.fn.dataTableExt.afnFiltering = [];
      }
      sDom += 'f<"clear"><"table_'+thisObj.options.id+'_top">rt';
      sDom += 'p<"clear">';
      dt_arg['sDom'] = sDom;  
      if(!dt_arg['oLanguage']) 
        dt_arg['oLanguage'] = {};

      dt_arg.oLanguage['sProcessing'] = null;
      dt_arg.oLanguage['sLoadingRecords'] =  please_wait_loading_data;
      dt_arg.oLanguage['sZeroRecords'] = please_wait_loading_data;
      dt_arg.oLanguage['sSearch'] = "";
      dt_arg.oLanguage['sEmptyTable'] = $.i18n.prop('resource_empty_data', thisObj.options.text.resource_plural);
      // let users override 
      $.each(thisObj.options.dt_arg, function(k,v){
        dt_arg[k] = v;
      });
      return dt_arg;
    },

    _drawCallback : function(oSettings) {
      var thisObj = this;
      $('#table_' + this.options.id + '_count').text($.i18n.prop(thisObj.options.text.resource_found, oSettings.fnRecordsDisplay()));
      this.element.find('table thead tr').each(function(index, tr){
        var $checkAll = $(tr).find(':input[type="checkbox"]');
        if(! $checkAll.data('events') || !('click' in $checkAll.data('events'))){
          $checkAll.unbind('click').bind('click', function (e) {
            var checked = $(this).is(':checked');
            thisObj.element.find('table tbody tr').each(function(innerIdx, innerTr){
              if(checked)
                $(innerTr).addClass('selected-row');
              else
                $(innerTr).removeClass('selected-row');
            });
          });
        }
      }); 

      this.element.find('table tbody').find('tr').each(function(index, tr) {
        // add custom td handlers
        var $currentRow = $(tr);
        var $expand = null;
        if(thisObj.options.expand_callback && thisObj.table){
          var allTds = thisObj.table.fnGetTds($currentRow[0]);      
          var row = [];
          var i =0; 
          $(allTds).each(function(){ 
            row[i++] = $(this).html();
          }); 
        }
        
        if(!$currentRow.data('events') || !('click' in $currentRow.data('events'))){
          $currentRow.unbind('click').bind('click', function (e) {
            if($(e.target).is('a') && $(e.target).hasClass('twist') && thisObj.options.expand_callback){
              if(!$currentRow.next().hasClass('expanded')){
                // Generate the expanded area
                $expand = thisObj.options.expand_callback(row);

                if(!$expand.hasClass('expanded-row-inner-wrapper'))
                  $expand.addClass('expanded-row-inner-wrapper');
                if($expand && $expand.length > 0){
                  $currentRow.after($('<tr>').addClass('expanded').append(
                                  $('<td>').attr('colspan', $currentRow.find('td').length).append(
                                    $expand)));
                  $currentRow.find('a.twist').addClass('expanded');
                }
              }else{
                var $twist = $currentRow.find('a.twist');
                $currentRow.next().toggle();
                if ($twist.hasClass('expanded')) {
                    $twist.removeClass('expanded');
                } else {
                    $twist.addClass('expanded');
                }
              }
            }else{
              var $selectedRow = $currentRow; 
              var $rowCheckbox = $selectedRow.find('input[type="checkbox"]');
              if($rowCheckbox && $rowCheckbox.length > 0){
                $selectedRow.toggleClass('selected-row');
                if($selectedRow.hasClass('selected-row'))
                  $rowCheckbox.attr('checked', true);
                else
                  $rowCheckbox.attr('checked', false);
              }
            }  
          // checked/uncheck on checkbox
            thisObj._onRowClick();
            thisObj._trigger('row_click', e);
          });
        }

        if (DEPRECATE && thisObj.options.context_menu_actions) {
          rID = 'ri-'+S4()+S4();
          $currentRow.attr('id', rID);
          $.contextMenu({
            selector: '#'+rID,
            build: function(trigger, e) {
              if(thisObj._countSelectedRows() <= 0)
                return null;
              return { items: thisObj.options.context_menu_actions()};
            }
          });
        }
      });
      this.element.qtip();
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
      //$menu.contextMenu(true);
    },

    _deactivateMenu : function() {
      $menu = $('#more-actions-'+this.options.id);
      $menu.addClass("inactive-menu");
      //$menu.contextMenu(false);
    },

    // args.title = title in the header (e.g.,'Manage key pairs');
    _decorateHeader : function(args) {
      var thisObj = this;
      $header = this.element.find('.table_' + this.options.id + '_header');
      $header.addClass('euca-table-header');
      $header.append(
        $('<span>').text(thisObj.options.text.header_title).append(
          $('<div>').addClass('help-link').append(
            $('<a>').attr('href','#').html('&nbsp;').click( function(evt){
              thisObj._trigger('help_click', evt);
            }))));
      return $header;
    },

    // args.refresh = text 'Refresh'
    _decorateSearchBar : function(args) {
      var thisObj = this; // ref to widget instance

      var $searchBar = this.element.find('#'+this.options.id+'_filter');
      $searchBar.find(":input").watermark(this.options.text.resource_search);
      var refresh = this.options.text.search_refresh ? this.options.text.search_refresh : search_refresh;
      $searchBar.append(
        $('<a>').addClass('table-refresh').attr('href','#').text(refresh).click(function(){
          thisObj.refreshTable();
        }));
      
      var filterArr = [];
      thisObj.element.find('.euca-table-filter').each(function(){ filterArr.push($(this));});
      thisObj.element.find('.dataTables_filter').each(function(){ filterArr.push($(this));});
      var $wrapper = $('<div class="table-filter-wrapper clearfix"/>');
      $(filterArr).each(function(){$wrapper.append($(this).clone(true));}); 
      $wrapper.insertAfter(filterArr[filterArr.length-1]);
      $(filterArr).each(function(){$(this).remove();});

      $wrapper.empty();
      $wrapper.prepend('<div class="dataTables_filter" id="images_filter"><a class="table-refresh" href="#">Refresh</a></div>');
      $wrapper.find('.table-refresh').click(function(){
        thisObj.refreshSource();
      });

      $wrapper.prepend(thisObj.$vel);
    },   

    // args.txt_create (e.g., Create new key)
    // args.txt_found ('e.g., 12 keys found)
    _decorateTopBar : function(args) {
      var thisObj = this; // ref to widget instance
      $tableTop = this.element.find('.table_' + this.options.id + '_top');
      $tableTop.addClass('euca-table-length clearfix');
      $createResourceDiv =  $('<div>').addClass('euca-table-add');
      if ( thisObj.options.text.create_resource )
        $createResourceDiv.append(
          $('<a>').attr('id','table-'+this.options.id+'-new').addClass('button').attr('href','#').text(thisObj.options.text.create_resource));
      $extraResourceDiv =  $('<div>').addClass('euca-table-extra');
      if ( thisObj.options.text.extra_resource )
        $extraResourceDiv.append(
          $('<a>').attr('id','table-'+this.options.id+'-extra').addClass('button').attr('href','#').text(thisObj.options.text.extra_resource));
      $tableTop.append(
        $createResourceDiv,
        $extraResourceDiv,
        $('<div>').addClass('euca-table-action actionmenu'),
        $('<div>').addClass('euca-table-size').append(
          $('<span>').attr('id','table_' + this.options.id + '_count'),
          '&nbsp;', showing_label,
          $('<a>').attr('href','#').addClass('show selected').text('10'),
          '&nbsp;|&nbsp;', // TODO: don't use nbsp; in place for padding!
          $('<a>').attr('href','#').addClass('show').text('25'),
          '&nbsp;|&nbsp;',
          $('<a>').attr('href','#').addClass('show').text('50'),
          '&nbsp;|&nbsp;',
          $('<a>').attr('href','#').addClass('show').text('100')));

      $tableTop.find('a.show').click(function () {
        if($(this).hasClass('selected'))
          return;
        $(this).parent().children('a').each( function() {
          $(this).removeClass('selected');
        });
        thisObj.table.fnSettings()._iDisplayLength = parseInt($(this).text().replace('|',''));
        thisObj.table.fnDraw();
        $(this).addClass('selected');
      });

      // add action to create new
      this.element.find('#table-' + this.options.id + '-new').click(function(e) {
        thisObj._trigger('menu_click_create', e); // users of the table should listen to
        $('html body').trigger('click', 'create-new');
        return false;
      });
      this.element.find('#table-' + this.options.id + '-extra').click(function(e) {
        thisObj._trigger('menu_click_extra', e); // users of the table should listen to
        $('html body').trigger('click', 'create-extra');
        return false;
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

      $menuDiv.append(
          $('<a>').attr('id','more-actions-'+this.options.id).addClass("button").addClass("inactive-menu").attr('href','#').text(txt_action).click(function(e){
            if($(this).hasClass('inactive-menu') && !$menuDiv.find('ul').hasClass('toggle-on'))
              return false;
            var menu_actions = thisObj.options.menu_actions();
            
            var $ul = $menuDiv.find('ul');
            if($ul && $ul.length > 0){
              $ul.slideToggle('fast');
              $ul.toggleClass('toggle-on'); 
            }else{
              $ul = $('<ul>').addClass('toggle-on');      
              $.each(menu_actions, function (key, menu){
                $ul.append(
                  $('<li>').attr('id', thisObj.options.id + '-' + key).append(
                    $('<a>').attr('href','#').text(menu.name).unbind('click').bind('click', menu.callback)));
              });
              $menuDiv.append($ul);
            }
            $('html body').trigger('click','table:instance');
            if($ul.hasClass('toggle-on')){
              $.each(menu_actions, function (key, menu){
                if(menu.disabled){
                  $ul.find('#'+thisObj.options.id + '-'+key).addClass('disabled').find('a').removeAttr('href').unbind('click');
                }else{
                  $ul.find('#'+thisObj.options.id + '-'+key).removeClass('disabled').find('a').attr('href','#').unbind('click').bind('click',menu.callback);
                }
              }); 
              $('html body').eucaevent('add_click', 'table:instance', e);
            }else
              $('html body').eucaevent('del_click', 'table:instance');
              return false;
          }));
      return $menuDiv;
    },

    
    _decorateLegendPagination : function (args) {
      var thisObj = this;
      var $wrapper = $('<div>').addClass('legend-pagination-wrapper clearfix');
      thisObj.element.find('.dataTables_paginate').wrapAll($wrapper); 
      if(thisObj.options.legend){
        $legend = $('<div>').attr('id',thisObj.options.id+'-legend'); 

        $legend.addClass('table-legend clearfix');
        var $itemWrapper = $('<div>').attr('class','legend-items-wrapper');
        $.each(thisObj.options.legend, function(idx, val){
          var itemCls = 'legend-'+val;
          textId = thisObj.options.id+'_legend_'+val.replace('-','_');
          var text = $.i18n.map[textId] ? $.i18n.map[textId] : val;
          $itemWrapper.append($('<span>').addClass('legend-item').addClass(itemCls).text(text));
        });
        $legend.append($itemWrapper);
        thisObj.element.find('.legend-pagination-wrapper').prepend($legend);
      }
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

    _glowRow : function(val, columnId){
      var selector = ':nth-child('+(columnId+1)+')';
      var rows = this.table.fnGetNodes();
      for ( i in rows){
        $td = $(rows[i]).find(selector);
        if ($td.html() && $td.html().indexOf(val) >= 0) {
          $td.parent().addClass('glow');
          return true;
        }
      }
      return false;
    },

    _removeGlow : function(val, columnId){
      var selector = ':nth-child('+(columnId+1)+')';
      var rows = this.table.fnGetNodes();
      for ( i in rows){
        $td = $(rows[i]).find(selector);
        if (val === $td.text()) {
          $td.parent().removeClass('glow');
          return true;
        }
      }
      return false;
    },

    _refreshTableInterval : function() {
      var tbody = this.element.find('table tbody'); 
      if(tbody.find('tr.selected-row').length > 0 || tbody.find('tr.expanded').length > 0 || tbody.find('tr.glow').length>0 )
        return;
      if($('html body').eucadata('countPendingReq') > MAX_PENDING_REQ)
        return;
      if(! $('html body').eucadata('isEnabled'))
        return;
      this.table.fnReloadAjax(undefined, undefined, true);
    },

/**** Public Methods ****/
    // this reloads data and refresh table
    refreshTable : function() {
      if($('html body').eucadata('countPendingReq') > MAX_PENDING_REQ)
        return;
      if(! $('html body').eucadata('isEnabled'))
        return;
      var oSettings = this.table.fnSettings();
      var tbody = this.element.find('table tbody'); 
      var selected = tbody.find('tr.selected-row');
      var expanded = tbody.find('tr.expanded');

      this.table.fnReloadAjax(this.table.oSettings, undefined, function() {
        if (selected != undefined && selected.length > 0) {
          $.each(selected, function(idx, row) {
            $(row).toggleClass('selected-row');
            $($(row).find('input[type="checkbox"]')[0]).attr('checked', true);
          });
        }
        if (expanded != undefined && expanded.length > 0) {
        }
       
        var $checkAll = this.table.find('thead').find(':input[type="checkbox"]');
        var checked = $checkAll.is(':checked');
        if(checked)
          $checkAll.trigger('click.datatable');
      });
    },

    // Force a refresh of the underlying data source.
    refreshSource : function() {
      // Force a fetch from backbone
      console.log('Fetch source');
      this.source.fetch();
    },

    glowRow : function(val, columnId) {
      var thisObj = this;
      var cId = columnId || 1;
      var token = null; 
      var counter = 0;
      token = runRepeat(function(){
        if ( thisObj._glowRow(val, cId)){
          cancelRepeat(token);
          setTimeout(function(){ thisObj._removeGlow(val, cId);}, GLOW_DURATION_SEC*1000); // remove glow effect after 7 sec
        } else if (counter++ > 30){ // give up glow effect after 60 seconds
          cancelRepeat(token);
        }
      }, 2000);
    },

    redraw : function() {
      this._refreshTableInterval();
    },

    // (optional) columnIdx: if undefined, returns matrix [row_idx, col_key]
    getSelectedRows : function (columnIdx) {
      var dataTable = this.table;
      if ( !dataTable )
     
      var $checkAll = this.table.find('thead').find(':input[type="checkbox"]');
      var checked = $checkAll.is(':checked');
      if(checked)
        $checkAll.trigger('click');
    },

    glowRow : function(val, columnId) {
      var thisObj = this;
      var cId = columnId || 1;
      var token = null; 
      var counter = 0;
      token = runRepeat(function(){
        if ( thisObj._glowRow(val, cId)){
          cancelRepeat(token);
          setTimeout(function(){ thisObj._removeGlow(val, cId);}, GLOW_DURATION_SEC*1000); // remove glow effect after 7 sec
        } else if (counter++ > 30){ // give up glow effect after 60 seconds
          cancelRepeat(token);
        }
      }, 2000);
    },

    // (optional) columnIdx: if undefined, returns matrix [row_idx, col_key]
    getSelectedRows : function (columnIdx) {
      var dataTable = this.table;
      if ( !dataTable )
        return [];
      var rows = dataTable.fnGetVisibleTrNodes();
      var selectedRows = [];
      for ( i = 0; i<rows.length; i++ ) {
        cb = rows[i].firstChild.firstChild;
        if ( cb != null && cb.checked == true ) {
          if(columnIdx)
            selectedRows.push(dataTable.fnGetData(rows[i], columnIdx));
          else{
            selectedRows.push(dataTable.fnGetData(rows[i])); // returns the entire row with key, value
          }
        }
      }
      return selectedRows;
    },
    
    changeAjaxUrl : function(url){
      var thisObj = this;
      var oSettings = thisObj.table.fnSettings();
      oSettings.sAjaxSource = url;
      thisObj.refreshTable();
    }, 
    close : function() {
      ; // cancelRepeat(this.refreshCallback);
    }
/**** End of Public Methods ****/ 
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
