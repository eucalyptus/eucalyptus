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
  $.widget('eucalyptus.eucatable_bb', {
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
      $('html body').eucadata('setDataNeeds', thisObj.options.data_deps);
      thisObj.$vel = $('<div class="visual_search" style="margin-top:-2px;width:90%;display:inline-block"></div>');

      var dtArg = this._getTableParam();

      thisObj.tableArg = dtArg;
      this.sAjaxSource = dtArg.sAjaxSource;

      // REQUIRE: SEARCH CONFIG
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
          // for displaying loader message on initial page load
          thisObj.bbdata.isLoaded = thisObj.source.hasLoaded();
          thisObj.bbdata.listenTo(thisObj.source, 'initialized', function() {
            thisObj.bbdata.trigger('initialized');
          });

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

          thisObj.bbdata.on('change add remove reset', function() {
            thisObj.refreshTable.call(thisObj)
          });

          if(thisObj.options.filters){
            var filterstring = '';
            $.each(thisObj.options.filters, function(idx, filter){
              if (filter['default']) {
                // concat filters to search on multiple facets at once
                filterstring += ' ' + filter['name'] + ': ' + filter['default'];
              } // not sure what to do if !default - this seems to work for everything we want 
            });
            if(filterstring != '') {
              filterstring.replace(/^\s+|\s+$/g,'');
              thisObj.vsearch.searchBox.setQuery(filterstring);
              thisObj.vsearch.searchBox.searchEvent($.Event('keydown'));
            }
          }

        // REQUIRE: LANDING PAGE
        require(['./views/landing_pages/landing_page_' + thisObj.options.id], function(page){

          // INITIALIZE LANDINGE PAGE WITH THIS RESOURCE ID
          thisObj.landing_page = new page({
             id: thisObj.options.id,
             collection: thisObj.searchConfig.records,
          });
          // ELEMENT USED TO DATATABLE'S HTML, BUT NOW RECIEVE RIVETS TEMPLATE
          thisObj.element = thisObj.landing_page.get_element();

          // DECORATE ELEMENT WITH TITLE, SEARCH BAR, ACTION BUTTONS, PAGE, ETC
          var $header = thisObj._decorateHeader();
          thisObj._decorateSearchBar();
          thisObj._decorateTopBar();
          thisObj._decorateActionMenu();
          thisObj._decorateLegendPagination();

          // TEMP. SOL: THIS IS TO PROVIDE THE TOTAL NUMBER OF ITEMS ON THIS LANDING PAGE - KYO 081613
          $('#table_' + thisObj.options.id + '_count').text($.i18n.prop(thisObj.options.text.resource_found, thisObj.searchConfig.records.length));

          console.log("EUCATALBE_BB: FINISHED DECORATION");

          thisObj.searchConfig.records.on('change add remove reset', function() {
            // THIS LISTENER SHUOLD BE SET INTERNALLY IN THE LANDING PAGE INSTANCE - KYO 080613
            thisObj.landing_page.refresh_view();     
          });
 
         console.log("EUCATALBE_BB: FINISHED SETUP OF LANDING PAGE RIVETS TEMPLATE");
 
        });  // END OF REQUIRE: LANDING_PAGE

      });  // END OF REQUIRE: SEARCH CONFIG
      console.log("END OF EUCATALBE_BB INIT");
    },

    _create : function() {
    },

    _destroy : function() {
    },

    _getTableParam : function(args){
      var thisObj = this;
      var dt_arg = {};
      // let users override 
      $.each(thisObj.options.dt_arg, function(k,v){
        console.log(k + " " + v);
        dt_arg[k] = v;
      });
      return dt_arg;
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
      $wrapper.prepend('<div class="dataTables_filter" id="images_filter"><a class="table-refresh" style="text-decoration: none;" href="#">&nbsp;</a></div>');
      $wrapper.find('.table-refresh').click(function(){
        thisObj.refreshSource();
      });

      $wrapper.prepend(thisObj.$vel);
    },   

    // args.txt_create (e.g., Create new key)
    // args.txt_found ('e.g., 12 keys found)
    _decorateTopBar : function(args) {
      var thisObj = this; // ref to widget instance

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
      return;
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

    _glowRow : function(val, columnId){
      if(this.table == null) return;
      var selector = ':nth-child('+(columnId+1)+')';
      return false;
    },

    _removeGlow : function(val, columnId){
      if(this.table == null) return;
      var selector = ':nth-child('+(columnId+1)+')';
      return false;
    },

/**** Public Methods ****/
    // this reloads data and refresh table
    refreshTable : function() {
      return;
    },

    // Force a refresh of the underlying data source.
    refreshSource : function() {
      // Force a fetch from backbone
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
      return;
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
     var thisObj = this;
     return thisObj.landing_page.get_checked_items_for_datatables(thisObj.tableArg.sAjaxSource, columnIdx);
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
