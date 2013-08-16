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
        data_deps: ['scalinggrps', 'scalingpolicys', 'alarms', 'launchconfigs'],
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'scalinggrp',
          "aaSorting": [[ 7, "desc" ]],
          "aoColumnDefs": [
            {
              "aTargets" : [0],
              "bSortable": false,
              "mData": function(oObj) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell"
            },
            {
              "aTargets" : [1],
              "mRender": function(data){
                 return eucatableDisplayColumnTypeTwist(data, data, 255);
              },
              "mData": function(source){
                 return source.name;
              },
            },
            { 
              "aTargets" : [2],
              "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "launch_config_name" 
            },
            /*
            {
              "aTargets" : [3],
              "mData": function(oObj) { 
                return 'some graph';
              }
            },
            */
            {
              "aTargets" : [3],
              "mData": function(oObj) { 
                return oObj.instances?oObj.instances.length:0;
              }
            },
            { 
              "aTargets" : [4],
              "mData": "desired_capacity" 
            },
            {
              "aTargets" : [5],
              "mData": function(oObj) { 
                return 'All healthy';
              }
            },
            {
              "bVisible": false,
              "aTargets":[6],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "name",
            },
          ],
        },
        text : {
          header_title : scaling_h_title,
          create_resource : scaling_create,
          resource_found : 'scaling_found',
          resource_search : scaling_search,
          resource_plural : scaling_plural,
        },
        expand_callback : function(row){ // row = [col1, col2, ..., etc]
          return thisObj._expandCallback(row);
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        menu_click_create : function (args) { thisObj._createAction() },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $scalingHelp, url: help_scaling.landing_content_url});
          $('#scaling-topselector').toggle();
        }
      });
      this.tableWrapper.appendTo(this.element);
      $(this.element).prepend($('#scaling-topselector', $wrapper));
    },

    _create : function() { 
      var thisObj = this;
    },

    _createAction : function() {
      require(['app'], function(app) {
        if(true || app.data.launchconfigs.length < 1) {
          // show dialog instead
          app.dialog('no_lc_alert', new Backbone.Model(
            {
              message: app.msg("create_scaling_group_no_launchconfigs_message"), 
              linkTarget: "#newlaunchconfig", 
              linkText: app.msg("create_scaling_group_no_launchconfigs_link"), 
              //title: app.msg("create_scaling_group_no_launchconfigs_title")
            }
          ));
        } else {
          window.location = '#newscalinggroup';
        }
      });
    },

    _destroy : function() {
    },

    _expandCallback : function(row){ 
      var $el = $('<div />');
      require(['app', 'views/expandos/scaling'], function(app, expando) {
         new expando({el: $el, model: app.data.scalingGroups.get($('<div>').html(row[6]).text()) });
      });
      return $el;
    },

    _createMenuActions : function() {
      var thisObj = this;
      selectedScaling = thisObj.baseTable.eucatable('getSelectedRows', 1);
      var itemsList = {};

      (function(){
        itemsList['quick'] = { "name": scaling_action_quick, callback: function(key, opt) {;}, disabled: function(){ return true;} } 
//        itemsList['suspend'] = { "name": scaling_action_suspend, callback: function(key, opt) {;}, disabled: function(){ return true;} }
//        itemsList['resume'] = { "name": scaling_action_resume, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['edit'] = { "name": scaling_action_edit, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['delete'] = { "name": scaling_action_delete, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['manage_instances'] = { "name": scaling_action_manage, callback: function(key, opt) {;}, disabled: function(){ return true;} }
      })();

      if ( selectedScaling.length === 1) {
        itemsList['quick'] = {"name":scaling_action_quick, callback: function(key, opt){ thisObj._dialogAction('quickscaledialog', selectedScaling); }}
//        itemsList['suspend'] = {"name":scaling_action_suspend, callback: function(key, opt){ thisObj._dialogAction('suspendscalinggroup', selectedScaling); }}
//        itemsList['resume'] = {"name":scaling_action_resume, callback: function(key, opt){ thisObj._dialogAction('resumescalinggroup', selectedScaling); }}
        itemsList['edit'] = {"name":scaling_action_edit, callback: function(key, opt){ thisObj._dialogAction('scalinggroupeditproperties', selectedScaling); }}
        itemsList['manage_instances'] = { "name": scaling_action_manage, callback: function(key, opt) { thisObj._dialogAction('scalinggroupmanageinstances', selectedScaling);}}
      }

      if ( selectedScaling.length >= 1) {
        itemsList['delete'] = {"name":scaling_action_delete, callback: function(key, opt){ thisObj._dialogAction('deletescalinggroup', selectedScaling); }}
      }

      return itemsList;
    },

    _dialogAction: function(dialog, selectedScaling) {
        require(['underscore', 'app'], function(_, app) {
            var sgrps = new Backbone.Collection();
            _.each(selectedScaling, function(s) { sgrps.push(app.data.scalinggrp.get(s)); });
            app.dialog(dialog, sgrps);
        });
    }
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
