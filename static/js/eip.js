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
  $.widget('eucalyptus.eip', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    releaseDialog : null,
    allocateDialog : null,
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #eipTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $eipTable = $wrapper.children().first();
      var $eipHelp = $wrapper.children().last();
      this.baseTable = $eipTable;
      this.element.add($eipTable);
      this.tableWrapper = $eipTable.eucatable({
        id : 'eips', // user of this widget should customize these options,
        dt_arg : {
          "bProcessing": true,
          "sAjaxSource": "../ec2?Action=DescribeAddresses",
          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "sPaginationType": "full_numbers",
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { "mDataProp": "public_ip" },
            { "mDataProp": "instance_id" },
            {
              "bVisible": false,
              "fnRender": function(oObj) { return oObj.aData.instance_id != 'nobody' ? 'assigned' : 'unassigned' } 
            }
          ],
        },
        text : {
          header_title : eip_h_title,
          create_resource : eip_allocate,
          resource_found : eip_found,
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        menu_click_create : function (args) { thisObj._createAction() },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, $eipHelp);
        },
        filters : [{name:"eip_state", options: ['all','assigned','unassigned'], filter_col:3, alias: {'assigned':'assigned','unassigned':'unassigned'}}],
      });
      this.tableWrapper.appendTo(this.element);

      // eip delete dialog start
      $tmpl = $('html body').find('.templates #eipDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();
      this.releaseDialog = $del_dialog.eucadialog({
         id: 'eips-delete',
         title: eip_delete_dialog_title,
         buttons: {
           'delete': {text: eip_dialog_del_btn, click: function() { thisObj._deleteListedeips(); $del_dialog.dialog("close");}},
           'cancel': {text: eip_dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_delete_title'], content: $del_help},
       });
      // eip delete dialog end
      // create eip dialog end
      $tmpl = $('html body').find('.templates #eipCreateDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $eip_dialog = $rendered.children().first();
      var $eip_dialog_help = $rendered.children().last();
      this.allocateDialog = $eip_dialog.eucadialog({
         id: 'eip-create-from-eip',
         title: eip_create_dialog_title,
         buttons: {
           'create': { text: eip_create_dialog_create_btn, click: function() { 
                volumeId = $eip_dialog.find('#eip-create-volume-selector').val();
                description = $.trim($eip_dialog.find('#eip-create-description').val());
                thisObj._createeip(volumeId, description);
                $eip_dialog.dialog("close");
              } 
            },
           'cancel': { text: eip_dialog_cancel_btn, focus:true, click: function() { $eip_dialog.dialog("close"); } }
         },
         help: {title: help_volume['dialog_eip_create_title'], content: $eip_dialog_help},
         on_open: {spin: true, callback: function(args) {
           var dfd = $.Deferred();
           thisObj._initallocateDialog(dfd) ; // pulls volumes info from the server
           return dfd.promise();
         }},
       });
      // create eip dialog end
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      thisObj = this;
      selectedeips = thisObj.baseTable.eucatable('getSelectedRows', 7); // 7th column=status (this is eip's knowledge)
      var itemsList = {};
      if ( selectedeips.length > 0 ){
        itemsList['delete'] = { "name": eip_action_delete, callback: function(key, opt) { thisObj._deleteAction(); } }
      }
      return itemsList;
    },

    _initallocateDialog : function(dfd) { // method should resolve dfd object
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeVolumes",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:false,
        cache:false,
        success:
          function(data, textStatus, jqXHR){
            $volSelector = $('#eip-create-volume-selector').html('');
     //       $volSelector.append($('<option>').attr('value', '').text($.i18n.map['selection_none']));
            if ( data.results ) {
              for( res in data.results) {
                volume = data.results[res];
                if ( volume.status === 'in-use' || volume.status === 'available' ) {
                  $volSelector.append($('<option>').attr('value', volume.id).text(volume.id));
                }
              } 
              dfd.resolve();
            } else {
              notifyError(null, error_loading_volumes_msg);
              dfd.reject();
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError(null, error_loading_volumes_msg);
            dfd.reject();
          }
      });
    },

    _geteipId : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },

    _deleteListedeips : function () {
      var thisObj = this;
      $eipsToDelete = this.releaseDialog.find("#eips-to-delete");
      var rowsToDelete = $eipsToDelete.text().split(ID_SEPARATOR);
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var eipId = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=Deleteeip&eipId=" + eipId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success:
          (function(eipId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess(null, eip_delete_success + ' ' + eipId);
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                notifyError(null, eip_delete_error + ' ' + eipId);
              }
           }
          })(eipId),
          error:
          (function(eipId) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError(null, eip_delete_error + ' ' + eipId);
            }
          })(eipId)
        });
      }
    },

    _createeip : function (volumeId, description) {
      var thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=Createeip&VolumeId=" + volumeId + "&Description=" + description,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:true,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, eip_create_success + ' ' + volumeId);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, eip_create_error + ' ' + volumeId);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError(null, eip_create_error + ' ' + volumeId);
          }
      });
    },

    _deleteAction : function(eipId) {
      var thisObj = this;
      eipsToDelete = [];
      if ( !eipId ) {
        eipsToDelete = thisObj.tableWrapper.eucatable('getSelectedRows', 1);
      } else {
        eipsToDelete[0] = eipId;
      }

      if ( eipsToDelete.length > 0 ) {
        thisObj.releaseDialog.eucadialog('setSelectedResources', eipsToDelete);
        $eipsToDelete = thisObj.releaseDialog.find("#eips-to-delete");
        $eipsToDelete.html(eipsToDelete.join(ID_SEPARATOR));
        thisObj.releaseDialog.dialog('open');
      }
    },

    _createAction : function() {
      this.allocateDialog.eucadialog('open');
    },

/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
