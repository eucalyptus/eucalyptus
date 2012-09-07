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
    associateDialog : null,
    disassociateDialog : null,

    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #eipTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_eip)));
      var $eipTable = $wrapper.children().first();
      var $eipHelp = $wrapper.children().last();
      this.baseTable = $eipTable;
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
              "fnRender": function(oObj) { 
                return oObj.aData.instance_id ? 'assigned' : 'unassigned' 
              } 
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
        filters : [{name:"eip_state", options: ['all','assigned','unassigned'], filter_col:3, alias: {'assigned':'assigned','unassigned':'unassigned'}, text: [eip_state_selector_all,eip_state_selector_assigned,eip_state_selector_unassigned] }],
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
      var thisObj = this;
      // eip release dialog start
      var $tmpl = $('html body').find('.templates #eipReleaseDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_eip)));
      var $release_dialog = $rendered.children().first();
      var $release_help = $rendered.children().last();
      this.releaseDialog = $release_dialog.eucadialog({
         id: 'eips-release',
         title: eip_release_dialog_title,
         buttons: {
           'release': {text: eip_release_dialog_release_btn, click: function() { thisObj._releaseListedIps(); $release_dialog.eucadialog("close");}},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $release_dialog.eucadialog("close");}}
         },
         help: {title: help_eip['dialog_release_title'], content: $release_help},
       });
      // eip release dialog end
      // allocate eip dialog end
      var allocateButtonId = 'eip-allocate-btn';
      $tmpl = $('html body').find('.templates #eipAllocateDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_eip)));
      var $eip_allocate_dialog = $rendered.children().first();
      var $eip_allocate_dialog_help = $rendered.children().last();
      this.allocateDialog = $eip_allocate_dialog.eucadialog({
         id: 'eip-allocate',
         title: eip_allocate_dialog_title,
         buttons: {
           'create': { domid: allocateButtonId, text: eip_allocate_dialog_allocate_btn, disabled: true, click: function() {
                var numberOfIps = $eip_allocate_dialog.find('#eip-allocate-count').val();
                if ( numberOfIps != parseInt(numberOfIps) ) {
                  $eip_allocate_dialog.eucadialog('showError', eip_allocate_count_error_msg);
                } else {
                  thisObj._allocateIps(numberOfIps);
                  $eip_allocate_dialog.eucadialog("close");
                }
              } 
            },
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $eip_allocate_dialog.eucadialog("close"); } }
         },
         help: {title: help_eip['dialog_allocate_title'], content: $eip_allocate_dialog_help},
       });
      var $ip_count_edit = this.allocateDialog.find('#eip-allocate-count');
      this.allocateDialog.eucadialog('buttonOnChange', $ip_count_edit,  allocateButtonId, function(){
        return $ip_count_edit.val() && $ip_count_edit.val().length>0;
      });
      // allocate eip dialog end
      // associate eip dialog end
      $tmpl = $('html body').find('.templates #eipAssociateDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_eip)));
      var $eip_associate_dialog = $rendered.children().first();
      var $eip_associate_dialog_help = $rendered.children().last();
      this.associateDialog = $eip_associate_dialog.eucadialog({
         id: 'eip-associate',
         title: eip_associate_dialog_title,
         buttons: {
           'associate': { text: eip_associate_dialog_associate_btn, click: function() {
               thisObj._associateIp(
                 $eip_associate_dialog.find("#eip-to-associate").html(),
                 $eip_associate_dialog.find('#eip-associate-dialog-instance-selector').val()
               );
               $eip_associate_dialog.eucadialog("close");
              } 
            },
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $eip_associate_dialog.eucadialog("close"); } }
         },
         help: {title: help_eip['dialog_associate_title'], content: $eip_associate_dialog_help},
         on_open: {spin: true, callback: function(args) {
           var dfd = $.Deferred();
           thisObj._initAssociateDialog(dfd) ; // pulls instances from the server
           return dfd.promise();
         }},
       });
      // associate eip dialog end
      // disassociate eip dialog start
      $tmpl = $('html body').find('.templates #eipDisassociateDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_eip)));
      var $eip_disassociate_dialog = $rendered.children().first();
      var $eip_disassociate_dialog_help = $rendered.children().last();
      this.disassociateDialog = $eip_disassociate_dialog.eucadialog({
         id: 'eip-disassociate',
         title: eip_disassociate_dialog_title,
         buttons: {
           'disassociate': { text: eip_disassociate_dialog_disassociate_btn, click: function() {
               thisObj._disassociateListedIps();
               $eip_disassociate_dialog.eucadialog("close");
              } 
            },
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $eip_disassociate_dialog.eucadialog("close"); } }
         },
         help: {title: help_eip['dialog_disassociate_title'], content: $eip_disassociate_dialog_help},
       });
      // disassociate eip dialog end
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      thisObj = this;
      selectedEips = thisObj.baseTable.eucatable('getSelectedRows', 3);
      var itemsList = {};

      (function(){
        itemsList['associate'] = { "name": eip_action_associate, callback: function(key, opt) {;}, disabled: function(){ return true;} } 
        itemsList['release'] = { "name": eip_action_release, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['disassociate'] = { "name": eip_action_disassociate, callback: function(key, opt) {;}, disabled: function(){ return true;} }
      })();

      // add associate
      if ( selectedEips.length == 1 && selectedEips[0] == 'unassigned' ){
        itemsList['associate'] = { "name": eip_action_associate, callback: function(key, opt) { thisObj._associateAction(); } }
      }
      if ( selectedEips.length > 0 ){
        if ( onlyInArray('assigned', selectedEips) )
        itemsList['disassociate'] = { "name": eip_action_disassociate, callback: function(key, opt) { thisObj._disassociateAction(); } }

        itemsList['release'] = { "name": eip_action_release, callback: function(key, opt) { thisObj._releaseAction(); } }
      }
      return itemsList;
    },

    _releaseListedIps : function () {
      var thisObj = this;
      var rowsToDelete = this.releaseDialog.eucadialog('getSelectedResources', 0);
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var eipId = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=ReleaseAddress&PublicIp=" + eipId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:false,
          cache:false,
          success:
          (function(eipId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess(null, eipId + ' ' + eip_release_success);
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                notifyError(null, eip_release_error + ' ' + eipId);
              }
           }
          })(eipId),
          error:
          (function(eipId) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError(null, eip_release_error + ' ' + eipId);
            }
          })(eipId)
        });
      }
    },

    _disassociateListedIps : function () {
      var thisObj = this;
      var ipsToDisassociate = this.disassociateDialog.eucadialog('getSelectedResources', 0);
      for ( i = 0; i<ipsToDisassociate.length; i++ ) {
        var eipId = ipsToDisassociate[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DisassociateAddress&PublicIp=" + eipId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:false,
          cache:false,
          success:
          (function(eipId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess(null, eipId + ' ' + eip_disassociate_success);
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                notifyError(null, eip_disassociate_error + ' ' + eipId);
              }
           }
          })(eipId),
          error:
          (function(eipId) {
            return function(jqXHR, textStatus, errorThrown){
              notifyError(null, eip_disassociate_error + ' ' + eipId);
            }
          })(eipId)
        });
      }
    },

    _allocateIps : function (numberIpsToAllocate) {
      var thisObj = this;
      for ( i=0; i<numberIpsToAllocate; i++)
        $.ajax({
          type:"GET",
          url:"/ec2?Action=AllocateAddress",
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          cache:false,
          async:false,
          success:
            function(data, textStatus, jqXHR){
              if ( data.results ) {
                notifySuccess(null, eip_allocate_success);
                thisObj.tableWrapper.eucatable('refreshTable');
              } else {
                notifyError(null, eip_allocate_error);
              }
            },
          error:
            function(jqXHR, textStatus, errorThrown){
              notifyError(null, eip_allocate_error);
            }
        });
    },

    _associateIp : function (publicIp, instanceId) {
      var thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=AssociateAddress&PublicIp="+publicIp+"&InstanceId="+instanceId,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        cache:false,
        async: false,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, eip_associate_success);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, eip_associate_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
            notifyError(null, eip_associate_error);
          }
      });
    },

    _initAssociateDialog : function(dfd) {  // should resolve dfd object
      var $instanceSelector = thisObj.associateDialog.find('#eip-associate-instance-id').html('');
      var results = describe('instance');
      var volume_ids = [];
      if ( results ) {
        for( res in results) {
          instance = results[res];
          if ( instance.state === 'running' ) 
            volume_ids.push(instance.id);
        }
      }
      if ( volume_ids.length == 0 )
        this.associateDialog.eucadialog('showError', no_running_instances);
      $instanceSelector.autocomplete({
        source: volume_ids
      });
      dfd.resolve();
    },

    _releaseAction : function() {
      var thisObj = this;
      eipsToRelease = thisObj.tableWrapper.eucatable('getSelectedRows', 1);
      var matrix = [];
      $.each(eipsToRelease,function(idx, key){
        matrix.push([key]);
      });
      if ( eipsToRelease.length > 0 ) {
        thisObj.releaseDialog.eucadialog('setSelectedResources', {title:[ip_address_label], contents: matrix});
        thisObj.releaseDialog.dialog('open');
      }
    },

    _disassociateAction : function(){
      var thisObj = this;
      var rows = thisObj.tableWrapper.eucatable('getSelectedRows');
      if ( rows.length > 0 ) {
        var matrix = [];
        $.each(rows, function(idx, ip){
          matrix.push([ip.public_ip, ip.instance_id]); 
        });
        this.disassociateDialog.eucadialog('setSelectedResources', {title: [ip_address_label, instance_label], contents: matrix});
        this.disassociateDialog.dialog('open');
      }
    },


    _associateAction : function() {
      var thisObj = this;
      eipsToAssociate = thisObj.tableWrapper.eucatable('getSelectedRows', 1);

      if ( eipsToAssociate.length == 1 ) {
        thisObj.associateDialog.find("#eip-to-associate").html(eipsToAssociate[0]);
        thisObj.associateDialog.dialog('open');
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
