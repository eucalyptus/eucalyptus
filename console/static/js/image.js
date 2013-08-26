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
      this.tableWrapper = $imgTable.eucatable_bb({
        id : 'images', // user of this widget should customize these options,
        data_deps: ['images', 'tags'],
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'image',
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
//      this.tableWrapper.appendTo(this.element);
      $('html body').eucadata('addCallback', 'image', 'image-landing', function() {
        thisObj.tableWrapper.eucatable_bb('redraw');
      });
    },

    _create : function() {
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      var thisObj = this;
      var images = thisObj.baseTable.eucatable_bb('getSelectedRows');
      var itemsList = {};

      (function(){
        itemsList['tag'] = {"name":table_menu_edit_tags_action, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['launchconfig'] = {"name":image_action_launchconfig, callback: function(key, opt){;}, disabled: function(){ return true; }}
      })();

      if ( images.length === 1) {
        itemsList['tag'] = {"name":table_menu_edit_tags_action, callback: function(key, opt){ thisObj._tagResourceAction(); }}
        itemsList['launchconfig'] = {"name":image_action_launchconfig, callback: function(key, opt){ thisObj._launchConfigAction(); }}
      };

      return itemsList;
    },

    _tagResourceAction : function(){
      var selected = this.tableWrapper.eucatable_bb('getSelectedRows', 10);
      if ( selected.length > 0 ) {
        require(['app'], function(app) {
           app.dialog('edittags', app.data.image.get(selected[0]));
        });
       }
    },

    _launchConfigAction : function(){
      var image = this.tableWrapper.eucatable_bb('getSelectedRows', 10);
      this.element.newlaunchconfig({image:image});
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
