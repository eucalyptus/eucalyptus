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
    delDialog : null,
    detachDialog : null,
    //forceDetachDialog : null, // forceDetach is not supported
    addDialog : null,
    attachDialog : null,
    attachButtonId : 'volume-attach-btn',
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #volumeTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $volTable = $wrapper.children().first();
      var $volHelp = $wrapper.children().last();
      this.baseTable = $volTable;
      this.tableWrapper = $volTable.eucatable({
        id : 'volumes', // user of this widget should customize these options,
        dt_arg : {
          "sAjaxSource": "../ec2?Action=DescribeVolumes",
          "fnServerData": function (sSource, aoData, fnCallback) {
                $.ajax( {
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": "_xsrf="+$.cookie('_xsrf'),
                    "success": fnCallback
                });

          },
          "aaSorting": [[ 7, "desc" ]],
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell"
            },
            { "mDataProp": "id" },
            {
              "fnRender": function(oObj) { 
                 return '<div class="table-row-status status-'+oObj.aData.status+'">&nbsp;</div>';
               },
              "sClass": "narrow-cell",
              "bSearchable": false,
              "iDataSort": 8, // sort on hidden status column
              "sWidth": 50,
            },
            { 
              "mDataProp": "size",
              "sClass": "centered-cell"
            },
            { "mDataProp": "attach_data.instance_id" },
            { "mDataProp": "snapshot_id" },
            { "mDataProp": "zone" },
            { 
              "asSorting" : [ 'desc', 'asc' ],
              "fnRender": function(oObj) { return formatDateTime(oObj.aData.create_time); },
              "iDataSort": 9
            },
            {
              "bVisible": false,
              "mDataProp": "status"
            },
            {
              "bVisible": false,
              "mDataProp": "create_time",
              "sType": "date"
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
        menu_click_create : function (args) { thisObj._createAction() },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $volHelp, url: help_volume.landing_content_url});
        },
        filters : [{name:"vol_state", options: ['all','attached','detached'], text: [vol_state_selector_all,vol_state_selector_attached,vol_state_selector_detached], filter_col:8, alias: {'attached':'in-use','detached':'available'}}],
        legend : ['creating', 'available', 'in-use', 'deleting', 'deleted', 'error'],
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
      var thisObj = this;
      // volume delete dialog start
      var $tmpl = $('html body').find('.templates #volumeDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();
      this.delDialog = $del_dialog.eucadialog({
         id: 'volumes-delete',
         title: volume_dialog_del_title,
         buttons: {
           'delete': {text: volume_dialog_del_btn, click: function() {
                var volumesToDelete = thisObj.delDialog.eucadialog('getSelectedResources', 0);
                $del_dialog.eucadialog("close");
                thisObj._deleteListedVolumes(volumesToDelete);
            }},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $del_dialog.eucadialog("close");}} 
         },
         help: { content: $del_help , url: help_volume.dialog_delete_content_url},
       });
      // volume delete dialog end
      // volume detach dialog start
      $tmpl = $('html body').find('.templates #volumeDetachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $detach_dialog = $rendered.children().first();
      var $detach_help = $rendered.children().last();
      this.detachDialog = $detach_dialog.eucadialog({
         id: 'volumes-detach',
         title: volume_dialog_detach_title,
         buttons: {
           'detach': {text: volume_dialog_detach_btn, click: function() {
                 var volumes =  dialogToUse.eucadialog('getSelectedResources', 0);
                 $detach_dialog.eucadialog("close");
                 thisObj._detachListedVolumes(volumes, false);
            }},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $detach_dialog.eucadialog("close");}} 
         },
         help: { content: $detach_help, url:  help_volume.dialog_detach_content_url},
       });
      // volume detach dialog end
      // attach dialog start
      $tmpl = $('html body').find('.templates #volumeAttachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $attach_dialog = $rendered.children().first();
      var $attach_dialog_help = $rendered.children().last();
      this.attachDialog = $attach_dialog.eucadialog({
         id: 'volumes-attach',
         title: volume_dialog_attach_title,
         buttons: {
           'attach': { domid: thisObj.attachButtonId, text: volume_dialog_attach_btn, disabled: true, click: function() { 
                volumeId = asText($attach_dialog.find('#volume-attach-volume-id').val());
                instanceId = asText($attach_dialog.find('#volume-attach-instance-id').val());
                device = $.trim(asText($attach_dialog.find('#volume-attach-device-name').val()));
                $attach_dialog.eucadialog("close");
                thisObj._attachVolume(volumeId, instanceId, device);
              } 
            },
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $attach_dialog.eucadialog("close"); } }
         },
         help: { content: $attach_dialog_help, url: help_volume.dialog_attach_content_url },
         on_open: {callback: []},
       });

      // attach dialog end
      // volume create dialog start
      var createButtonId = 'volumes-add-btn';
      $tmpl = $('html body').find('.templates #volumeAddDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $add_dialog = $rendered.children().first();
      var $add_help = $rendered.children().last();
      var option = {
         id: 'volumes-add',
         title: volume_dialog_add_title,
         buttons: {
           'create': { domid: createButtonId, text: volume_dialog_create_btn, disabled: true, click: function() { 
              var size = $.trim(asText($add_dialog.find('#volume-size').val()));
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
         help: { content: $add_help , url: help_volume.dialog_add_content_url, pop_height: 500},
         on_open: {spin: true, callback: [ function(args) {
           var dfd = $.Deferred();
           var $az_selector = thisObj.addDialog.find('#volume-add-az-selector');
           var $vol_size_edit = thisObj.addDialog.find('#volume-size');
           $add_dialog.eucadialog('buttonOnChange', $az_selector,  createButtonId, function(){
             return $az_selector.val() !== '' &&  $vol_size_edit.val() == parseInt(asText($vol_size_edit.val()));
           }); 
           $add_dialog.eucadialog('buttonOnKeyup', $vol_size_edit,  createButtonId, function(){
             return $az_selector.val() !== '' &&  $vol_size_edit.val() == parseInt(asText($vol_size_edit.val()));
           });
           thisObj._initAddDialog(dfd) ; // pulls az and snapshot info from the server
           return dfd.promise();
         }]},
       };
      this.addDialog = $add_dialog.eucadialog(option);
      var $volSize = thisObj.addDialog.find('#volume-size');
      var $snapSelector = thisObj.addDialog.find('#volume-add-snapshot-selector');
      $snapSelector.change( function(){
        snapshotId = $snapSelector.val();
        if (snapshotId) {
          var snapshot = describe('snapshot', snapshotId);
          $volSize.val(snapshot['volume_size']);
          //check is create button can be activated
          if (thisObj.addDialog.find('#volume-add-az-selector').val() != '')
            thisObj.addDialog.eucadialog('enableButton', createButtonId);
        }
      });
      $add_dialog.eucadialog('validateOnType', '#volume-size', function(size) {
        if ( size != '' && (size != parseInt(size) || size < 1) )
          return volume_dialog_size_error_msg;
        else
          return null;
      });
      // volume create dialog end
    },

    _destroy : function() {
    },

    _initAddDialog : function(dfd) { // method should resolve dfd object
      var thisObj = this;
      var results = describe('zone');
      var $azSelector = thisObj.addDialog.find('#volume-add-az-selector').html('');
      if (results && results.length > 1)
        $azSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_dialog_zone_select']));
      for( res in results) {
        var azName = results[res].name;
        $azSelector.append($('<option>').attr('value', azName).text(azName));
      }
     
      results = describe('snapshot'); 
      var $snapSelector = thisObj.addDialog.find('#volume-add-snapshot-selector').html('');
      $snapSelector.append($('<option>').attr('value', '').text($.i18n.map['selection_none']));
      if ( results ) {
        for( res in results) {
          var snapshot = results[res];
          if ( snapshot.status === 'completed' ) {
            $snapSelector.append($('<option>').attr('value', snapshot.id).attr('title', snapshot.volume_size).text(
               snapshot.id + ' (' + snapshot.volume_size + ' ' + $.i18n.map['size_gb'] +')'));
          }
        } 
      }
      dfd.resolve();
    },

    _generateRecommendedDeviceNames : function(count) {
      possibleNames = {};
      for(i=0; i<11 && i<=count; i++){ // f..p
        possibleNames['/dev/sd'+String.fromCharCode(102+i)] = 1;
      }
      return possibleNames;
    },

    _suggestNextDeviceName : function(instanceId) {
      var instance = describe('instance', instanceId);
      if (instance) {
        var count = 1;
        for(device in instance.block_device_mapping) count++;
        possibleNames = this._generateRecommendedDeviceNames(count);
        for(device in instance.block_device_mapping){
          possibleNames[device] = 0;
        }
        for(n in possibleNames){
          if (possibleNames[n] == 1){
            return n;
          }
        }
      }
      return '';
    },

    _initAttachDialog : function(dfd) {  // should resolve dfd object
      var thisObj = this;
      var $instanceSelector = this.attachDialog.find('#volume-attach-instance-id');
      var $volumeSelector = this.attachDialog.find('#volume-attach-volume-id');
      var $deviceName = thisObj.attachDialog.find('#volume-attach-device-name');

      if(!$instanceSelector.val()){ // launch from volume landing
        var inst_ids = [];
        var vol_id = asText($volumeSelector.val());
        var results = describe('volume');
        var volume = null;
        if ( results ) {
          for( res in results) {
            if (results[res].id === vol_id){
              volume=results[res];
              break;
            }
          }
        }
        results = describe('instance');
        if ( volume && results ) {
          for( res in results) {
            var instance = results[res];
            if ( instance.state === 'running' && instance.placement === volume.zone)
              inst_ids.push(instance.id);
          }
        }
        if ( inst_ids.length == 0 )
          this.attachDialog.eucadialog('showError', no_running_instances);

        $instanceSelector.autocomplete({
          source: inst_ids,
          select: function(event, ui) {
            if ($.trim(asText($deviceName.val())) == ''){
              $deviceName.val(thisObj._suggestNextDeviceName(ui.item.value));
            }
            if ($deviceName.val() != '' && $volumeSelector.val() != '')
              thisObj.attachDialog.eucadialog('activateButton', thisObj.attachButtonId);
          }
        });
        $instanceSelector.watermark(instance_id_watermark);
      }
      if(!$volumeSelector.val()){ // launch from instance landing
        var inst_id = asText($instanceSelector.val());
        var results = describe('instance');
        var instance = null;
        if (results){
          for( res in results){
            if (results[res].id === inst_id){
              instance = results[res];
              break;
            }
          }
        }
        var vol_ids = [];
        results = describe('volume');
        if( results && instance ) {
          for ( res in results){
            var volume = results[res];
            if ( volume.status === 'available' )
              vol_ids.push(volume.id);
          }
        }
        if (vol_ids.length == 0 )
          this.attachDialog.eucadialog('showError', no_available_volume);
        $volumeSelector.autocomplete( {
          source: vol_ids,
          select: function() {
            if ($deviceName.val() != '' && $instanceSelector.val() != '')
              thisObj.attachDialog.eucadialog('activateButton', thisObj.attachButtonId);
          }
        });
        $volumeSelector.watermark(volume_id_watermark);
      }
      //dfd.resolve();
    },

    _deleteListedVolumes : function (volumesToDelete) {
      var thisObj = this;
      doMultiAjax(volumesToDelete, function(item, dfd){
        var volumeId = item; 
        $.ajax({
          type:"POST",
          url:"/ec2?Action=DeleteVolume",
          data:"_xsrf="+$.cookie('_xsrf')+"&VolumeId="+volumeId,
          dataType:"json",
          timeout:PROXY_TIMEOUT,
          async:true,
          success:
          (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess(null, $.i18n.prop('volume_delete_success', volumeId));
                dfd.resolve();
              } else {
                notifyError($.i18n.prop('volume_delete_error', volumeId), undefined_error);
                dfd.reject();
              }
           }
          })(volumeId),
          error:
          (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('volume_delete_error', volumeId), getErrorMessage(jqXHR));
              dfd.reject();
            }
          })(volumeId)
        });
      });
      thisObj.tableWrapper.eucatable('refreshTable');
    },

    _attachVolume : function (volumeId, instanceId, device) {
      var thisObj = this;
      $.ajax({
        type:"POST",
        url:"/ec2?Action=AttachVolume",
        data:"_xsrf="+$.cookie('_xsrf')+"&VolumeId="+volumeId+"&InstanceId="+instanceId+"&Device="+device,
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('volume_attach_success', volumeId, instanceId));
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError($.i18n.prop('volume_attach_error', volumeId, instanceId), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('volume_attach_error', volumeId, instanceId), getErrorMessage(jqXHR));
          }
      });
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
              notifySuccess(null, $.i18n.prop('volume_create_success', volId));
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
      dialogToUse = this.detachDialog;
      doMultiAjax( volumes, function(item, dfd){
        var volumeId = item;
        $.ajax({
          type:"POST",
          url:"/ec2?Action=DetachVolume",
          data:"_xsrf="+$.cookie('_xsrf')+"&VolumeId="+volumeId,
          dataType:"json",
          timeout:PROXY_TIMEOUT,
          async:true,
          success:
          (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == 'detaching' ) {
                if (force)
                  notifySuccess(null, $.i18n.prop('volume_force_detach_success', volumeId));
                else
                  notifySuccess(null, $.i18n.prop('volume_detach_success', volumeId));
                dfd.resolve();
              } else {
                if (force)
                  notifyError($.i18n.prop('volume_force_detach_error', volumeId), undefined_error);
                else
                  notifyError($.i18n.prop('volume_detach_error', volumeId), undefined_error);
                dfd.reject();
              }
           }
          })(volumeId),
          error:
          (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              if (force)
                notifyError($.i18n.prop('volume_force_detach_error', volumeId), getErrorMessage(jqXHR));
              else
                notifyError($.i18n.prop('volume_detach_error', volumeId), getErrorMessage(jqXHR));
              dfd.reject();
            }
          })(volumeId)
        });
      });
      thisObj.tableWrapper.eucatable('refreshTable');
    },

    _deleteAction : function() {
      var thisObj = this;
      volumesToDelete = thisObj.tableWrapper.eucatable('getSelectedRows', 1);
      if( volumesToDelete.length > 0 ) {
        var matrix = [];
        $.each(volumesToDelete, function(idx, volume){
          matrix.push([volume]); 
        });
        thisObj.delDialog.eucadialog('setSelectedResources', {title:[volume_label], contents: matrix});
        thisObj.delDialog.dialog('open');
      }
    },

    _createAction : function() {
      this.dialogAddVolume();
    },

    _attachAction : function() {
      var thisObj = this;
      var volumeToAttach = thisObj.tableWrapper.eucatable('getSelectedRows', 1)[0];
      thisObj.dialogAttachVolume(volumeToAttach, null);
    },

    _detachAction : function(){
      var thisObj = this;
      var rows = thisObj.tableWrapper.eucatable('getSelectedRows');
      dialogToUse = this.detachDialog;
      if ( rows.length > 0 ) {
        var matrix = [];
        $.each(rows, function(idx, volume){
          matrix.push([volume.id, volume.attach_data.instance_id]); 
        });
        dialogToUse.eucadialog('setSelectedResources', {title: [volume_label,instance_label], contents: matrix});
        dialogToUse.dialog('open');
      }
    },

    _createSnapshotAction : function() {
      var volumeToUse = this.tableWrapper.eucatable('getSelectedRows', 1)[0];
      addSnapshot(volumeToUse);
    },

    _createMenuActions : function() {
      var thisObj = this;
      var volumes = thisObj.baseTable.eucatable('getSelectedRows');
      var itemsList = {};

      (function(){
        itemsList['attach'] = { "name": volume_action_attach, callback: function(key, opt) {;}, disabled: function(){ return true;} } 
        itemsList['detach'] = { "name": volume_action_detach, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['create_snapshot'] = { "name": volume_action_create_snapshot, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['delete'] = { "name": volume_action_delete, callback: function(key, opt) {;}, disabled: function(){ return true;} }
      })();

      // add attach action
      if ( volumes.length === 1 && volumes[0].status === 'available' ){
        itemsList['attach'] = { "name": volume_action_attach, callback: function(key, opt) { thisObj._attachAction(); } }
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
        if (addOption)
          itemsList['detach'] = { "name": volume_action_detach, callback: function(key, opt) { thisObj._detachAction(); } }
      }
      // create snapshot-action
      if ( volumes.length === 1) {
         if ( volumes[0].status === 'in-use' || volumes[0].status === 'available' )
            itemsList['create_snapshot'] = { "name": volume_action_create_snapshot, callback: function(key, opt) { thisObj._createSnapshotAction(); } }
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
        if (addOption)
          itemsList['delete'] = { "name": volume_action_delete, callback: function(key, opt) { thisObj._deleteAction(); } }
      }
      return itemsList;
    },

/**** Public Methods ****/
    close: function() {
      //this.tableWrapper.eucatable('close');
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },

    dialogAttachVolume : function(volume, instance){
      var thisObj = this;
      var openCallback = function() {
        if(volume){
          var $volumeId = thisObj.attachDialog.find('#volume-attach-volume-id');
          $volumeId.val(volume);
          $volumeId.attr('disabled', 'disabled');
          thisObj.attachDialog.find('#volume-attach-device-name').val('');
        } 
        if(instance){
          var $instanceId = thisObj.attachDialog.find('#volume-attach-instance-id');
          $instanceId.val(instance);
          $instanceId.attr('disabled', 'disabled');
          thisObj.attachDialog.find('#volume-attach-device-name').val(thisObj._suggestNextDeviceName(instance));
        }
        thisObj._initAttachDialog(); // pulls instance info from server
        var $instance_id = thisObj.attachDialog.find('#volume-attach-instance-id');
        var $device_name = thisObj.attachDialog.find('#volume-attach-device-name');
        thisObj.attachDialog.eucadialog('buttonOnKeyup', $device_name, thisObj.attachButtonId, function () {
          return $instance_id.val() != '';
        });
      }
      var on_open = this.attachDialog.eucadialog('option', 'on_open'); 
      // make sure that there is only one set variables callback function
      if (on_open.callback.length == 1)
        on_open.callback.pop();
      on_open.callback.push(openCallback);
      this.attachDialog.eucadialog('option', 'on_open', on_open);
      this.attachDialog.eucadialog('open');
    },

    dialogAddVolume : function(snapshotId){
      var thisObj = this;
      if (snapshotId) {
        var openCallback = function() {
          var $snapSelector = thisObj.addDialog.find('#volume-add-snapshot-selector');
          var $size = thisObj.addDialog.find('#volume-size');
          var snapshot = describe('snapshot', snapshotId);
          $snapSelector.val(snapshotId);
          $snapSelector.attr('disabled', 'disabled');
          $size.val(snapshot['volume_size']);
        }
        var on_open = this.addDialog.eucadialog('option', 'on_open');
        // create dialog has its own on_open
        if (on_open.callback.length == 2)
          on_open.callback.pop();
        on_open.callback.push(openCallback);
        this.addDialog.eucadialog('option', 'on_open', on_open);
      }
      this.addDialog.eucadialog('open');
    }
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
