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
    emiToManifest : {},
    emiToPlatform : {},
    instVolMap : {},// {i-123456: {vol-123456:attached,vol-234567:attaching,...}}
    instIpMap : {}, // {i-123456: 192.168.0.1}

    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #instanceTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $instTable = $wrapper.children().first();
      var $instHelp = $wrapper.children().last();

      this._reloadData();
      this.element.add($instTable);
       $.when( 
        (function(){
          var dfd = $.Deferred();
          $.when(thisObj._getEmi())
           .then(function(){thisObj._mapVolumeState()}, function(){ dfd.resolve();})
           .then(function(){thisObj._mapIp()}, function(){dfd.resolve();})
           .done(function(){dfd.resolve()})
           .fail(function(){dfd.resolve()})
          return dfd.promise(); 
        })()
       ).done(function(out){
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
          menu_actions : function(args){
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
        }) //end of eucatable
        thisObj.tableWrapper.appendTo(thisObj.element);



      }); // end of done()
    },
    _create : function() { 
      var thisObj = this;
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
    },

    _destroy : function() {
    },

    descVolRepeat : null,
    descAddrRepeat : null,  
    _reloadData : function() {
      var thisObj = this;
      thisObj.descVolRepeat = runRepeat(function(){return thisObj._mapVolumeState(); },10000);
      thisObj.descAddrRepeat = runRepeat(function(){return thisObj._mapIp();}, 10000);
      /* to cancel later: cancelRepeat(thisObj.descVolRepeat);
         to clear all repeat: clearRepeat();
      */
    },
    
    _getEmi : function() {
      var thisObj = this;
      return $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeImages",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            $.each(data.results, function(idx, img){
               thisObj.emiToManifest[img['name']] = img['location'];
               thisObj.emiToPlatform[img['name']] = img['platform'];
            });
            } else {
                  ;//TODO: how to notify errors?
            }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
           ;//TODO: how to notify errors?
        }
      });
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
       menuItems['attach'] = {"name":instance_action_attach, callback: function(key, opt) { ; }}
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
         menuItems['detach'] = {"name":instance_action_detach, callback: function(key, opt) { ; }}
     }

     // TODO: assuming associate-address is valid for only running/pending instances
     if(numSelected === 1 && ('running' in stateMap || 'pending' in stateMap) && ($.inArray(instIds[0], stateMap['running']>=0) || $.inArray(instIds[0], stateMap['pending'] >=0)))
       menuItems['associate'] = {"name":instance_action_associate, callback: function(key, opt){; }}
  
     // TODO: assuming disassociate-address is for only one selected instance 
     if(numSelected  === 1 && instIds[0] in thisObj.instIpMap){
       menuItems['disassociate'] = {"name":instance_action_disassociate, callback: function(key, opt){;}}
     }
 
     return menuItems;
    },
    _countVol : 0,
    // TODO: should be auto-reloaded
    _mapVolumeState : function() {
      var thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeVolumes",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            thisObj.instVolMap = {};
            $.each(data.results, function(idx, volume){
              if (volume.attach_data && volume.attach_data['status']){
                var inst = volume.attach_data['instance_id'].toLowerCase();
                var state = volume.attach_data['status'];
                var vol_id = volume.id;
                if(!(inst in thisObj.instVolMap))
                  thisObj.instVolMap[inst] = {};
                var vols = thisObj.instVolMap[inst];
                $.extend(vols, {vol_id:state})
              } 
            });
          } else { ; }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
          ; }
      });
    },

    // TODO: should be auto-reloaded
    _mapIp : function() {
      var thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=DescribeAddresses",
        data:"_xsrf="+$.cookie('_xsrf'),
        dataType:"json",
        async:"false",
        success: function(data, textStatus, jqXHR){
          if (data.results) {
            thisObj.instIpMap = {};
            $.each(data.results, function(idx, addr){
              if (addr['instance_id'] && addr['instance_id'].length > 0){
                var instId = addr['instance_id'];
                instId = instId.substring(0, 10); 
                instId = instId.toLowerCase();
                thisObj.instIpMap[instId] = addr['public_ip'];
              }
            });
          }else{ ; }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
          ; }
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
/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
