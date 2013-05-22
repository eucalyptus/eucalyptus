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
  $.widget('eucalyptus.volume', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    attachDialog : null,
    tagDialog : null,
    attachButtonId : 'volume-attach-btn',
    createButtonId : 'volumes-add-btn',
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #volumeTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $volTable = $wrapper.children().first();
      var $volHelp = $wrapper.children().last();
      this.baseTable = $volTable;
      this.tableWrapper = $volTable.eucatable({
        id : 'volumes', // user of this widget should customize these options,
        data_deps: ['volumes', 'snapshots', 'tags', 'instances'],
        hidden : thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'volume',
          "aaSorting": [[ 7, "desc" ]],
          "aoColumnDefs": [
            {
              // Display the checkbox button in the main table
              "bSortable": false,
              "aTargets":[0],
              "mData": function(source) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell"
            },
            {
              // Display the id of the volume in the main table
              "aTargets":[1], 
              "mData": function(source){
                this_mouseover = source.id;
                this_value = source.display_id;
                return eucatableDisplayColumnTypeTwist(this_mouseover, this_value, 256);
              },
              "sClass": "wrap-content",
            },
            {
              // Display the status of the volume in the main table
              "aTargets":[2],
              "mData": function(source) { 
                 return eucatableDisplayColumnTypeVolumeStatus(source.status);
               },
              "sClass": "narrow-cell",
              "bSearchable": false,
              "iDataSort": 8, // sort on hidden status column
              "sWidth": 50,
            },
            { 
              // Display the size of the volume in the main table
              "aTargets":[3],
              "mRender": function(data) {
                if(isInt(data)) 
                  return data;
                else
                  return "ERROR";
              },
              "mData": "size",
              "sClass": "centered-cell"
            },
            { 
              // Display the instance id of the attached volume in the main table
              "aTargets":[4],
              "mData": function(source){
                this_mouseover = source.instance_id;
                this_value = source.display_instance_id;
                return eucatableDisplayResource(this_mouseover, this_value, 256);
              },
              "sClass": "wrap-content",
            },
            { 
              // Display the snapshot id of the volume in the main table
              "aTargets":[5],
              "mData": function(source){
                this_mouseover = source.snapshot_id;
                this_value = source.display_snapshot_id;
                return eucatableDisplayResource(this_mouseover, this_value, 256);
              },
              "sClass": "wrap-content",
            },
            { 
              // Display the availibility zone of the volume in the main table
              "aTargets":[6],
              "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "zone",
              "sClass": "wrap-content",
            },
            { 
              // Display the creation time of the volume in the main table
              "aTargets":[7], 
              "asSorting" : [ 'desc', 'asc' ],
              "mRender": function(data) { return formatDateTime(data); },
              "mData": "create_time",
              "iDataSort": 9
            },
            {
              // Invisible column for the status, used for sort
              "bVisible": false,
              "aTargets":[8],
              "mData": "status",
            },
            {
              // Invisible column for the creation time, used for sort
              "bVisible": false,
              "aTargets":[9],
              "mRender": function(data) {
                return data;			// escaping causes the sort operation to fail	013013
              },
              "mData": "create_time",
              "sType": "date"
            },
            {
              // Invisible column for the id
              "bVisible": false,
              "aTargets":[10],
              "mData": "id",
            }
          ],
        },
        text : {
          header_title : volume_h_title,
          create_resource : volume_create,
          resource_found : 'volume_found',
          resource_search : volume_search,
          resource_plural : volume_plural,
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        expand_callback : function(row){ // row = [col1, col2, ..., etc]
          return thisObj._expandCallback(row);
        },
        menu_click_create : function (args) {
            thisObj._createVolumeAction();       // BACKBONE INTEGRATED DIALOG  --- Kyo 041013 
        },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $volHelp, url: help_volume.landing_content_url});
        },
        filters : [{name:"vol_state", options: ['all','attached','detached'], text: [vol_state_selector_all,vol_state_selector_attached,vol_state_selector_detached], filter_col:8, alias: {'attached':'in-use','detached':'available'}}],
        legend : ['creating', 'available', 'in-use', 'deleting', 'deleted', 'error'],
      });
      this.tableWrapper.appendTo(this.element);
      $('html body').eucadata('addCallback', 'volume', 'volume-landing', function() {
        thisObj.tableWrapper.eucatable('redraw');
      });
    },

    _create : function() { 
      var thisObj = this;
      // volume create dialog start
      $tmpl = $('html body').find('.templates #volumeAddDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $add_dialog = $rendered.children().first();
      var $add_help = $rendered.children().last();
      var option = {
         id: 'volumes-add',
         title: volume_dialog_add_title,
         buttons: {
           'create': { domid: thisObj.createButtonId, text: volume_dialog_create_btn, disabled: true, click: function() { 
              var size = $.trim($add_dialog.find('#volume-size').val());
              var az = $add_dialog.find('#volume-add-az-selector').val();
              var $snapshot = $add_dialog.find('#volume-add-snapshot-selector :selected');
              var isValid = true;

              if ( size == parseInt(size) ) {
                if ( $snapshot.val() != '' && parseInt($snapshot.attr('title')) > parseInt(size) ) {
                  isValid = false;
                  $add_dialog.eucadialog('showError', volume_dialog_snapshot_error_msg);
                }
                if( parseInt(size) <= 0) {
                  isValid = false; 
                  $add_dialog.eucadialog('showError', volume_dialog_size_error_msg);
                }
              } else {
                  isValid = false; 
                  $add_dialog.eucadialog('showError', volume_dialog_size_error_msg);
              }
              if ( az === '' ) {
                isValid = false;
                $add_dialog.eucadialog('showError', volume_dialog_az_error_msg);
              }
              if ( isValid ) {
                $add_dialog.eucadialog("close");
                thisObj._createVolume(size, az, asText($snapshot.val()));
              } 
            }},
           'cancel': {text: dialog_cancel_btn, click: function() { $add_dialog.eucadialog("close");}} 
         },
         help: { content: $add_help , url: help_volume.dialog_add_content_url, pop_height: 500}
       };
      // volume create dialog end
      // tag dialog begins
      $tmpl = $('html body').find('.templates #resourceTagWidgetTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $tag_dialog = $rendered.children().first();
      var $tag_help = $rendered.children().last();
      this.tagDialog = $tag_dialog.eucadialog({
        id: 'volumes-tag-resource',
        title: 'Add/Edit tags',
        help: {content: $tag_help, url: help_instance.dialog_terminate_content_url},
      });
      // tag dialog ends

    },

    _destroy : function() {
    },

    _createVolume : function (size, az, snapshotId) {
      var thisObj = this;
      sid = snapshotId != '' ? "&SnapshotId=" + snapshotId : '';
      $.ajax({
        type:"POST",
        url:"/ec2?Action=CreateVolume",
        data:"_xsrf="+$.cookie('_xsrf')+"&Size="+size+"&AvailabilityZone="+az+sid,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              var volId = data.results.id;
              notifySuccess(null, $.i18n.prop('volume_create_success', DefaultEncoder().encodeForHTML(volId)));
              thisObj.tableWrapper.eucatable('refreshTable');
              thisObj.tableWrapper.eucatable('glowRow', volId);
            } else {
              notifyError($.i18n.prop('volume_create_error'), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('volume_create_error'), getErrorMessage(jqXHR));
          }
      });
    },

    _detachListedVolumes : function (volumes, force) {
      var thisObj = this;
      var done = 0;
      var all = volumes.length;
      var error = [];
      doMultiAjax( volumes, function(item, dfd){
        var volumeId = item;
        $.ajax({
          type:"POST",
          url:"/ec2?Action=DetachVolume",
          data:"_xsrf="+$.cookie('_xsrf')+"&VolumeId="+volumeId,
          dataType:"json",
          async:true,
          success: (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == 'detaching' ) {
                ;
              } else {
                error.push({id:volumeId, reason: undefined_error});
              }
           }
          })(volumeId),
          error: (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              error.push({id:volumeId, reason:  getErrorMessage(jqXHR)});
            }
          })(volumeId),
          complete: (function(volumeId) {
            return function(jqXHR, textStatus){
              done++;
              if(done < all)
                notifyMulti(100*(done/all), $.i18n.prop('volume_detach_progress', all));
              else {
	        // XSS Node:: 'volume_detach_fail' would contain a chunk HTML code in the failure description string.
	     	// Message Example - Failed to send release request to Cloud for {0} IP address(es). <a href="#">Click here for details. </a>
	        // For this reason, the message string must be rendered as html()
                var $msg = $('<div>').addClass('multiop-summary').append(
                  $('<div>').addClass('multiop-summary-success').html($.i18n.prop('volume_detach_done', (all-error.length), all)));
                if (error.length > 0)
                  $msg.append($('<div>').addClass('multiop-summary-failure').html($.i18n.prop('volume_detach_fail', error.length)));
                notifyMulti(100, $msg.html(), error);
                thisObj.tableWrapper.eucatable('refreshTable');
              }
              dfd.resolve();
            }
          })(volumeId)
        });
      });
    },

    _tagResourceAction : function(){
      var selected = this.tableWrapper.eucatable('getSelectedRows', 10);
      if ( selected.length > 0 ) {
        require(['app'], function(app) {
           app.dialog('edittags', app.data.volume.get(selected[0]));
        });
       }
    },

    _deleteVolumeAction : function() {
      var dialog = 'delete_volume_dialog';
      var selected = this.tableWrapper.eucatable('getSelectedRows', 10);
      require(['views/dialogs/' + dialog], function( dialog) {
        new dialog({items: selected});
      });
    },

    _createSnapshotAction : function() {
      var dialog = 'create_snapshot_dialog';
      var selected = this.tableWrapper.eucatable('getSelectedRows', 10);
      require(['views/dialogs/' + dialog], function( dialog) {
        new dialog({volume_id: selected});
      });
    },

    _attachVolumeAction : function() {
      var selected = this.tableWrapper.eucatable('getSelectedRows', 10);
      require(['views/dialogs/attach_volume_dialog'], function(dialog) {
        new dialog({volume_id: selected});
      });
    },

    _detachVolumeAction : function() {
      var dialog = 'detach_volume_dialog';
      var selected = this.tableWrapper.eucatable('getSelectedRows', 10);
      require(['views/dialogs/' + dialog], function( dialog) {
        new dialog({volume_ids: selected});
      });
    },

    _createVolumeAction : function(){
      var dialog = 'create_volume_dialog';
      require(['app'], function(app) {
        app.dialog(dialog);
      });
    },

    _expandCallback : function(row){ 
      var $el = $('<div />');
      require(['app', 'views/expandos/volume'], function(app, expando) {
         new expando({el: $el, model: app.data.volumes.get(row[10]) });
      });
      return $el;
    },


    _createMenuActions : function() {
      var thisObj = this;
      var volumes = thisObj.baseTable.eucatable('getSelectedRows');
      var itemsList = {};

      (function(){
        itemsList['attach_volume'] = {"name": volume_action_attach, callback: function(key, opt) {;}, disabled: function(){ return true;} }  // Backbone Dialog -- Kyo 040713
        itemsList['detach_volume'] = {"name": volume_action_detach, callback: function(key, opt) {;}, disabled: function(){ return true;} }  // Backbone Dialog -- Kyo 040713
        itemsList['create_snapshot_from_volume'] = {"name": volume_action_create_snapshot, callback: function(key, opt) {;}, disabled: function(){ return true;} }  // Backbone Dialog -- Kyo 040713
        itemsList['delete_volume'] = {"name": volume_action_delete, callback: function(key, opt) {;}, disabled: function(){ return true;} }     // Backbone Dialog -- Kyo 040613
        itemsList['tag'] = {"name":table_menu_edit_tags_action, callback: function(key, opt) {;}, disabled: function(){ return true;} }
      })();

      // add attach action
      if ( volumes.length === 1 && volumes[0].status === 'available' ){
        itemsList['attach_volume'] = {"name": volume_action_attach, callback: function(key, opt){ thisObj._attachVolumeAction(); }}   // Backbone Dialog -- Kyo 040813
      }

      // detach actions
      if ( volumes.length > 0 ) {
        addOption = true;
        for (v in volumes) {
          if ( volumes[v].status !== 'in-use' ) {
            addOption = false;
            break;
          }
          // do not allow to detach ebs root volume
          if (volumes[v].attach_data.instance_id) {
            addOption = !isRootVolume(volumes[v].attach_data.instance_id, volumes[v].id);
            if (!addOption)
              break;
          }
        }
        if (addOption){
//          itemsList['detach'] = { "name": volume_action_detach, callback: function(key, opt) { thisObj._detachAction(); } }
          itemsList['detach_volume'] = { "name": volume_action_detach, callback: function(key, opt) { thisObj._detachVolumeAction(); } }    // Backbone Dialog -- Kyo 040813
        }
      }

      // create snapshot-action
      if ( volumes.length === 1) {
         if ( volumes[0].status === 'in-use' || volumes[0].status === 'available' ){
            itemsList['create_snapshot_from_volume'] = {"name": volume_action_create_snapshot, callback: function(key, opt){ thisObj._createSnapshotAction(); }}   // BAckbone Dialog -- Kyo 040813
         }
      }

      // add delete action
      if ( volumes.length > 0 ) {
        addOption = true;
        for (v in volumes) {
          if (! ( volumes[v].status === 'available'  || volumes[v].status === 'failed')) {
            addOption = false;
            break;
          }
        }
        if (addOption){
//          itemsList['delete'] = { "name": volume_action_delete, callback: function(key, opt) { thisObj._deleteAction(); } }
          itemsList['delete_volume'] = {"name": volume_action_delete, callback: function(key, opt){ thisObj._deleteVolumeAction(); }}  // Backbone Dialog -- Kyo 040813
        }
      }

      // add resource tag option	031913
      if (volumes.length === 1) {
        itemsList['tag'] = {"name":table_menu_edit_tags_action, callback: function(key, opt){ thisObj._tagResourceAction(); }}
      }

      return itemsList;
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
