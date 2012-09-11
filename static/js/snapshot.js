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
  $.widget('eucalyptus.snapshot', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    delDialog : null,
    createDialog : null,
    addVolumeDialog : null,
    createVolButtonId : 'volume-create-btn',
    createSnapButtonId : 'snapshot-create-btn',
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #snapshotTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_snapshot)));
      var $snapshotTable = $wrapper.children().first();
      var $snapshotHelp = $wrapper.children().last();
      this.baseTable = $snapshotTable;
      this.tableWrapper = $snapshotTable.eucatable({
        id : 'snapshots', // user of this widget should customize these options,
        dt_arg : {
          "bProcessing": true,
          "sAjaxSource": "../ec2?Action=DescribeSnapshots",
          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "sPaginationType": "full_numbers",
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { "mDataProp": "id" },
            {
              "fnRender": function(oObj) { 
                 $div = $('<div>').addClass('table-row-status').addClass('status-'+oObj.aData.status);
                 $div.append(oObj.aData.status=='pending' ?  oObj.aData.progress : '&nbsp;');
                 return asHTML($div);
               },
              "sWidth": "20px",
              "bSearchable": false,
              "iDataSort": 7, // sort on hiden status column
            },
            { "mDataProp": "volume_size" },
            { "mDataProp": "volume_id" },
            { "mDataProp": "description" },
            // output start time in browser format and timezone
            { "fnRender": function(oObj) { d = new Date(oObj.aData.start_time); return d.toLocaleString(); } },
            {
              "bVisible": false,
              "mDataProp": "status"
            }
          ],
        },
        text : {
          header_title : snapshot_h_title,
          create_resource : snapshot_create,
          resource_found : snapshot_found,
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        menu_click_create : function (args) { thisObj._createAction() },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, $snapshotHelp);
        },
        filters : [{name:"snap_state", options: ['all','in-progress','completed'], text: [snap_state_selector_all, snap_state_selector_in_progress, snap_state_selector_completed], filter_col:7, alias: {'in-progress':'pending','completed':'completed'}} ],
        legend : ['pending', 'completed', 'error'],
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
      var thisObj = this;
      // snapshot delete dialog start
      var $tmpl = $('html body').find('.templates #snapshotDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_snapshot)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();
      this.delDialog = $del_dialog.eucadialog({
         id: 'snapshots-delete',
         title: snapshot_delete_dialog_title,
         buttons: {
           'delete': {text: snapshot_dialog_del_btn, click: function() { thisObj._deleteListedSnapshots(); $del_dialog.eucadialog("close");}},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $del_dialog.eucadialog("close");}} 
         },
         help: {title: help_snapshot['dialog_delete_title'], content: $del_help},
       });
      // snapshot delete dialog end
      // create snapshot dialog start
      $tmpl = $('html body').find('.templates #snapshotCreateDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_snapshot)));
      var $snapshot_dialog = $rendered.children().first();
      var $snapshot_dialog_help = $rendered.children().last();
      this.createDialog = $snapshot_dialog.eucadialog({
         id: 'snapshot-create-from-snapshot',
         title: snapshot_create_dialog_title,
         buttons: {
           'create': { domid: thisObj.createSnapButtonId, text: snapshot_create_dialog_create_btn, disabled: true, click: function() { 
                volumeId = $.trim($snapshot_dialog.find('#snapshot-create-volume-id').val());
                if (VOL_ID_PATTERN.test(volumeId)) {
                  description = $.trim($snapshot_dialog.find('#snapshot-create-description').val());
                  thisObj._createSnapshot(volumeId, description);
                  $snapshot_dialog.eucadialog("close");
                } else {
                  thisObj.createDialog.eucadialog('showError', snapshot_create_dialog_error_msg);
                }
              } 
            },
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $snapshot_dialog.eucadialog("close"); } }
         },
         help: {title: help_snapshot['dialog_create_title'], content: $snapshot_dialog_help},
         on_open: {spin: true, callback: function(args) {
           var dfd = $.Deferred();
           thisObj._initCreateDialog(dfd) ; // pulls volumes info from the server
           return dfd.promise();
         }},
       });
       var $vol_selector = this.createDialog.find('#snapshot-create-volume-id');
       this.createDialog.eucadialog('buttonOnFocus', $vol_selector, thisObj.createSnapButtonId, function(){
         return VOL_ID_PATTERN.test($vol_selector.val());
       });
      // create snapshot dialog end
      // volume create dialog start
      $tmpl = $('html body').find('.templates #volumeAddDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $add_dialog = $rendered.children().first();
      var $add_help = $rendered.children().last();
      this.addVolumeDialog = $add_dialog.eucadialog({
         id: 'volumes-add',
         title: volume_dialog_add_title,
         buttons: {
           'create': { domid: thisObj.createVolButtonId, text: volume_dialog_create_btn, disabled: true, click: function() { 
              var size = $.trim($add_dialog.find('#volume-size').val());
              var az = $add_dialog.find('#volume-add-az-selector').val();
              var $snapshot = $add_dialog.find('#volume-add-snapshot-selector :selected');
              var isValid = true;

              if ( size == parseInt(size) ) {
                if ( $snapshot.val() != '' && parseInt($snapshot.attr('title')) > parseInt(size) ) {
                  isValid = false;
                  $add_dialog.eucadialog('showError', volume_dialog_snapshot_error_msg);
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
                thisObj._createVolume(size, az, $snapshot.val());
                $add_dialog.eucadialog("close");
              } 
            }},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $add_dialog.eucadialog("close");}} 
         },
         help: {title: help_volume['dialog_add_title'], content: $add_help},
         on_open: {spin: true, callback: function(args) {
           var dfd = $.Deferred();
           thisObj._initVolumeAddDialog(dfd) ; // pulls az and snapshot info from the server
           return dfd.promise();
         }},
       });
       var $az_selector = thisObj.addVolumeDialog.find('#volume-add-az-selector');
       var $vol_size_edit = thisObj.addVolumeDialog.find('#volume-size');

       this.addVolumeDialog.eucadialog('buttonOnChange', $az_selector,  thisObj.createVolButtonId, function(){
         return $az_selector.val() !== '' &&  $vol_size_edit.val() && $vol_size_edit.val().length>0;
       }); 
       this.addVolumeDialog.eucadialog('buttonOnChange', $vol_size_edit,  thisObj.createVolButtonId, function(){
         return $az_selector.val() !== '' &&  $vol_size_edit.val() && $vol_size_edit.val().length>0;
       }); 
       // volume create dialog end
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      thisObj = this;
      var selectedSnapshots = thisObj.baseTable.eucatable('getSelectedRows', 7); // 7th column=status (this is snapshot's knowledge)
      var itemsList = {};
      if ( selectedSnapshots.length > 0 && onlyInArray('completed', selectedSnapshots)){
        itemsList['delete'] = { "name": snapshot_action_delete, callback: function(key, opt) { thisObj._deleteAction(); } }
      }
      return itemsList;
    },

    _initVolumeAddDialog : function(dfd) { // method should resolve dfd object
      var thisObj = this;
      var results = describe('zone');
      var $azSelector = thisObj.addVolumeDialog.find('#volume-add-az-selector').html('');
      if (results && results.length > 1)
        $azSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_dialog_zone_select']));
      for( res in results) {
        var azName = results[res].name;
        $azSelector.append($('<option>').attr('value', azName).text(azName));
      }
     
      results = describe('snapshot'); 
      var $snapSelector = thisObj.addVolumeDialog.find('#volume-add-snapshot-selector').html('');
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
              notifySuccess(null, volume_create_success(data.results.id));
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
    _initCreateDialog : function(dfd) { // method should resolve dfd object
      thisObj = this;
      var $volSelector = this.createDialog.find('#snapshot-create-volume-id').val('');
      var results = describe('volume');
      var volume_ids = [];
      if ( results ) {
        for( res in results) {
           var volume = results[res];
           if ( volume.status === 'in-use' || volume.status === 'available' ) 
             volume_ids.push(volume.id);
        }
      }
      
      if ( volume_ids.length == 0 ) {
        this.createDialog.eucadialog('hideButton', thisObj.createSnapButtonId, 'create');
        this.createDialog.find('#snapshot-create-dialog-some-volumes').hide();
        this.createDialog.find('#snapshot-create-dialog-no-volumes').show();
        this.createDialog.find('#snapshot-create-dialog-new-vol').click( function() {
          thisObj.createDialog.dialog('close');
          thisObj.addVolumeDialog.dialog('open');
        });
      } else {
        this.createDialog.eucadialog('showButton', thisObj.createSnapButtonId, 'create', true);
        this.createDialog.find('#snapshot-create-dialog-some-volumes').show();
        this.createDialog.find('#snapshot-create-dialog-no-volumes').hide(); 
        $volSelector.autocomplete({
          source: volume_ids
        });
      }
      dfd.resolve();
    },

    _getSnapshotId : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },

    _deleteListedSnapshots : function () {
      var thisObj = this;
      var snapshotsToDelete = thisObj.delDialog.eucadialog('getSelectedResources',0);
      for ( i = 0; i<snapshotsToDelete.length; i++ ) {
        var snapshotId = snapshotsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DeleteSnapshot&SnapshotId=" + snapshotId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success:
          (function(snapshotId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess(null, snapshot_delete_success(snapshotId));
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                notifyError(null, snapshot_delete_error(snapshotId));
              }
           }
          })(snapshotId),
          error:
          (function(snapshotId) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError(null, snapshot_delete_error(snapshotId));
            }
          })(snapshotId)
        });
      }
    },

    _createSnapshot : function (volumeId, description) {
      var thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=CreateSnapshot&VolumeId=" + volumeId + "&Description=" + description,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, snapshot_create_success(volumeId));
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, snapshot_create_error(volumeId));
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError(null, snapshot_create_error(volumeId));
          }
      });
    },

    _deleteAction : function(){
      var thisObj = this;
      snapshotsToDelete = thisObj.tableWrapper.eucatable('getSelectedRows', 1);
      var matrix = [];
      $.each(snapshotsToDelete,function(idx, key){
        matrix.push([key]);
      });
      if ( snapshotsToDelete.length > 0 ) {
        thisObj.delDialog.eucadialog('setSelectedResources',{title:[snapshot_label], contents: matrix});
        thisObj.delDialog.dialog('open');
      }
    },

    _createAction : function() {
      this.createDialog.eucadialog('open');
    },

/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
