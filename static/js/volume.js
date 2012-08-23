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
    forceDetachDialog : null,
    addDialog : null,
    attachDialog : null,
    waitDialog : null,
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #volumeTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $volTable = $wrapper.children().first();
      var $volHelp = $wrapper.children().last();
      this.baseTable = $volTable;
      this.element.add($volTable);
      this.tableWrapper = $volTable.eucatable({
        id : 'volumes', // user of this widget should customize these options,
        dt_arg : {
          "bProcessing": true,
          "sAjaxSource": "../ec2?Action=DescribeVolumes",
          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "sPaginationType": "full_numbers",
          "sDom": '<"table_volumes_header"><"table-volume-filter">f<"clear"><"table_volumes_top">rt<"table-volumes-legend">p<"clear">',
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { "mDataProp": "id" },
            {
              "fnRender": function(oObj) { s = (oObj.aData.status == 'in-use') ? oObj.aData.attach_data.status : oObj.aData.status; return '<div title="'+ $.i18n.map['volume_state_' + s.replace('-','_')] +'" class="volume-status-' + oObj.aData.status + '">&nbsp;</div>'; },
              "sWidth": "20px",
              "bSearchable": false,
              "iDataSort": 8, // sort on hiden status column
            },
            { "mDataProp": "size" },
            { "mDataProp": "attach_data.instance_id" },
            { "mDataProp": "snapshot_id" },
            { "mDataProp": "zone" },
            // output creation time in browser format and timezone
            { "fnRender": function(oObj) { d = new Date(oObj.aData.create_time); return d.toLocaleString(); } },
            {
              "bVisible": false,
              "mDataProp": "status"
            }
          ],
        },
        text : {
          header_title : volume_h_title,
          create_resource : volume_create,
          resource_found : volume_found,
        },
        menu_actions : function(args){ 
          selectedVolumes = thisObj.baseTable.eucatable('getValueForSelectedRows', 8); // 8th column=status (this is volume's knowledge)
          var itemsList = {};
         // add attach action
          if ( selectedVolumes.length == 1 && selectedVolumes.indexOf('available') == 0 ){
            itemsList['attach'] = { "name": volume_action_attach, callback: function(key, opt) { thisObj._attachAction(); } }
          }
          // detach actions
          if ( selectedVolumes.length > 0 ) {
            addDetach = true;
            for (s in selectedVolumes) {
              if ( selectedVolumes[s] != 'in-use' ) {
                addDetach = false;
                break;
              }
            }
            if ( addDetach ) {
              itemsList['detach'] = { "name": volume_action_detach, callback: function(key, opt) { thisObj._detachAction(false); } }
              itemsList['force_detach'] = { "name": volume_action_force_detach, callback: function(key, opt) { thisObj._detachAction(true); } }
            }
          }
          if ( selectedVolumes.length  == 1 ) {
             if ( selectedVolumes[0] == 'in-use' || selectedVolumes[0] == 'available' ) {
                itemsList['create_snapshot'] = { "name": volume_action_create_snapshot, callback: function(key, opt) { thisObj._createSnapshotAction(); } }
            }
          }
        // add delete action
          if ( selectedVolumes.length > 0 ){
             itemsList['delete'] = { "name": volume_action_delete, callback: function(key, opt) { thisObj._deleteAction(); } }
          }
          return itemsList;
        },
        context_menu_actions : function(row) {
          var state = row['status'];
          switch (state) {
            case 'available':
              return {
                "attach": { "name": volume_action_attach, callback: function(key, opt) { thisObj._attachAction(thisObj._getVolumeId(opt.selector)); } },
                "create_snapshot": { "name": volume_action_create_snapshot, callback: function(key, opt) { thisObj._createSnapshotAction(thisObj._getVolumeId(opt.selector)); } },
                "delete": { "name": volume_action_delete, callback: function(key, opt) { thisObj._deleteAction(thisObj._getVolumeId(opt.selector)); } }
              }
            case 'in-use':
              return {
                "detach": { "name": volume_action_detach, callback: function(key, opt) { thisObj._detachAction($(opt.selector), false); } },
                "force_detach": { "name": volume_action_force_detach, callback: function(key, opt) { thisObj._detachAction($(opt.selector), true); } },
                "create_snapshot": { "name": volume_action_create_snapshot, callback: function(key, opt) { thisObj._createSnapshotAction(thisObj._getVolumeId(opt.selector)); } },
                "delete": { "name": volume_action_delete, callback: function(key, opt) { thisObj._deleteAction(thisObj._getVolumeId(opt.selector)); } }
              }
            default:
              return {
                "delete": { "name": volume_action_delete, callback: function(key, opt) { thisObj._deleteAction(thisObj._getVolumeId(opt.selector)); } }
              }
          }
        },
        menu_click_create : function (args) { thisObj._createAction() },
      //  td_hover_actions : { instance: [4, function (args) { thisObj.handleInstanceHover(args); }], snapshot: [5, function (args) { thisObj.handleSnapshotHover(args); }] }
        help_click : function(evt) {
          var $helpHeader = $('<div>').addClass('euca-table-header').append(
                              $('<span>').text(help_volume['landing_title']).append(
                                $('<div>').addClass('help-link').append(
                                  $('<a>').attr('href','#').html('&larr;'))));
          thisObj._flipToHelp(evt,$helpHeader, $volHelp);
        },
      });
      this.tableWrapper.appendTo(this.element);

      //add filter to the table
      // TODO: make templates
      $tableFilter = $('div.table-volume-filter');
      $tableFilter.addClass('euca-table-filter');
      $tableFilter.append(
        $('<span>').addClass("filter-label").html(table_filter_label),
        $('<select>').attr('id', 'volumes-selector'));

      filterOptions = ['all', 'attached', 'detached'];
      $sel = $tableFilter.find("#volumes-selector");
      for (o in filterOptions)
        $sel.append($('<option>').val(filterOptions[o]).text($.i18n.map['volume_selecter_' + filterOptions[o]]));

      $.fn.dataTableExt.afnFiltering.push(
	function( oSettings, aData, iDataIndex ) {
          // first check if this is called on a volumes table
          if (oSettings.sInstance != 'volumes')
            return true;
          selectorValue = $("#volumes-selector").val();
          switch (selectorValue) {
            case 'attached':
              return 'in-use' == aData[8];
              break;
            case 'detached':
              return 'available' == aData[8];
              break;
          }
          return true;
        }
      );

      // attach action
      $("#volumes-selector").change( function() { thisObj._reDrawTable() } );

      // TODO: let's move legend to html as a template
      //add leged to the volumes table
      $tableLegend = $("div.table-volumes-legend");
      $tableLegend.append($('<span>').addClass('volume-legend').html(volume_legend));

      statuses = ['creating', 'available', 'in-use', 'deleting', 'deleted', 'error'];
      for (s in statuses)
        $tableLegend.append($('<span>').addClass('volume-status-legend').addClass('volume-status-' + statuses[s]).html($.i18n.map['volume_state_' + statuses[s].replace('-','_')]));

      $tmpl = $('html body').find('.templates #volumeDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();
      this.delDialog = $del_dialog.eucadialog({
         id: 'volumes-delete',
         title: volume_dialog_del_title,
         buttons: {
           'delete': {text: volume_dialog_del_btn, click: function() { thisObj._deleteListedVolumes(); $del_dialog.dialog("close");}},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_delete_title'], content: $del_help},
       });

      $tmpl = $('html body').find('.templates #volumeDetachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $detach_dialog = $rendered.children().first();
      var $detach_help = $rendered.children().last();
      this.detachDialog = $detach_dialog.eucadialog({
         id: 'volumes-detach',
         title: volume_dialog_detach_title,
         buttons: {
           'detach': {text: volume_dialog_detach_btn, click: function() { thisObj._detachListedVolumes(false); $detach_dialog.dialog("close");}},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $detach_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_detach_title'], content: $detach_help},
       });

      $tmpl = $('html body').find('.templates #volumeForceDetachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $force_detach_dialog = $rendered.children().first();
      var $force_detach_help = $rendered.children().last();
      this.forceDetachDialog = $force_detach_dialog.eucadialog({
         id: 'volumes-force-detach',
         title: volume_dialog_force_detach_title,
         buttons: {
           'detach': {text: volume_dialog_detach_btn, click: function() { thisObj._detachListedVolumes(true); $force_detach_dialog.dialog("close");}},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $force_detach_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_force_detach_title'], content: $force_detach_help},
       });

      $tmpl = $('html body').find('.templates #volumeWaitDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $wait_dialog = $rendered.children().first();
      var $wait_dialog_help = $rendered.children().last();
      this.waitDialog = $wait_dialog.eucadialog({
         id: 'volumes-wait',
         title: volume_dialog_wait,
         buttons: {
           'cancel': { text: volume_dialog_cancel_btn, focus:true, click: function() { $wait_dialog.dialog("close"); } } 
         },
         help: {title: help_volume['dialog_volume_wait_title'], content: $wait_dialog_help},
       });

      var attachButtonId = 'volume-attach-btn';
      $tmpl = $('html body').find('.templates #volumeAttachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $attach_dialog = $rendered.children().first();
      var $attach_dialog_help = $rendered.children().last();
      this.attachDialog = $attach_dialog.eucadialog({
         id: 'volumes-attach',
         title: volume_dialog_attach_title,
         buttons: {
           'attach': { domid: attachButtonId, text: volume_dialog_attach_btn, disabled: true, click: function() { 
                volumeId = $attach_dialog.find('#volume-attach-volume-selector').val();
                instanceId = $attach_dialog.find('#volume-attach-instance-selector').val()
                device = $.trim($attach_dialog.find('#volume-attach-device-name').val());
                thisObj._attachVolume(volumeId, instanceId, device);
                $attach_dialog.dialog("close");
              } 
            },
           'cancel': { text: volume_dialog_cancel_btn, focus:true, click: function() { $attach_dialog.dialog("close"); } }
         },
         help: {title: help_volume['dialog_volume_attach_title'], content: $attach_dialog_help},
         on_open: {spin: true, callback: function(args) {
           var dfd = $.Deferred();
           thisObj._initAttachDialog(dfd); // pulls instance info from server
           return dfd.promise();
         }},
       });
      this.attachDialog.eucadialog('onKeypress', 'volume-attach-device-name', attachButtonId, function () {
         var inst = thisObj.attachDialog.find('#volume-attach-instance-selector').val();
         return inst != '';
        });
      this.attachDialog.find('#volume-attach-instance-selector').change( function () {
        instanceId = thisObj.attachDialog.find('#volume-attach-instance-selector').val()
        device = thisObj.attachDialog.find('#volume-attach-device-name').val();
        $button = thisObj.attachDialog.parent().find('#' + attachButtonId);
        if ( device.length > 0 && instanceId !== '')
          $button.prop("disabled", false).removeClass("ui-state-disabled");
        else
          $button.prop("disabled", false).addClass("ui-state-disabled");
       });

      var createButtonId = 'volumes-add-btn';
      $tmpl = $('html body').find('.templates #volumeAddDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $add_dialog = $rendered.children().first();
      var $add_help = $rendered.children().last();
      this.addDialog = $add_dialog.eucadialog({
         id: 'volumes-add',
         title: volume_dialog_add_title,
         buttons: {
           'create': { domid: createButtonId, text: volume_dialog_create_btn, disabled: true, click: function() { 
              var size = $.trim($add_dialog.find('#volume-size').val());
              var az = $add_dialog.find('#volume-add-az-selector').val();
              var $snapshot = $add_dialog.find('#volume-add-snapshot-selector :selected');
              var isValid = true;
              $notification = $add_dialog.find('div.dialog-notifications');

              if ( size == parseInt(size) ) {
                if ( $snapshot.val() != '' && parseInt($snapshot.attr('title')) > parseInt(size) ) {
                  isValid = false;
                  $notification.html(volume_dialog_snapshot_error_msg);
                }
              } else {
                isValid = false; 
                $notification.html(volume_dialog_size_error_msg);
              }
              if ( az === '' ) {
                isValid = false;
                $notification.html($notification.html() + "<br/>" + volume_dialog_az_error_msg);
              }
              if ( isValid ) {
                thisObj._createVolume(size, az, $snapshot.val());
                $add_dialog.dialog("close");
              } 
            }},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $add_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_add_title'], content: $add_help},
         on_open: {spin: true, callback: function(args) {
           var dfd = $.Deferred();
           thisObj._initAddDialog(dfd) ; // pulls az and snapshot info from the server
           return dfd.promise();
         }},
       });
       this.addDialog.eucadialog('onKeypress', 'volume-size', createButtonId, function () {
         var az = thisObj.addDialog.find('#volume-add-az-selector').val();
         return az != '';
       });
       this.addDialog.find('#volume-add-az-selector').change( function () {
         size = $.trim(thisObj.addDialog.find('#volume-size').val());
         az = thisObj.addDialog.find('#volume-add-az-selector').val();
         $button = thisObj.addDialog.parent().find('#' + createButtonId);
         if ( size.length > 0 && az !== '')     
           $button.prop("disabled", false).removeClass("ui-state-disabled");
         else
           $button.prop("disabled", false).addClass("ui-state-disabled");
       });
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _initAddDialog : function(dfd) { // method should resolve dfd object
      this.addDialog.find('div.dialog-notifications').html('');
      $.when(
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DescribeAvailabilityZones",
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:false,
          success:
           function(data, textStatus, jqXHR){
              $azSelector = $('#volume-add-az-selector').html('');
              $azSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_dialog_zone_select']));
              if ( data.results ) {
                for( res in data.results) {
                  azName = data.results[res].name;
                  $azSelector.append($('<option>').attr('value', azName).text(azName));
                } 
              } else {
                notifyError(null, error_loading_azs_msg);
                dfd.reject();
              }
           },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError(null, error_loading_azs_msg);
              dfd.reject();
            }
      })).then(function (output){
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DescribeSnapshots",
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:false,
          success:
            function(data, textStatus, jqXHR){
              $snapSelector = $('#volume-add-snapshot-selector').html('');
              $snapSelector.append($('<option>').attr('value', '').text($.i18n.map['selection_none']));
              if ( data.results ) {
                for( res in data.results) {
                  snapshot = data.results[res];
                  if ( snapshot.status === 'completed' ) {
                    $snapSelector.append($('<option>').attr('value', snapshot.id).attr('title', snapshot.volume_size).text(
                      snapshot.id + ' (' + snapshot.volume_size + ' ' + $.i18n.map['size_gb'] +')'));
                  }
                } 
                dfd.resolve();
              } else {
                notifyError(null, error_loading_snapshots_msg);
                dfd.reject();
              }
           },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError(null, error_loading_snapshots_msg);
              dfd.reject();
            }
        });
      }, function (output) { dfd.reject(); });
    },

    _initAttachDialog : function(dfd) {  // should resolve dfd object
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeInstances",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:false,
        success:
          function(data, textStatus, jqXHR){
            $instanceSelector = $('#volume-attach-instance-selector').html('');
            $instanceSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_attach_select_instance']));
            if ( data.results ) {
              for( res in data.results) {
                instance = data.results[res];
                if ( instance.state === 'running' ) {
                  $instanceSelector.append($('<option>').attr('value', instance.id).text(instance.id));
                }
              }
              dfd.resolve();
            } else {
              notifyError(null, error_loading_instances_msg);
              dfd.reject();
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError(null, error_loading_instances_msg);
            dfd.reject();
          }
      })
    },

    _getVolumeId : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },
/*
    handleInstanceHover : function(e) {
      switch(e.type) {
        case 'mouseleave':
          $(e.currentTarget).removeClass("hoverCell");
          break;
        case 'mouseenter':
          $(e.currentTarget).addClass("hoverCell");
          break;
      }
    },

    handleSnapshotHover : function(e) {
      switch(e.type) {
        case 'mouseleave':
          $(e.currentTarget).removeClass("hoverCell");
          $(e.currentTarget).off('click');
          break;
        case 'mouseenter':
          $(e.currentTarget).addClass("hoverCell");
          break;
      }
    },
*/
    _reDrawTable : function() {
      this.tableWrapper.eucatable('reDrawTable');
    },

    _deleteListedVolumes : function () {
      var thisObj = this;
      $volumesToDelete = this.delDialog.find("#volumes-to-delete");
      var rowsToDelete = $volumesToDelete.text().split(ID_SEPARATOR);
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var volumeId = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DeleteVolume&VolumeId=" + volumeId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success:
          (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess(null, volume_delete_success + ' ' + volumeId);
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                notifyError(null, volume_delete_error + ' ' + volumeId);
              }
           }
          })(volumeId),
          error:
          (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError(null, volume_delete_error + ' ' + volumeId);
            }
          })(volumeId)
        });
      }
    },

    _attachVolume : function (volumeId, instanceId, device) {
      thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=AttachVolume&VolumeId=" + volumeId + "&InstanceId=" + instanceId + "&Device=" + device,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, volume_attach_success + ' ' + volumeId);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, volume_attach_error + ' ' + volumeId);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError(null, volume_attach_error + ' ' + volumeId);
          }
      });
    },

    _createVolume : function (size, az, snapshotId) {
      var thisObj = this;
      sid = snapshotId != '' ? "&SnapshotId=" + snapshotId : '';
      $.ajax({
        type:"GET",
        url:"/ec2?Action=CreateVolume&Size=" + size + "&AvailabilityZone=" + az + sid,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, volume_create_success + ' ' + data.results.id);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, volume_create_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError(null, volume_create_error);
          }
      });
    },

    _detachListedVolumes : function (force) {
      var thisObj = this;
      dialogToUse = force ? this.forceDetachDialog : this.detachDialog; 
      $volumesToDelete = dialogToUse.find("#volumes-to-detach");
      var volumes = $volumesToDelete.text().split(ID_SEPARATOR);
      for ( i = 0; i<volumes.length; i++ ) {
        var volumeId = volumes[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DetachVolume&VolumeId=" + volumeId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success:
          (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == 'detaching' ) {
                if (force)
                  notifySuccess(null, volume_force_detach_success + ' ' + volumeId);
                else
                  notifySuccess(null, volume_detach_success + ' ' + volumeId);
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                if (force)
                  notifyError(null, volume_force_detach_error + ' ' + volumeId);
                else
                  notifyError(null, volume_detach_error + ' ' + volumeId);
              }
           }
          })(volumeId),
          error:
          (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              if (force)
                notifyError(null, volume_force_detach_error + ' ' + volumeId);
              else
                notifyError(null, volume_detach_error + ' ' + volumeId);
            }
          })(volumeId)
        });
      }
    },

    _deleteAction : function(volumeId) {
      var thisObj = this;
      volumesToDelete = [];
      if ( !volumeId ) {
        volumesToDelete = thisObj.tableWrapper.eucatable('getAllSelectedRows');
      } else {
        volumesToDelete[0] = volumeId;
      }

      if ( volumesToDelete.length > 0 ) {
        thisObj.delDialog.eucadialog('setSelectedResources', volumesToDelete);
        $volumesToDelete = thisObj.delDialog.find("#volumes-to-delete");
        $volumesToDelete.html(volumesToDelete.join(ID_SEPARATOR));
        thisObj.delDialog.dialog('open');
      }
    },

    _createAction : function() {
      this.addDialog.eucadialog('open');
    },

    _attachAction : function(volumeId) {
      thisObj = this;
      var volumeToAttach = '';
      if ( !volumeId ) {
        rows = thisObj.tableWrapper.eucatable('getAllSelectedRows', 1);
        volumeToAttach = rows[0];
      } else {
        volumeToAttach = volumeId;
      }
      this.attachDialog.find('div.dialog-notifications').html('');
      $volumeSelector = this.attachDialog.find('#volume-attach-volume-selector');
      $volumeSelector.html('');
      $volumeSelector.append($('<option>').attr('value', volumeId).text(volumeId));
      $volumeSelector.attr('disabled', 'disabled');
      this.attachDialog.eucadialog('open');
    },

    _detachAction : function(row, force) {
      var thisObj = this;
      volumes = [];
      if ( !row ) {
        rows = thisObj.tableWrapper.eucatable('getContentForSelectedRows');
        for(r in rows){
          $row = $(rows[r]);
          volumes.push([$row.find('td:eq(1)').text(), $row.find('td:eq(4)').text()]);
        }
      } else {
        volumes.push([row.find('td:eq(1)').text(), row.find('td:eq(4)').text()]);
      }

      dialogToUse = force ? this.forceDetachDialog : this.detachDialog;
      if ( volumes.length > 0 ) {
        $detachIds = dialogToUse.find("tbody.resource-ids");
        $detachIds.html('');
        $volumesToDetach = dialogToUse.find("#volumes-to-detach");
        ids = [];
        for ( i = 0; i<volumes.length; i++ ) {
          vol = escapeHTML(volumes[i][0]);
          inst = escapeHTML(volumes[i][1]);
          ids.push(volumes[i][0]);
          $tr = $('<tr>').append($('<td>').text(vol),$('<td>').text(inst));
          $detachIds.append($tr);
        }
        $volumesToDetach.html(ids.join(ID_SEPARATOR));
        dialogToUse.dialog('open');
      }
    },

    _createSnapshotAction : function() {

    },

/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
