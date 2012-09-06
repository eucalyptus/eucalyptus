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
  $.widget('eucalyptus.instance', $.eucalyptus.eucawidget, {
    options : { },
    tableWrapper : null,
    termDialog : null,
    rebootDialog : null,
    stopDialog : null,
    connectDialog : null,
    consoleDialog : null,
    attachDialog : null,
    detachDialog : null,
    passwordDialog : null,
    emiToManifest : {},
    emiToPlatform : {},
    instVolMap : {},// {i-123456: {vol-123456:attached,vol-234567:attaching,...}}
    instIpMap : {}, // {i-123456: 192.168.0.1}
    instPassword : {}, // only windows instances

    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #instanceTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $instTable = $wrapper.children().first();
      var $instHelp = $wrapper.children().last();
      this._getEmi();
      thisObj.tableWrapper = $instTable.eucatable({
        id : 'instances', // user of this widget should customize these options,
        dt_arg : {
          "sAjaxSource": "../ec2?Action=DescribeInstances",
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { // platform
              "fnRender" : function(oObj) { 
               if (thisObj.emiToPlatform[oObj.aData.image_id])
                 return thisObj.emiToPlatform[oObj.aData.image_id];
               else
                 return "linux";
               }
            },
            { "mDataProp": "id" },
            { "mDataProp": "state" },
            { "mDataProp": "image_id" }, 
            { "mDataProp": "placement" }, // TODO: placement==zone?
            { "mDataProp": "ip_address" },
            { "mDataProp": "private_ip_address" },
            { "mDataProp": "key_name" },
            { "mDataProp": "group_name" },
            // output creation time in browser format and timezone
            { "fnRender": function(oObj) { d = new Date(oObj.aData.launch_time); return d.toLocaleString(); } },
            {
              "bVisible": false,
              "mDataProp": "root_device_type"
            },
          ]
        },
        text : {
          header_title : instance_h_title,
          create_resource : instance_create,
          resource_found : instance_found,
        },
        menu_click_create : function(e) {
          var $container = $('html body').find(DOM_BINDING['main']);
          $container.maincontainer("changeSelected", e, {selected:'launcher'});
        },
        menu_actions : function(args){
          return thisObj._createMenuActions(); 
        },
        context_menu_actions : function(row) {
          return thisObj._createMenuActions();
        },
        help_click : function(evt) {
          // TODO: make this a reusable operation
          thisObj._flipToHelp(evt,$instHelp);
        },
        draw_cell_callback : function(row, col, val){
          if(col===4){
            if(!thisObj.emiToManifest[val])
              return val; // in case of error, print EMI
            else
              return thisObj.emiToManifest[val];
          }else
            return val;
        },
        filters : [{name:"inst_state", options: ['all','running','pending','stopped','terminated'], text: [instance_state_selector_all,instance_state_selector_running,instance_state_selector_pending,instance_state_selector_stopped,instance_state_selector_terminated], filter_col:3}, 
                   {name:"inst_type", options: ['all', 'ebs','instance-store'], text: [instance_type_selector_all, instance_type_selector_ebs, instance_type_selector_instancestore], filter_col:11}],
      }) //end of eucatable
      thisObj.tableWrapper.appendTo(thisObj.element);
    },
    _create : function() { 
      var thisObj = this;
      thisObj._reloadData();
      var  $tmpl = $('html body').find('.templates #instanceTermDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $term_dialog = $rendered.children().first();
      var $term_help = $rendered.children().last();
      this.termDialog = $term_dialog.eucadialog({
        id: 'instances-terminate',
        title: instance_dialog_term_title,
        buttons: {
          'terminate': {text: instance_dialog_term_btn, click: function() { thisObj._terminateInstances(); $term_dialog.eucadialog("close");}},
          'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $term_dialog.eucadialog("close");}}
        },
        help: {content: $term_help},
      });

      $tmpl = $('html body').find('.templates #instanceRebootDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $reboot_dialog = $rendered.children().first();
      var $reboot_help = $rendered.children().last();
      this.rebootDialog = $reboot_dialog.eucadialog({
        id: 'instances-reboot',
        title: instance_dialog_term_title,
        buttons: {
          'reboot': {text: instance_dialog_reboot_btn, click: function() { thisObj._rebootInstances(); $reboot_dialog.eucadialog("close");}},
          'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $reboot_dialog.eucadialog("close");}}
        },
        help: {content: $reboot_help},
      });
      
      $tmpl = $('html body').find('.templates #instanceStopDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $stop_dialog = $rendered.children().first();
      var $stop_help = $rendered.children().last();
      this.stopDialog = $stop_dialog.eucadialog({
        id: 'instances-stop',
        title: instance_dialog_stop_title,
        buttons: {
          'stop': {text: instance_dialog_stop_btn, click: function() { thisObj._stopInstances(); $stop_dialog.eucadialog("close");}},
          'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $stop_dialog.eucadialog("close");}}
        },
        help: {content: $stop_help},
      });
      
      $tmpl = $('html body').find('.templates #instanceConnectDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $connect_dialog = $rendered.children().first();
      var $connect_help = $rendered.children().last();
      this.connectDialog = $connect_dialog.eucadialog({
        id: 'instances-connect',
        title: instance_dialog_connect_title,
        buttons: {
          'close': {text: dialog_close_btn, focus:true, click: function() { $connect_dialog.eucadialog("close");}}
        },
        help: {content: $connect_help},
      });
      
      $tmpl = $('html body').find('.templates #instanceConsoleDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $console_dialog = $rendered.children().first();
      var $console_help = $rendered.children().last();
      this.consoleDialog = $console_dialog.eucadialog({
        id: 'instances-console',
        title: instance_dialog_console_title,
        buttons: {
          'close': {text: dialog_close_btn, focus:true, click: function() { $console_dialog.eucadialog("close");}}
        },
        help: {content: $console_help},
      });

      // volume detach dialog start
      $tmpl = $('html body').find('.templates #volumeDetachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $detach_dialog = $rendered.children().first();
      var $detach_help = $rendered.children().last();
  
      $detach_dialog.append(
        $('<div>').addClass('dialog-field').append(
          $('<span>').addClass('label').text(volume_dialog_detach_volume_label),
          $('<select>').attr('id','volume-detach-volume-selector')));

      this.detachDialog = $detach_dialog.eucadialog({
         id: 'volumes-detach',
         title: volume_dialog_detach_title,
         buttons: {
           'detach': {text: volume_dialog_detach_btn, domid: 'btn-vol-detach',disabled:true, click: function() { thisObj._detachVolume(); $detach_dialog.eucadialog("close");}},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $detach_dialog.eucadialog("close");}} 
         },
         help: {title: help_volume['dialog_detach_title'], content: $detach_help},
       });
      var $volSelector =$detach_dialog.find('#volume-detach-volume-selector'); 
      this.detachDialog.eucadialog('buttonOnChange',$volSelector, '#btn-vol-detach', function() { return $volSelector.val() ? true : false; });
      
      // volume detach dialog end
      // attach dialog start
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
                $attach_dialog.eucadialog("close");
              } 
            },
           'cancel': { text: dialog_cancel_btn, focus:true, click: function() { $attach_dialog.eucadialog("close"); } }
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
      // attach dialog end

      $tmpl = $('html body').find('.templates #instancePasswordDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $password_dialog = $rendered.children().first();
      var $password_help = $rendered.children().last();
      this.passwordDialog = $password_dialog.eucadialog({
        id: 'instances-password',
        title: instance_dialog_password_title,
        buttons: {
          'password' : {text: instance_dialog_password_btn, click: function() { thisObj._getPassword(); $password_dialog.eucadialog("close");}},
          'close': {text: dialog_close_btn, focus:true, click: function() { $password_dialog.eucadialog("close");}}
        },
        help: {content: $password_help},
      });
      var $fileSelector = thisObj.passwordDialog.find('input#fileupload');
      $fileSelector.fileupload( {
        dataType: 'json',
        url: "../ec2",
        fileInput: null,
        done: function (e, data) {
          $.each(data.result, function (index, result) {
            thisObj.instPassword[result.instance] = result.password;
            notifyError(null, instance_dialog_password_success+' : '+ result.password);
          });
        },
        fail : function (e, data) { notifyError(null, instance_dialog_password_error); },
      });
    },

    _destroy : function() { },

    _getEmi : function() {
      var thisObj = this;
      var results = describe('image');
      $.each(results, function(idx, img){
        thisObj.emiToManifest[img['name']] = img['location'];
        thisObj.emiToPlatform[img['name']] = img['platform'];
      });
    },

    _reloadData : function() {
      var thisObj = this;
      $('html body').eucadata('addCallback', 'volume', 'instance-vol-map', function(){return thisObj._mapVolumeState();});
      $('html body').eucadata('addCallback', 'eip', 'instance-ip-map', function(){return thisObj._mapIp();});
      $('html body').eucadata('refresh','volume');
      $('html body').eucadata('refresh','eip');
    },

    _createMenuActions : function(args) {
      var thisObj = this;
    // # selected rows, instance_state, attach state, inst type
      var selectedRows = thisObj.tableWrapper.eucatable('getSelectedRows');  
      var numSelected = selectedRows.length;
      var stateMap = {};
      var rootTypeMap = {}; 
      var instIds = [];
      $.each(selectedRows, function(rowIdx, row){
        $.each(row, function(key, val){
          instIds.push(row['id'].toLowerCase());
          if(key==='state') {
            if(!stateMap[val])
              stateMap[val] = [row['id'].toLowerCase()];
            else
              stateMap[val].push(row['id'].toLowerCase()); 
          } else if(key === 'root_device_type')

            if(!rootTypeMap[val])
              rootTypeMap[val] = [row['id'].toLowerCase()];
            else
              rootTypeMap[val].push(row['id'].toLowerCase()); 
        });
      });
     var menuItems = {};
     // add all menu items first
     (function(){
       menuItems['connect'] = {"name":instance_action_connect, callback: function(key, opt) { ; }, disabled: function(){ return true; }};
       menuItems['stop'] = {"name":instance_action_stop, callback: function(key, opt){ ; }, disabled: function(){ return true; }};
       menuItems['start'] = {"name":instance_action_start, callback: function(key, opt){ ; }, disabled: function(){ return true; }};
       menuItems['reboot'] = {"name":instance_action_reboot, callback: function(key, opt){ ; }, disabled: function(){ return true; }};
       menuItems['launchmore'] = {"name":instance_action_launch_more, callback: function(key, opt){ ; }, disabled: function(){ return true; }};
       menuItems['terminate'] = {"name":instance_action_terminate, callback: function(key, opt){ ; }, disabled: function(){ return true; }};
       menuItems['console'] = {"name":instance_action_console, callback: function(key, opt) { ; }, disabled: function(){ return true; }};
       menuItems['attach'] = {"name":instance_action_attach, callback: function(key, opt) { ; }, disabled: function(){ return true; }};
       menuItems['detach'] = {"name":instance_action_detach, callback: function(key, opt) { ; }, disabled: function(){ return true; }};
       menuItems['associate'] = {"name":instance_action_associate, callback: function(key, opt){; }, disabled: function(){ return true; }};
       menuItems['disassociate'] = {"name":instance_action_disassociate, callback: function(key, opt){;}, disabled: function(){ return true; }};
       menuItems['getpassword'] = {"name":instance_action_getpassword, callback: function(key, opt) { ; }, disabled: function(){ return true; }};
     })();

     if(numSelected === 1 && 'running' in stateMap && $.inArray(instIds[0], stateMap['running']>=0)){
       menuItems['connect'] = {"name":instance_action_connect, callback: function(key, opt) { thisObj._connectAction(); }}
     }

     if(numSelected >= 1 && 'ebs' in rootTypeMap && !('instance-store' in rootTypeMap) &&
        ('running' in stateMap) && allInArray(instIds, stateMap['running'])){
       menuItems['stop'] = {"name":instance_action_stop, callback: function(key, opt){ thisObj._stopAction(); }}
     }

     if(numSelected >= 1 && 'ebs' in rootTypeMap && !('instance-store' in rootTypeMap) &&
       ('stopped' in stateMap) && allInArray(instIds, stateMap['stopped'])){
       menuItems['start'] = {"name":instance_action_start, callback: function(key, opt){ thisObj._startInstances(); }} 
     }

     if(numSelected >= 1 && ('running' in stateMap) && allInArray(instIds, stateMap['running'])){
       menuItems['reboot'] = {"name":instance_action_reboot, callback: function(key, opt){ thisObj._rebootAction(); }}
     }

     if(numSelected == 1){
       menuItems['launchmore'] = {"name":instance_action_launch_more, callback: function(key, opt){ ; }}
     }
  
     if(numSelected >= 1){ // TODO: no state check for terminate?
       menuItems['terminate'] = {"name":instance_action_terminate, callback: function(key, opt){ thisObj._terminateAction(); }}
     }

     if(numSelected === 1 && 'running' in stateMap && $.inArray(instIds[0], stateMap['running']>=0)){
       menuItems['console'] = {"name":instance_action_console, callback: function(key, opt) { thisObj._consoleAction(); }}
       menuItems['attach'] = {"name":instance_action_attach, callback: function(key, opt) { thisObj._attachAction(); }}
     }
   
     // detach-volume is for one selected instance 
     if(numSelected === 1 && 'running' in stateMap && $.inArray(instIds[0], stateMap['running']>=0) && 
        (instIds[0] in thisObj.instVolMap)){
       var vols = thisObj.instVolMap[instIds[0]];
       var attachedFound = false;
       $.each(vols, function(vol, state){
         if(state==='attached')
           attachedFound = true;
       });   
       if(attachedFound)
         menuItems['detach'] = {"name":instance_action_detach, callback: function(key, opt) { thisObj._detachAction(); }}
     }

     // TODO: assuming associate-address is valid for only running/pending instances
     if(numSelected === 1 && ('running' in stateMap || 'pending' in stateMap) && ($.inArray(instIds[0], stateMap['running']>=0) || $.inArray(instIds[0], stateMap['pending'] >=0)))
       menuItems['associate'] = {"name":instance_action_associate, callback: function(key, opt){; }}
  
     // TODO: assuming disassociate-address is for only one selected instance 
     if(numSelected  === 1 && instIds[0] in thisObj.instIpMap)
       menuItems['disassociate'] = {"name":instance_action_disassociate, callback: function(key, opt){;}}

     if(numSelected === 1 && ('running' in stateMap) && ($.inArray(instIds[0], stateMap['running']>=0)) && (thisObj.tableWrapper.eucatable('getSelectedRows', 1)[0] ==='windows')){
       menuItems['getpassword'] = {"name":instance_action_getpassword, callback: function(key,opt){ thisObj._getPasswordAction(); }}
     }
 
     return menuItems;
    },
    _countVol : 0,
    // TODO: should be auto-reloaded
    _mapVolumeState : function() {
      var thisObj = this;
      var results = describe('volume');
      $.each(results, function(idx, volume){
        if (volume.attach_data && volume.attach_data['status']){
          var inst = volume.attach_data['instance_id'].toLowerCase();
          var state = volume.attach_data['status'];
          var vol_id = volume.id;
          if(!(inst in thisObj.instVolMap))
            thisObj.instVolMap[inst] = {};
          var vols = thisObj.instVolMap[inst];
          vols[vol_id] = state;
        } 
      });
    },

    // TODO: should be auto-reloaded
    _mapIp : function() {
      var thisObj = this;
      var results = describe('eip');
      $.each(results, function(idx, addr){
        if (addr['instance_id'] && addr['instance_id'].length > 0){
          var instId = addr['instance_id'];
          instId = instId.substring(0, 10); 
          instId = instId.toLowerCase();
          thisObj.instIpMap[instId] = addr['public_ip'];
        }
      });
    },

/// Action methods
    _terminateAction : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 2);
      var rootType = thisObj.tableWrapper.eucatable('getSelectedRows', 11);
      if ( instances.length > 0 ) {
        var matrix = [];
        $.each(instances, function(idx,id){
          matrix.push([id]);
        });
        if ($.inArray('ebs',rootType)>=0){
          thisObj.termDialog.eucadialog('addNote','ebs-backed-warning', instance_dialog_ebs_warning); 
        }
        thisObj.termDialog.eucadialog('setSelectedResources', {title: [instance_dialog_term_resource_title], contents: matrix});
        thisObj.termDialog.eucadialog('open');
       }
    },
    _terminateInstances : function(){
      var thisObj = this;
      var instances = thisObj.termDialog.eucadialog('getSelectedResources',0);
      instances = instances.join(' ');
      $.ajax({
          type:"GET",
          url:"/ec2?Action=TerminateInstances&InstanceId=" + instances,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if ( data.results && data.results == true ) {
              notifySuccess(null, instance_terminate_success + ' ' + instances);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, instance_terminate_error + ' ' + instances);
            }
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError(null, instance_terminate_error + ' ' + instances);
          }
        });
    },
    _rebootAction : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 2);
      if ( instances.length > 0 ) {
        var matrix = [];
        $.each(instances, function(idx,id){
          matrix.push([id]);
        });
        thisObj.rebootDialog.eucadialog('setSelectedResources', {title: [instance_dialog_reboot_resource_title], contents: matrix});
        thisObj.rebootDialog.eucadialog('open');
       }
    },
    _rebootInstances : function(){
      var thisObj = this;
      var instances = thisObj.rebootDialog.eucadialog('getSelectedResources',0);
      instances = instances.join(' ');
      $.ajax({
          type:"GET",
          url:"/ec2?Action=RebootInstances&InstanceId=" + instances,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if ( data.results && data.results == true ) {
              notifySuccess(null, instance_reboot_success + ' ' + instances);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, instance_reboot_error + ' ' + instances);
            }
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError(null, instance_reboot_error + ' ' + instances);
          }
        });
    },
    _stopAction : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 2);
      if ( instances.length > 0 ) {
        var matrix = [];
        $.each(instances, function(idx,id){
          matrix.push([id]);
        });
        thisObj.stopDialog.eucadialog('setSelectedResources', {title: [instance_dialog_stop_resource_title], contents: matrix});
        thisObj.stopDialog.eucadialog('open');
       }
    },
    _stopInstances : function(){
      var thisObj = this;
      var instances = thisObj.stopDialog.eucadialog('getSelectedResources',0);
      instances = instances.join(' ');
      $.ajax({
          type:"GET",
          url:"/ec2?Action=StopInstances&InstanceId=" + instances,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if ( data.results && data.results == true ) {
              notifySuccess(null, instance_stop_success + ' ' + instances);
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError(null, instance_stop_error + ' ' + instances);
            }
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError(null, instance_stop_error + ' ' + instances);
          }
        });
    },
    _startInstances : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 2);
      instances = instances.join(' ');
      $.ajax({
        type:"GET",
        url:"/ec2?Action=StartInstances&InstanceId=" + instances,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:true,
        success: function(data, textStatus, jqXHR){
          if ( data.results && data.results == true ) {
            notifySuccess(null, instance_start_success + ' ' + instances);
            thisObj.tableWrapper.eucatable('refreshTable');
          } else {
            notifyError(null, instance_start_error + ' ' + instances);
          }
        },
        error: function(jqXHR, textStatus, errorThrown){
          notifyError(null, instance_start_error + ' ' + instances);
        }
      });
    },
    _connectAction : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 2);
      var oss = thisObj.tableWrapper.eucatable('getSelectedRows', 1);
      if ( instances.length > 0 ) {
        // connect is for one instance 
        var instance = instances[0];
        var os = oss[0]; 
        if(os === 'windows') 
          thisObj.connectDialog.eucadialog('addNote','instance-connect-text',instance_dialog_connect_windows_text);
        else
          thisObj.connectDialog.eucadialog('addNote','instance-connect-text',instance_dialog_connect_linux_text);

        thisObj.connectDialog.eucadialog('open');
       }
    },
    _consoleAction : function() {
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 2);
      instances=instances.join(' ');

      $.when( 
        $.ajax({
          type:"GET",
          url:"/ec2?Action=GetConsoleOutput&InstanceId=" + instances,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:true,
        })).done(function(out){
          var data = out[0];
          if(data && data.results){
            consoleOutput = data.results;   
            thisObj.consoleDialog.eucadialog('addNote', 'instance-console-output', consoleOutput); 
            thisObj.consoleDialog.eucadialog('open');
          }else{
            notifyError(null, instance_console_error + ' ' + instances);
          }
        }).fail(function(out){
          notifyError(null, instance_console_error + ' ' + instances);
        });
    },
    _initAttachDialog : function(dfd) {  // should resolve dfd object
      var thisObj = this;
      var $volumeSelector = $('#volume-attach-volume-selector').html('');
      $volumeSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_attach_select_volume']));
      var results = describe('volume');
      if ( results ) {
        for( res in results) {
          var volume = results[res];
          if ( volume.status === 'available' ) 
            $volumeSelector.append($('<option>').attr('value', volume.id).text(volume.id));
        }
      }
      thisObj.attachDialog.find('#volume-attach-device-name').val('');
      dfd.resolve();
    },
    _attachAction : function() {
      var thisObj = this;
      var instanceToAttach = thisObj.tableWrapper.eucatable('getSelectedRows', 2)[0];
      var $instanceSelector = this.attachDialog.find('#volume-attach-instance-selector');
      $instanceSelector.html('');
      $instanceSelector.append($('<option>').attr('value', instanceToAttach).text(instanceToAttach));
      $instanceSelector.attr('disabled', 'disabled');
      this.attachDialog.eucadialog('open');
    },
    _attachVolume : function (volumeId, instanceId, device) {
      var thisObj = this;
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
    _detachAction : function(){
      var thisObj = this;
      var instance = thisObj.tableWrapper.eucatable('getSelectedRows', 2)[0];
      thisObj.detachDialog.eucadialog('setSelectedResources', {title: [volume_dialog_detach_resource_label], contents: [[instance]]});
     
      var results = describe('volume');
      var $volumeSelector = thisObj.detachDialog.find('#volume-detach-volume-selector').html('');
      $volumeSelector.append($('<option>').attr('value', '').text($.i18n.map['volume_detach_select_volume']));
      $.each(results, function(idx, volume){
        if (volume.attach_data && volume.attach_data['status']){
          var inst = volume.attach_data['instance_id'];
          var state = volume.attach_data['status'];
          var vol_id = volume.id;
          if( state==='attached' && inst ===instance){
            $volumeSelector.append($('<option>').attr('value', vol_id).text(vol_id));
          }
        } 
      });
      thisObj.detachDialog.eucadialog('open');
    },

    _detachVolume : function (force) {
      var thisObj = this;
      var $volumeSelector = thisObj.detachDialog.find('#volume-detach-volume-selector');
      var volumeId = $volumeSelector.val();

      $.ajax({
        type:"GET",
        url:"/ec2?Action=DetachVolume&VolumeId=" + volumeId,
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:true,
        success: (function(volumeId) {
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
         error: (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              if (force)
                notifyError(null, volume_force_detach_error + ' ' + volumeId);
              else
                notifyError(null, volume_detach_error + ' ' + volumeId);
            }
          })(volumeId)
      });
    },
    _getPasswordAction : function() {
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 2);
      var keynames = thisObj.tableWrapper.eucatable('getSelectedRows', 8);
      if ( instances.length > 0 ) {
        if(thisObj.instPassword[instances[0]]){
          notifyError(null, instance_dialog_password_success+' : '+ thisObj.instPassword[instances[0]]);
        }else{
          // connect is for one instance 
          var instance = instances[0];
          var keyname = keynames[0];
          var matrix = [[instance, keyname]];
          thisObj.passwordDialog.eucadialog('setSelectedResources', {title: [instance_dialog_password_resource_label_instance,instance_dialog_password_resource_label_keyname], contents: matrix});
          thisObj.passwordDialog.eucadialog('open');
        }
      }
    },
    _getPassword : function() {
      var thisObj = this; 
      var $fileSelector = thisObj.passwordDialog.find('input#fileupload');
      var instanceId = thisObj.passwordDialog.eucadialog('getSelectedResources',0).join(' ');
      $fileSelector.fileupload( "add", {
        files: $fileSelector.files,
        fileInput: $fileSelector,
        formData: [{name: 'Action', value: 'GetPassword'}, {name:'InstanceId', value:instanceId}, {name:'_xsrf', value:$.cookie('_xsrf')}],  
      });
    },
/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
