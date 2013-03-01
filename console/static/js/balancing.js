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
  $.widget('eucalyptus.balancing', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    releaseDialog : null,
    allocateDialog : null,
    associateDialog : null,
    disassociateDialog : null,
    associateBtnId : 'balancing-associate-btn',

    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #balancingTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_balancing)));
      var $balancingTable = $wrapper.children().first();
      var $balancingHelp = $wrapper.children().last();
      this.baseTable = $balancingTable;
      this.tableWrapper = $balancingTable.eucatable({
        id : 'balancing', // user of this widget should customize these options,
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "bProcessing": true,
          "bServerSide": true,
          "sAjaxSource": 'balancers',
          "bAutoWidth" : false,
          "sPaginationType": "full_numbers",
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell"
            },
            {
              "mDataProp": "name",
              "iDataSort": 4
            },
            { "mDataProp": "dns_name" },
            {
              "fnRender": function(oObj) { 
                return 'some graph';
              }
            },
            { 
              "asSorting" : [ 'desc', 'asc' ],
              "fnRender": function(oObj) { return formatDateTime(oObj.aData.created_time); },
              "iDataSort": 9
            }
          ],
        },
        text : {
          header_title : balancing_h_title,
          create_resource : balancing_create,
          resource_found : 'balancing_found',
          resource_search : balancing_search,
          resource_plural : balancing_plural,
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        menu_click_create : function (args) { thisObj.createAction() },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $balancingHelp, url: help_balancing.landing_content_url});
        }
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
      var thisObj = this;
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      var thisObj = this;
      selectedBalancing = thisObj.baseTable.eucatable('getSelectedRows', 3);
      var itemsList = {};

      (function(){
        itemsList['add'] = { "name": balancing_action_add, callback: function(key, opt) {;}, disabled: function(){ return true;} } 
        itemsList['remove'] = { "name": balancing_action_remove, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['edit'] = { "name": balancing_action_edit, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['delete'] = { "name": balancing_action_delete, callback: function(key, opt) {;}, disabled: function(){ return true;} }
      })();

      // add associate
      /*
      if ( selectedBalancing.length == 1 && selectedBalancing[0] == 'unassigned' ){
        itemsList['associate'] = { "name": balancing_action_associate, callback: function(key, opt) { thisObj._associateAction(); } }
      }
      if ( selectedBalancing.length > 0 ){
        if ( onlyInArray('assigned', selectedBalancing) )
        itemsList['disassociate'] = { "name": balancing_action_disassociate, callback: function(key, opt) { thisObj._disassociateAction(); } }

        itemsList['release'] = { "name": balancing_action_release, callback: function(key, opt) { thisObj._releaseAction(); } }
      }
      */
      return itemsList;
    }
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
