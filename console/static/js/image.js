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
  $.widget('eucalyptus.image', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #imageTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_image)));
      var $imgTable = $wrapper.children().first();
      var $imgHelp = $wrapper.children().last();
      this.baseTable = $imgTable;
      this.tableWrapper = $imgTable.eucatable({
        id : 'images', // user of this widget should customize these options,
        data_deps: ['images', 'tags'],
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'image',
          "aoColumnDefs": [
            {
              // Display the checkbox button in the main table
              "bSortable": false,
              "aTargets":[0],
              "mData": function(source) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell"
            },
            { 
	      // Display the id of the image in eucatable
	      "aTargets":[1], 
              "mData": function(source){
                this_mouseover = source.id;
                this_value = source.display_id;
                return eucatableDisplayColumnTypeTwist(this_mouseover, this_value, 256);
              },
              "sClass": "wrap-content",
            },
            {
	      // Display the name of the image in eucatable
	      // Allow the name to be clickable
	      // Use 'twist' in CSS
	      "aTargets":[2], 
              "mRender" : function(data) { 
                 return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "name",
              "sClass": "wrap-content",
            }, 
            {
	      // Display the name of the image in eucatable
	      // Allow the name to be clickable
	      // Use 'twist' in CSS
	      "aTargets":[3], 
              "mRender" : function(data) { 
                 return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "id",
            }, 
            { 
	      // Display the artitecture of the image in eucatable
	      "aTargets":[4],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "architecture",
	    },
            {
	      // Display the description of the image in eucatable
	      "aTargets":[5],
	      "mRender": function(data) {
                return eucatableDisplayColumnTypeText (data, data, 30);
              },
              "mData": "description",
              "sClass": "wrap-content",
	    },
            { 
	      // Display the root device type of the image in eucatable
	      "aTargets":[6],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "root_device_type",
	    },
            {
              // Display the launch instance button for the image in eucatable
              "bSortable": false,
              "aTargets":[7],
              "sClass": "centered-cell",
              "mRender": function(data) {
	        return eucatableDisplayColumnTypeLaunchInstanceButton (data); 
	      },
              "mData": "id",
              "sWidth": 80,
            },
            {
              // Hidden column for the state of the image
              "bVisible": false,
              "aTargets":[8],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "state",
            },
            {
              // Hidden column for the type of the image
              "bVisible": false,
              "aTargets":[9],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "type",
            },
            { 
              // Hidden column for the id of the image
              "bVisible": false,
              "aTargets":[10],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "id",
            },
            { 
              // Hidden column for the platform/OS of the image
              // idx = 9
              "bVisible" : false,
              "aTargets":[11],
              "mData" : function(source) {
                return source.platform ? DefaultEncoder().encodeForHTML(source.platform) : 'linux';
              },
            },
            {
              // Hidden column for the location of the image
              "bVisible" : false,
              "aTargets":[12],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "location",
            },
            {
              // Hidden column for the ownership of the image ?
              "bVisible": false,
              "aTargets":[13],
              "mData": function(source){
                var results = describe('sgroup');
                var group = null;
                for(i in results){
                  if(results[i].name === 'default'){
                    group = results[i];
                    break;
                  }
                } 
                if(group && group.owner_id === source.ownerId)
                  return 'self'; // equivalent of 'describe-images -self'
                else
                  return 'all'; 
              },
            }
          ],
        },
        text : {
          header_title : image_h_title,
          resource_found : 'image_found',
          resource_search : image_search,
          resource_plural : image_plural,
        },
        menu_actions : function(args){
          return thisObj._createMenuActions();
        },
        expand_callback : function(row){ // row = [col1, col2, ..., etc]
          return thisObj._expandCallback(row);
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content:$imgHelp, url: help_image.landing_content_url});
        },
        show_only : [{filter_value: 'machine', filter_col: 9},{filter_value: 'available', filter_col: 8}],
        filters : [
          {name:"img_ownership", options: ['all','self'], text: [launch_instance_image_table_owner_all, launch_instance_image_table_owner_me], filter_col:13}, 
          {name:"img_platform", options: ['all', 'linux', 'windows'], text: [launch_instance_image_table_platform_all,
launch_instance_image_table_platform_linux, launch_instance_image_table_platform_windows], filter_col:11},
          {name:"img_architect", options: ['all', 'i386','x86_64'], text: ['32 and 64 bit', '32 bit', '64 bit'], filter_col:4},
          {name:"img_type", options: ['all', 'ebs','instance-store'], text: [instance_type_selector_all, instance_type_selector_ebs, instance_type_selector_instancestore], filter_col:6},
          ],
      });
      this.tableWrapper.appendTo(this.element);
      $('html body').eucadata('addCallback', 'image', 'image-landing', function() {
        thisObj.tableWrapper.eucatable('redraw');
      });
    },

    _create : function() {
    },

    _destroy : function() {
    },

    _expandCallback : function(row){ 
      var $el = $('<div />');
      require(['app', 'views/expandos/image'], function(app, expando) {
         new expando({el: $el, model: app.data.images.get(row[10])});
      });
      return $el;
    },

    _createMenuActions : function() {
      var thisObj = this;
      var images = thisObj.baseTable.eucatable('getSelectedRows');
      var itemsList = {};

      (function(){
        itemsList['tag'] = {"name":table_menu_edit_tags_action, callback: function(key, opt) {;}, disabled: function(){ return true;} };
        itemsList['launchconfig'] = {"name":images_menu_create_scaling_group_launch_config, callback: function(key,opt){;}, disabled: function() {return true;}};
      })();

      if ( images.length === 1) {
        itemsList['tag'] = {"name":table_menu_edit_tags_action, callback: function(key, opt){ thisObj._tagResourceAction(); }};
        itemsList['launchconfig'] = {"name":images_menu_create_scaling_group_launch_config, callback: function(key,opt){thisObj._startLaunchConfigWiz();} };
      };

      return itemsList;
    },

    _tagResourceAction : function(){
      var selected = this.tableWrapper.eucatable('getSelectedRows', 10);
      if ( selected.length > 0 ) {
        require(['app'], function(app) {
           app.dialog('edittags', app.data.image.get(selected[0]));
        });
       }
    },

    _startLaunchConfigWiz: function(e) {
      var selected = this.tableWrapper.eucatable('getSelectedRows', 10);
      var $container = $('html body').find(DOM_BINDING['main']);
      if (selected.length == 1) {
        require(['app'], function(app) {
          $container.maincontainer("changeSelected", e, {selected:'newlaunchconfig', filter:{image:app.data.image.get(selected[0])}});
        });
      }
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
