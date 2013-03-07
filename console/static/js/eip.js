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
    options : { 
      from_instance : false,
    },
    baseTable : null,
    tableWrapper : null,
    releaseDialog : null,
    allocateDialog : null,
    associateDialog : null,
    disassociateDialog : null,
    associateBtnId : 'eip-associate-btn',

    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #eipTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_eip)));
      var $eipTable = $wrapper.children().first();
      var $eipHelp = $wrapper.children().last();
      this.baseTable = $eipTable;
      this.tableWrapper = $eipTable.eucatable({
        id : 'eips', // user of this widget should customize these options,
        data_deps: ['addresses'],
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'eip',
          "aoColumnDefs": [
            {
	      // Display the checkbox button in the main table
              "bSortable": false,
              "aTargets":[0],
              "mData": function(source) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell",
            },
            {
	      // Display the allocated public IP in eucatable
	      "aTargets":[1],
      	      "mRender": function(data) {
		return DefaultEncoder().encodeForHTML(data);
	      },
              "mData": "public_ip",
              "iDataSort": 4
            },
            { 
	      // Display the instance ID in eucatable
	      "aTargets":[2],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "instance_id",
	    },
            {
	      // Invisible Column for storing the status of the IP
              "bVisible": false,
              "aTargets":[3],
              "mData": function(source) {
                if (!source.instance_id)
                    return 'unassigned';
                else if (source.instance_id.indexOf('available') >= 0)
                  return 'unassigned';
                else if (source.instance_id.indexOf('nobody') >= 0)
                  return 'unallocated';
                else
                  return 'assigned'
              },
            },
            {
	      // Invisialbe Column for allowing the IPs to be sorted
              "bVisible": false,
              "aTargets":[4],
              "mData": function(source) {
                return ipv4AsInteger(source.public_ip);
              },
            }
          ],
        },
        text : {
          header_title : eip_h_title,
          create_resource : eip_allocate,
          resource_found : 'eip_found',
          resource_search : eip_search,
          resource_plural : eip_plural,
        },
        menu_actions : function(args){ 
          return thisObj._createMenuActions();
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        menu_click_create : function (args) { thisObj.createAction() },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $eipHelp, url: help_eip.landing_content_url});
        },
        filters : [{name:"eip_state", options: ['all','assigned','unassigned'], filter_col:3, alias: {'assigned':'assigned','unassigned':'unassigned'}, text: [eip_state_selector_all,eip_state_selector_assigned,eip_state_selector_unassigned] }],
      });
      this.tableWrapper.appendTo(this.element);
      $('html body').eucadata('addCallback', 'eip', 'eip-landing', function() {
        thisObj.tableWrapper.eucatable('redraw');
      });
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
           'release': {text: eip_release_dialog_release_btn, click: function() {
                var rowsToDelete = thisObj.releaseDialog.eucadialog('getSelectedResources', 0);
                $release_dialog.eucadialog("close");
                thisObj._releaseListedIps(rowsToDelete);
            }},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $release_dialog.eucadialog("close");}}
         },
         help: { content: $release_help, url: help_eip.dialog_release_content_url },
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
                  $eip_allocate_dialog.eucadialog("close");
                  thisObj._allocateIps(numberOfIps);
                }
              } 
            },
           'cancel': { text: dialog_cancel_btn, click: function() { $eip_allocate_dialog.eucadialog("close"); } }
         },
         help: { content: $eip_allocate_dialog_help, url: help_eip.dialog_allocate_content_url },
       });
      var $ip_count_edit = $eip_allocate_dialog.find('#eip-allocate-count');
      $eip_allocate_dialog.eucadialog('buttonOnKeyup', $ip_count_edit,  allocateButtonId, function(){
        var count = parseInt($ip_count_edit.val());
        return $ip_count_edit.val() == count && count > 0;
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
           'associate': { domid: this.associateBtnId, text: eip_associate_dialog_associate_btn, disabled: true, click: function() {
               fixedValue = $eip_associate_dialog.find('#associate-fixed-value').html();
               selectedValue = $eip_associate_dialog.find('#associate-selected-value').val();
               if (thisObj.options.from_instance) {
                 if ( isValidIPv4Address(selectedValue) ) {
                   $eip_associate_dialog.eucadialog("close");
                   thisObj._associateIp(selectedValue, fixedValue);
                 } else {
                   thisObj.associateDialog.eucadialog('showFieldError', '#associate-selected-value', eip_associate_invalid_ip_address);
                 }
               } else {
                 $eip_associate_dialog.eucadialog("close");
                 thisObj._associateIp(fixedValue, selectedValue);
               }
              } 
            },
           'cancel': { text: dialog_cancel_btn, click: function() { $eip_associate_dialog.eucadialog("close"); } }
         },
         help: { content: $eip_associate_dialog_help, url: help_eip.dialog_associate_content_url },
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
               var ipsToDisassociate = thisObj.disassociateDialog.eucadialog('getSelectedResources', 0);
               $eip_disassociate_dialog.eucadialog("close");
               thisObj._disassociateListedIps(ipsToDisassociate);
              } 
            },
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $eip_disassociate_dialog.eucadialog("close"); } }
         },
         help: { content: $eip_disassociate_dialog_help, url: help_eip.dialog_disassociate_content_url },
       });
      // disassociate eip dialog end
    },

    _destroy : function() {
    },

    _createMenuActions : function() {
      var thisObj = this;
      selectedEips = thisObj.baseTable.eucatable('getSelectedRows', 3);
      var itemsList = {};

      (function(){
        itemsList['associate'] = { "name": eip_action_associate, callback: function(key, opt) {;}, disabled: function(){ return true;} } 
        itemsList['disassociate'] = { "name": eip_action_disassociate, callback: function(key, opt) {;}, disabled: function(){ return true;} }
        itemsList['release'] = { "name": eip_action_release, callback: function(key, opt) {;}, disabled: function(){ return true;} }
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

    _releaseListedIps : function (rowsToDelete) {
      var thisObj = this;
      var done = 0;
      var all = rowsToDelete.length;
      var error = [];

      doMultiAjax(rowsToDelete, function(item, dfd){
        var eipId = item;
        $.ajax({
          type:"POST",
          url:"/ec2?Action=ReleaseAddress",
          data:"_xsrf="+$.cookie('_xsrf')+"&PublicIp="+eipId,
          dataType:"json",
          async:false,
          cache:false,
          success:
          (function(eipId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                ;
              } else {
                error.push({id:eipId, reason: undefined_error});
              }
           }
          })(eipId),
          error:
          (function(eipId) {
            return function(jqXHR, textStatus, errorThrown){
              error.push({id:eipId, reason:  getErrorMessage(jqXHR)});
            }
          })(eipId),
          complete: (function(eipId) {
            return function(jqXHR, textStatus){
              done++;
              if(done < all)
                notifyMulti(100*(done/all), $.i18n.prop('eip_release_progress', all));
              else {
		// XSS Node:: 'eip_release_fail' would contain a chunk HTML code in the failure description string.
		// Message - Failed to send release request to Cloud for {0} IP address(es). <a href="#">Click here for details. </a>
		// For this reason, the message string must be rendered as html()
                var $msg = $('<div>').addClass('multiop-summary').append(
                  $('<div>').addClass('multiop-summary-success').html($.i18n.prop('eip_release_done', (all-error.length), all)));
                if (error.length > 0)
                  $msg.append($('<div>').addClass('multiop-summary-failure').html($.i18n.prop('eip_release_fail', error.length)));
                notifyMulti(100, $msg.html(), error);
                thisObj.tableWrapper.eucatable('refreshTable');
              }
              dfd.resolve();
            }
          })(eipId),
        });
      }, 100);
    },

    _disassociateListedIps : function (ipsToDisassociate) {
      var thisObj = this;
      var done = 0;
      var all = ipsToDisassociate.length;
      var error = [];
      doMultiAjax(ipsToDisassociate, function(item, dfd){
        var eipId = item;
        $.ajax({
          type:"POST",
          url:"/ec2?Action=DisassociateAddress",
          data:"_xsrf="+$.cookie('_xsrf')+"&PublicIp="+eipId,
          dataType:"json",
          async:false,
          cache:false,
          success:
          (function(eipId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                ;
              } else {
                error.push({id:eipId, reason: undefined_error});
              }
           }
          })(eipId),
          error:
          (function(eipId) {
            return function(jqXHR, textStatus, errorThrown){
              error.push({id:eipId, reason:  getErrorMessage(jqXHR)});
            }
          })(eipId),
          complete: (function(eipId) {
            return function(jqXHR, textStatus){
              done++;
              if(done < all)
                notifyMulti(100*(done/all), $.i18n.prop('eip_disassociate_progress', all));
              else {
		// XSS Node:: 'eip_disassociate_fail' would contain a chunk HTML code in the failure description string.
		// Message Example - Failed to send release request to Cloud for {0} IP address(es). <a href="#">Click here for details. </a>
		// For this reason, the message string must be rendered as html()
                var $msg = $('<div>').addClass('multiop-summary').append(
                  $('<div>').addClass('multiop-summary-success').html($.i18n.prop('eip_disassociate_done', (all-error.length), all)));
                if (error.length > 0)
                  $msg.append($('<div>').addClass('multiop-summary-failure').html($.i18n.prop('eip_disassociate_fail', error.length)));
                notifyMulti(100, $msg.html(), error);
                thisObj.tableWrapper.eucatable('refreshTable');
              }
              dfd.resolve();
            }
          })(eipId),
        });
      }, 100);
    },

    _allocateIps : function (numberIpsToAllocate) {
      var thisObj = this;
      var arrayIps = [];
      for ( i=0; i<numberIpsToAllocate; i++)
        arrayIps.push(0);

      var done = 0;
      var all = arrayIps.length;
      var error = [];
      var allocatedIps = [];
      doMultiAjax(arrayIps, function(item, dfd){
        $.ajax({
          type:"POST",
          url:"/ec2?Action=AllocateAddress",
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          cache:false,
          async:false,
          success: function(data, textStatus, jqXHR){
              if ( data.results ) {
                ip = data.results.public_ip;
                allocatedIps.push(ip);
              } else {
                error.push({id:'unknown', reason: undefined_error});
              }
          },
          error: function(jqXHR, textStatus, errorThrown){
              error.push({id:'unknown', reason:  getErrorMessage(jqXHR)});
          },
          complete: function(jqXHR, textStatus){
            done++;
            if(done < all)
              notifyMulti(100*(done/all), $.i18n.prop('eip_allocate_progress', all));
            else {
	      // XSS Node:: 'eip_allocate_fail' would contain a chunk HTML code in the failure description string.
	      // Message Example - Failed to send release request to Cloud for {0} IP address(es). <a href="#">Click here for details. </a>
	      // For this reason, the message string must be rendered as html()
              var $msg = $('<div>').addClass('multiop-summary').append(
                $('<div>').addClass('multiop-summary-success').html($.i18n.prop('eip_allocate_done', (all-error.length), all)));
              if (error.length > 0)
                $msg.append($('<div>').addClass('multiop-summary-failure').html($.i18n.prop('eip_allocate_fail', error.length)));
              notifyMulti(100, $msg.html(), error);
              thisObj.tableWrapper.eucatable('refreshTable');
              $.each(allocatedIps, function(idx, ip){
                thisObj.tableWrapper.eucatable('glowRow', ip);
              });
            }
            dfd.resolve();
          }
        });
      }, 100);
    },

    _associateIp : function (publicIp, instanceId) {
      var thisObj = this;
      $.ajax({
        type:"POST",
        url:"/ec2?Action=AssociateAddress",
        data:"_xsrf="+$.cookie('_xsrf')+"&PublicIp="+publicIp+"&InstanceId="+instanceId,
        dataType:"json",
        cache:false,
        async: false,
        success:
          function(data, textStatus, jqXHR){
            if ( data.results ) {
              notifySuccess(null, $.i18n.prop('eip_associate_success', publicIp, instanceId));
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError($.i18n.prop('eip_associate_error', publicIp, instanceId), undefined_error);
            }
          },
        error:
          function(jqXHR, textStatus, errorThrown){
              notifyError($.i18n.prop('eip_associate_error', publicIp, instanceId), getErrorMessage(jqXHR));
          }
      });
    },

    _initAssociateDialog : function(dfd) {  
      var thisObj = this;
      var $selector = thisObj.associateDialog.find('#associate-selected-value').html('');
      if(! thisObj.options.from_instance){ 
        var results = describe('instance');
        var inst_ids = [];
        if ( results ) {
          for( res in results) {
            var instance = results[res];
            if ( instance._state.name === 'running' )		// instance.state was returning 'undefined'. Missed from David's Fix. Austin, 022813 
              inst_ids.push(instance.id);
          }
        }
        if ( inst_ids.length === 0 )
          this.associateDialog.eucadialog('showError', no_running_instances);
        var sorted_ids = sortArray(inst_ids);
        $selector.autocomplete({
          source: sorted_ids,
          select: function() { thisObj.associateDialog.eucadialog('activateButton', thisObj.associateBtnId); }
        });
        $selector.watermark(instance_id_watermark);
      }else{ // called from instance landing page
        var results = describe('eip');
        var addresses = [];
        if (results) {
          for( res in results){
            var addr = results[res];
            if ( ! addr.instance_id )
              addresses.push(addr.public_ip);
          }
        } 
        if (addresses.length ===0 )
          this.associateDialog.eucadialog('showError', no_available_address);
        var sorted = sortArray(addresses);
        $selector.autocomplete({
          source: sorted,
          select: function() { thisObj.associateDialog.eucadialog('activateButton', thisObj.associateBtnId); }
        });
        $selector.watermark(address_watermark);
      }
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
      thisObj.dialogDisassociateIp(rows);
    },


    _associateAction : function() {
      var thisObj = this;
      var eipsToAssociate = thisObj.tableWrapper.eucatable('getSelectedRows', 1);
      thisObj.dialogAssociateIp(eipsToAssociate[0], null);
    },

/**** Public Methods ****/
    dialogAssociateIp : function(ip, instance){
      var thisObj = this;
      if(ip){
        thisObj.associateDialog.find('#associate-fixed-value').text(ip);
        thisObj.associateDialog.find('#eip-associate-instance-txt').text(eip_associate_dialog_text(ip));
        thisObj.associateDialog.dialog('open');
      }else if(instance){
        thisObj.associateDialog.find('#associate-fixed-value').text(instance);
        thisObj.associateDialog.find('#eip-associate-instance-txt').text(instance_dialog_associate_ip_text(instance));
        thisObj.associateDialog.dialog('open');
      }
    },
    dialogDisassociateIp : function(addresses){
      var thisObj = this;
      if ( addresses.length > 0 ) {
        var matrix = [];
        $.each(addresses, function(idx, ip){
          matrix.push([ip.public_ip, ip.instance_id]); 
        });
        thisObj.disassociateDialog.eucadialog('setSelectedResources', {title: [ip_address_label, instance_label], contents: matrix});
        thisObj.disassociateDialog.dialog('open');
      }
    },
    createAction : function() {
      this.allocateDialog.eucadialog('open');
    },
    close: function() {
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})
(jQuery, window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
