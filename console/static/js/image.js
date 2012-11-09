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
          "fnServerData": function (sSource, aoData, fnCallback) {
                $.ajax( {
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": "_xsrf="+$.cookie('_xsrf'),
                    "success": fnCallback
                });

          },
          "aoColumns": [
            { 
              "fnRender" : function(oObj) { 
                 return $('<div>').append($('<a>').addClass('twist').attr('href','#').text(oObj.aData.name)).html();
              },
            },
            { "mDataProp": "id" },
            { "mDataProp": "architecture" },
            { "mDataProp": "description" },
            { "mDataProp": "root_device_type" },
            {
              "bSortable": false,
              "sClass": "centered-cell",
              "fnRender": function(oObj) { 
                return  '<a href="#" class="button table-button" onClick="startLaunchWizard({image:\''+oObj.aData.id+'\'}); $(\'html body\').trigger(\'click\', \'create-new\'); return false;">' + image_launch_btn +'</a>' },
              "sWidth": 80,
            },
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
              "mDataProp": "id",
            },
            { // idx = 9
              "bVisible" : false,
              "fnRender" : function(oObj) {
                return oObj.aData.platform ? oObj.aData.platform : 'linux';
              }
            },
            {
              "bVisible" : false,
              "mDataProp" : "location",
            },
            {
              "bVisible": false,
              "fnRender" : function(oObj){
                var results = describe('sgroup');
                var group = null;
                for(i in results){
                  if(results[i].name === 'default'){
                    group = results[i];
                    break;
                  }
                } 
                if(group && group.owner_id === oObj.aData.ownerId)
                  return 'self'; // equivalent of 'describe-images -self'
                else
                  return 'all'; 
              }
            }
          ],
        },
        text : {
          header_title : image_h_title,
          resource_found : 'image_found',
          resource_search : image_search,
          resource_plural : image_plural,
        },
        expand_callback : function(row){ // row = [col1, col2, ..., etc]
          return thisObj._expandCallback(row);
        },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content:$imgHelp, url: help_image.landing_content_url});
        },
        show_only : [{filter_value: 'machine', filter_col: 7},{filter_value: 'available', filter_col: 6}],
        filters : [
          {name:"img_ownership", options: ['all','self'], text: [launch_instance_image_table_owner_all, launch_instance_image_table_owner_me], filter_col:11}, 
          {name:"img_platform", options: ['all', 'linux', 'windows'], text: [launch_instance_image_table_platform_all,
launch_instance_image_table_platform_linux, launch_instance_image_table_platform_windows], filter_col:9},
          {name:"img_architect", options: ['all', 'i386','x86_64'], text: ['32 and 64 bit', '32 bit', '64 bit'], filter_col:2},
          {name:"img_type", options: ['all', 'ebs','instance-store'], text: [instance_type_selector_all, instance_type_selector_ebs, instance_type_selector_instancestore], filter_col:4},
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
      var imgId = row[8];
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
      if(image.block_device_mapping){
        vol = image.block_device_mapping[getRootDeviceName(image)];
        if (vol)
          snapshot = vol['snapshot_id'];
      }
 
      var $wrapper = $('<div>');
      var $imgInfo = $('<div>').addClass('image-table-expanded-machine').addClass('clearfix').append(
                       $('<div>').addClass('expanded-section-label').text(image_table_expanded_machine).after(
                       $('<div>').addClass('expanded-section-content').addClass('clearfix').append(
                         $('<ul>').addClass('image-expanded').addClass('clearfix').append(
                           $('<li>').append( 
                             $('<div>').addClass('expanded-title').text(image_table_expanded_image_id),
                             $('<div>').addClass('expanded-value').text(image['id'])),
                           $('<li>').append(
                             $('<div>').addClass('expanded-title').text(image_table_expanded_snapshot_id),
                             $('<div>').addClass('expanded-value').text(snapshot)),
                           $('<li>').append(
                             $('<div>').addClass('expanded-title').text(image_table_expanded_account),
                             $('<div>').addClass('expanded-value').text(image['owner_id'])),
                           $('<li>').append(
                             $('<div>').addClass('expanded-title').text(image_table_expanded_manifest),
                             $('<div>').addClass('expanded-value').text(image['location'].replace('&#x2f;','/')))))));

      var $kernelInfo = null;
      if(kernel){
        $kernelInfo = $('<div>').addClass('image-table-expanded-kernel').addClass('clearfix').append(
                        $('<div>').addClass('expanded-section-label').text(image_table_expanded_kernel).after(
                          $('<div>').addClass('expanded-section-content').addClass('clearfix').append(
                            $('<ul>').addClass('kernel-expanded').addClass('clearfix').append(
                              $('<li>').append(
                                $('<div>').addClass('expanded-title').text(image_table_expanded_kernel_id),
                                $('<div>').addClass('expanded-value').text(kernel['id'])),
                              $('<li>').append(
                                $('<div>').addClass('expanded-title').text(image_table_expanded_name),
                                $('<div>').addClass('expanded-value').text(kernel['name'])),
                              $('<li>').append(
                                $('<div>').addClass('expanded-title').text(image_table_expanded_arch),
                                $('<div>').addClass('expanded-value').text(kernel['architecture'])),
                              $('<li>').append(
                                $('<div>').addClass('expanded-title').text(image_table_expanded_manifest),
                                $('<div>').addClass('expanded-value').text(kernel['location'].replace('&#x2f;','/'))),
                              $('<li>').append(
                                $('<div>').addClass('expanded-title').text(image_table_expanded_desc),
                                $('<div>').addClass('expanded-value').html(kernel['description'] ? kernel['description'] : '&nbsp;'))))));
      }
      var $ramdiskInfo = null;
      if(ramdisk){
        $ramdiskInfo = $('<div>').addClass('image-table-expanded-ramdisk').addClass('clearfix').append(
                         $('<div>').addClass('expanded-section-label').text(image_table_expanded_ramdisk).after(
                           $('<div>').addClass('expanded-section-content').addClass('clearfix').append(
                             $('<ul>').addClass('ramdisk-expanded').addClass('clearfix').append(
                               $('<li>').append(
                                 $('<div>').addClass('expanded-title').text(image_table_expanded_ramdisk_id),
                                 $('<div>').addClass('expanded-value').text(ramdisk['id'])),
                               $('<li>').append(
                                 $('<div>').addClass('expanded-title').text(image_table_expanded_name),
                                 $('<div>').addClass('expanded-value').text(ramdisk['name'])),
                               $('<li>').append(
                                 $('<div>').addClass('expanded-title').text(image_table_expanded_arch),
                                 $('<div>').addClass('expanded-value').text(ramdisk['architecture'])),
                               $('<li>').append(
                                 $('<div>').addClass('expanded-title').text(image_table_expanded_manifest),
                                 $('<div>').addClass('expanded-value').text(ramdisk['location'].replace('&#x2f;','/'))),
                               $('<li>').append(
                                 $('<div>').addClass('expanded-title').text(image_table_expanded_desc),
                                 $('<div>').addClass('expanded-value').html(ramdisk['description'] ? ramdisk['description'] : '&nbsp;'))))));
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
