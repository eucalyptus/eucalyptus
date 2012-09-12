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
        dt_arg : {
          "sAjaxSource": "../ec2?Action=DescribeImages",
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<a href="#" onClick="startLaunchWizard(\''+oObj.aData.id+'\');">' + image_launch_btn +'</a>' },
            },
            { "mDataProp": "name" },
            {
              "fnRender": function(oObj) { 
                 return '<div class="table-row-status status-'+oObj.aData.state+'">&nbsp;</div>';
               },
              "sWidth": "20px",
              "bSearchable": false,
              "iDataSort": 6, // sort on hidden status column
            },
            { "mDataProp": "architecture" },
            { "mDataProp": "description" },
            { "mDataProp": "root_device_type" },
            {
              "bVisible": false,
              "mDataProp": "state"
            },
            {
              "bVisible": false,
              "mDataProp": "type"
            },
            {
              "bVisible": false,
              "fnRender": function(oObj) {
                 return oObj.aData.platform ? 'windows' : 'linux';
               },
            },
          ],
        },
        text : {
          header_title : image_h_title,
          resource_found : image_found,
        },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, $imgHelp);
        },
        legend : ['pending','available','failed'],
        show_only : {filter_value: 'machine', filter_col: 7},
        filters : [
          /* {name:"img_ownership", options: ['all','my'], text: ['All images', 'Images owned by me'], filter_col:TBD}, */
          {name:"img_platform", options: ['all', 'linux', 'windows'], text: [launch_instance_image_table_platform_all,
launch_instance_image_table_platform_windows, launch_instance_image_table_platform_linux], filter_col:8},
          {name:"img_architect", options: ['all', 'i386','x86_64'], text: ['32 and 64 bit', '32 bit', '64 bit'], filter_col:3},
          {name:"img_type", options: ['all', 'ebs','instance-store'], text: [instance_type_selector_all, instance_type_selector_ebs, instance_type_selector_instancestore], filter_col:5},
          ],
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
    },

    _destroy : function() {
    },

/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
