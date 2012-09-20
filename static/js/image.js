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
              "fnRender": function(oObj) { return '<a href="#" onClick="startLaunchWizard({image:\''+oObj.aData.id+'\'});">' + image_launch_btn +'</a>' },
            },
            { 
              "fnRender" : function(oObj) { 
                 return $('<div>').append($('<div>').addClass('twist').text(oObj.aData.name)).html();
              }
            },
            { 
              "fnRender": function(oObj) {
                 return oObj.aData.platform ? 'windows' : 'linux';
               },
            },
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
            { // idx = 9
              "bVisible": false,
              "mDataProp": "id",
            },
          ],
        },
        text : {
          header_title : image_h_title,
          resource_found : image_found,
          resource_search : image_search,
        },
        expand_callback : function(row){ // row = [col1, col2, ..., etc]
          return thisObj._expandCallback(row);
        },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, $imgHelp);
        },
        legend : ['pending','available','failed'],
        show_only : {filter_value: 'machine', filter_col: 8},
        filters : [
          /* {name:"img_ownership", options: ['all','my'], text: ['All images', 'Images owned by me'], filter_col:TBD}, */
          {name:"img_platform", options: ['all', 'linux', 'windows'], text: [launch_instance_image_table_platform_all,
launch_instance_image_table_platform_linux, launch_instance_image_table_platform_windows], filter_col:2},
          {name:"img_architect", options: ['all', 'i386','x86_64'], text: ['32 and 64 bit', '32 bit', '64 bit'], filter_col:4},
          {name:"img_type", options: ['all', 'ebs','instance-store'], text: [instance_type_selector_all, instance_type_selector_ebs, instance_type_selector_instancestore], filter_col:6},
          ],
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
    },

    _destroy : function() {
    },
    _expandCallback : function(row){ 
      var thisObj = this;
      var imgId = row[9];
      var results = describe('image');
      var image = null;
      var kernel = null;
      var ramdisk = null;
      for(i in results){
        if(results[i].id === imgId){
          image = results[i];
          break;
        }
      }
      if(!image)
        return null;
      for(i in results){
        if(results[i].id === image.kernel_id)
          kernel = results[i];
        if(results[i].id === image.ramdisk_id)
          ramdisk = results[i];
      }
      var snapshot = '';
      if(image.block_device_mapping && image.block_device_mapping['snapshot_id'])
        snapshot = image.block_device_mapping['snapshot_id'];
 
      var $wrapper = $('<div>');
      var $imgInfo = $('<ul>').addClass('image-expanded').text(image_table_expanded_machine).append(
        $('<li>').append( 
          $('<div>').addClass('expanded-value').text(image['id']),
          $('<div>').addClass('expanded-title').text(image_table_expanded_image_id)),
        $('<li>').append(
          $('<div>').addClass('expanded-value').text(snapshot),
          $('<div>').addClass('expanded-title').text(image_table_expanded_snapshot_id)),
        $('<li>').append(
          $('<div>').addClass('expanded-value').text(image['owner_id']),
          $('<div>').addClass('expanded-title').text(image_table_expanded_account)),
        $('<li>').append(
          $('<div>').addClass('expanded-value').text(image['location']),
          $('<div>').addClass('expanded-title').text(image_table_expanded_manifest)));

      var $kernelInfo = null;
      if(kernel){
        $kernelInfo = $('<ul>').addClass('kernel-expanded').text(image_table_expanded_kernel).append(
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(kernel['id']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_kernel_id)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(kernel['name']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_name)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(kernel['architecture']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_arch)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(kernel['location']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_manifest)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(kernel['description']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_desc)));
      }
      var $ramdiskInfo = null;
      if(ramdisk){
        $ramdiskInfo = $('<ul>').addClass('ramdisk-expanded').text(image_table_expanded_ramdisk).append(
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(ramdisk['id']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_ramdisk_id)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(ramdisk['name']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_name)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(ramdisk['architecture']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_arch)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(ramdisk['location']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_manifest)),
          $('<li>').append(
            $('<div>').addClass('expanded-value').text(ramdisk['description']),
            $('<div>').addClass('expanded-title').text(image_table_expanded_desc)));
      }
      $wrapper.append($imgInfo);
      if($kernelInfo)
        $wrapper.append($kernelInfo);
      if($ramdiskInfo)
        $wrapper.append($ramdiskInfo);

      return $wrapper;
    },

/**** Public Methods ****/
    close: function() {
//      this.tableWrapper.eucatable('close');
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
