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
  $.widget('eucalyptus.scaling', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    releaseDialog : null,
    allocateDialog : null,
    associateDialog : null,
    disassociateDialog : null,
    associateBtnId : 'scaling-associate-btn',

    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #scalingTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_scaling)));
      var $scalingTable = $wrapper.children().first();
      var $scalingHelp = $wrapper.children().last();
      this.baseTable = $scalingTable;
      this.tableWrapper = $scalingTable.eucatable({
        id : 'scaling', // user of this widget should customize these options,
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "bProcessing": true,
          "bServerSide": true,
          "sAjaxDataProp": function(json) {
            return json;
          },
          "sAjaxSource": 'scalinggrp',
          "fnServerData": function (sSource, aoData, fnCallback) {
                data = $('html body').eucadata('get', sSource);
                fnCallback(data);
          },
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
            { "mDataProp": "launch_config_name" },
            {
              "fnRender": function(oObj) { 
                return 'some graph';
              }
            },
            {
              "fnRender": function(oObj) { 
                return oObj.aData.instances.length;
              }
            },
            { "mDataProp": "desired_capacity" },
            {
              "fnRender": function(oObj) { 
                return 'All healthy';
              }
            }
          ],
        },
        text : {
          header_title : scaling_h_title,
          create_resource : scaling_create,
          resource_found : 'scaling_found',
          resource_search : scaling_search,
          resource_plural : scaling_plural,
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        menu_click_create : function (args) { thisObj.createAction() },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $scalingHelp, url: help_scaling.landing_content_url});
        }
      });
      this.tableWrapper.appendTo(this.element);
      $('html body').eucadata('addCallback', 'scalinggrp', 'scalinggrp-landing', function() {
        thisObj.tableWrapper.eucatable('redraw');
      });
    },

    _create : function() { 
      var thisObj = this;
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      var thisObj = this;
      selectedScaling = thisObj.baseTable.eucatable('getSelectedRows', 3);
      var itemsList = {};

      (function(){
        itemsList['quick'] = { "name": scaling_action_quick, callback: function(key, opt) {;}, disabled: function(){ return true;} } 
        itemsList['suspend'] = { "name": scaling_action_suspend, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['resume'] = { "name": scaling_action_resume, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['edit'] = { "name": scaling_action_edit, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['delete'] = { "name": scaling_action_delete, callback: function(key, opt) {;}, disabled: function(){ return true;} }
      })();

      // add associate
      /*
      if ( selectedScaling.length == 1 && selectedScaling[0] == 'unassigned' ){
        itemsList['associate'] = { "name": scaling_action_associate, callback: function(key, opt) { thisObj._associateAction(); } }
      }
      if ( selectedScaling.length > 0 ){
        if ( onlyInArray('assigned', selectedScaling) )
        itemsList['disassociate'] = { "name": scaling_action_disassociate, callback: function(key, opt) { thisObj._disassociateAction(); } }

        itemsList['release'] = { "name": scaling_action_release, callback: function(key, opt) { thisObj._releaseAction(); } }
      }
      */
      return itemsList;
    }
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
