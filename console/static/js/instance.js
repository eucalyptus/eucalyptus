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
    options : {
      state_filter : null,
    },
    tableWrapper : null,
    termDialog : null,
    rebootDialog : null,
    stopDialog : null,
    connectDialog : null,
    consoleDialog : null,
    detachDialog : null,
    launchMoreDialog : null,
    tagDialog : null,
    instVolMap : {},// {i-123456: {vol-123456:attached,vol-234567:attaching,...}}
    instIpMap : {}, // {i-123456: 192.168.0.1}
    instPassword : {}, // only windows instances
    detachButtonId : 'btn-vol-detach',
    _init : function() {
            console.log ( "INIT!!!" );
        
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #instanceTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $instTable = $wrapper.children().first();
      var $instHelp = $wrapper.children().last();
      thisObj.tableWrapper = $instTable.eucatable({
        id : 'instances', // user of this widget should customize these options,
        data_deps: ['instances', 'volumes', 'addresses'],
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'instance',
          "aaSorting": [[ 10, "desc" ]],
          "aoColumnDefs": [
            {
	      // Display the checkbox button in the main table
              "bSortable": false,
              "aTargets":[0],
              "mData": function(source) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell",
            },
            { 
	      // Hidden column for displaying the platform of the instance
              "bVisible": false,
              "aTargets":[1],
              "mData": function(source) { 
			var result = describe('image', source.image_id);
               		if(result && result.platform) 
				return result.platform;
			return "linux";
              },
            },
            { 
	      // Display the display_id of the instance in the main table
	      // If display_id doesn't exist, display its id
	      // Allow the id to be clickable to display the platform data above
	      // Use CSS 'twist'
	      "aTargets":[2],
              "mData": function(source){
                this_title = source.id;
                this_id = source.id;
                if(source.display_id)
                  this_id = source.display_id;
                return eucatableDisplayColumnTypeTwist(this_title, this_id, 255);
              },
            },
            { 
	      // Display the status of the instance in the main table
              "iDataSort": 12,
              "aTargets":[3],
              "mData": function(source) { 
	         var state = source.state;
                 if (state == undefined) {
                   state = source._state.name;
                 }
                 return eucatableDisplayColumnTypeInstanceStatus(state);
              },
            },
            { 
	      // Display the image id of the instance in the main table
	      "aTargets":[4],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "image_id",
	    },
            { 
	      // Display the availiability zone of the instance in the main table
              "aTargets":[5],
	      "mData": function(source) {
                var zone = source.placement;
                if (zone == undefined) {
                    zone = source._placement.zone;
                }
                return DefaultEncoder().encodeForHTML(zone);
              },
	    }, 
            {
	      // Display the public dns name of the instance in the main table
	      "aTargets":[6],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "public_dns_name",
 	      "sClass": "wrap-content"
	    },
            {
	      // Display the private dns name of the instance in the main table
              "aTargets":[7],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "private_dns_name",
 	      "sClass": "wrap-content"
	    },
            { 
	      // Display the key name of the instance in the main table
              "aTargets":[8],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "key_name",
	    },
            {
	      // Display the group name of the instance in the main table
	      "aTargets":[9],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "group_name",
	    },
            { 
	      // Display the launch time of the instance in the main table
	      "aTargets":[10],
              "mRender": function(data) {
		return formatDateTime(data)
	      },
              "mData": "launch_time",
              "iDataSort": 13,
              "asSorting" : [ 'desc', 'asc' ],
            },
            {
	      // Hidden column for the root device type of the instance
              "bVisible": false,
              "aTargets":[11],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "root_device_type",
            },
            {
	      // Hidden column for the status of the instance
              "bVisible": false,
              "aTargets":[12],
	      "mData": function(source) {
                 var state = source.state;
                 if (state == undefined) {
                   state = source._state.name;
                 }
                return DefaultEncoder().encodeForHTML(state);
              },
            },
            {
	      // Hidden column for the launch time of the instance
              "asSorting" : [ 'desc', 'asc' ],
              "bVisible": false,
              "aTargets":[13],
	      "mRender": function(data) {
                return data;		// Sorting fails when encoded	013113
              },
              "mData": "launch_time",
              "sType": "date",
            },
            {
	      // Hidden column for the image location of the instance
              "bVisible": false,
              "aTargets":[14],
              "mData": function(source) {
			var image = null;
              		var result = describe('image', source.image_id);
			if( result ){
				image = result;
			};
                 	return image ? image.location : '';		
              },
            },
            {
	      // Hidden column for the instance type of the instance
              "bVisible": false,
              "aTargets":[15],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "instance_type",
            },
            {
	      // Hidden column for the ip address of the instance
              "bVisible": false,
              "aTargets":[16],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "ip_address",
            },
            { 
	      // Hidden column for the instance id of the instance
              "bVisible": false,
              "aTargets":[17],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": "id",
            },
            { 
	      // Hidden column for the display_id of the instance
              "bVisible": false,
              "aTargets":[18],
	      "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
              },
              "mData": function(source){
                if(source.display_id)
                  return source.display_id;
                return null;
              },
            },
          ]
        },
        text : {
          header_title : instance_h_title,
          create_resource : instance_create,
          resource_found : 'instance_found',
          resource_search : instance_search,
          resource_plural : instance_plural,
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
          thisObj._flipToHelp(evt,{content: $instHelp, url: help_instance.landing_content_url});
        },
        draw_cell_callback : null, 
        expand_callback : function(row){ // row = [col1, col2, ..., etc]
          return thisObj._expandCallback(row);
        },
        filters : [{name:"inst_state", default: thisObj.options.state_filter, options: ['all','running','pending','stopped','terminated'], text: [instance_state_selector_all,instance_state_selector_running,instance_state_selector_pending,instance_state_selector_stopped,instance_state_selector_terminated], filter_col:12}, 
                   {name:"inst_type", options: ['all', 'ebs','instance-store'], text: [instance_type_selector_all, instance_type_selector_ebs, instance_type_selector_instancestore], filter_col:11}],
        legend : ['running','pending','stopping','stopped','shuttingdown','terminated']
      }) //end of eucatable
      thisObj.tableWrapper.appendTo(thisObj.element);
      $('html body').eucadata('addCallback', 'instance', 'instance-landing', function() {
        thisObj.tableWrapper.eucatable('redraw');
      });
    },
    _create : function() { 
      var thisObj = this;
      thisObj._reloadData();
      var $tmpl = $('html body').find('.templates #instanceTermDlgTmpl').clone();
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
        help: {content: $term_help, url: help_instance.dialog_terminate_content_url},
      });

     // TEMP LOCATION FOR TAG RESOURCE DIALOG
      $tmpl = $('html body').find('.templates #resourceTagWidgetTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $tag_dialog = $rendered.children().first();
      var $tag_help = $rendered.children().last();
      this.tagDialog = $tag_dialog.eucadialog({
        id: 'instances-tag-resource',
        title: 'Add/Edit tags',
        help: {content: $tag_help, url: help_instance.dialog_terminate_content_url},
      });
     // END OF TEMP LOCATION

      $tmpl = $('html body').find('.templates #instanceRebootDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $reboot_dialog = $rendered.children().first();
      var $reboot_help = $rendered.children().last();
      this.rebootDialog = $reboot_dialog.eucadialog({
        id: 'instances-reboot',
        title: instance_dialog_reboot_title,
        buttons: {
          'reboot': {text: instance_dialog_reboot_btn, click: function() { thisObj._rebootInstances(); $reboot_dialog.eucadialog("close");}},
          'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $reboot_dialog.eucadialog("close");}}
        },
        help: {content: $reboot_help, url: help_instance.dialog_reboot_content_url},
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
        help: {content: $stop_help, url: help_instance.dialog_stop_content_url},
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
        title: 'instance_dialog_console_title',
        buttons: {
          'close': {text: dialog_close_btn, focus:true, click: function() { $console_dialog.eucadialog("close");}}
        },
        help: {content: $console_help},
        width: 680,
      });

  // volume detach dialog start
      $tmpl = $('html body').find('.templates #volumeInstDetachDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $detach_dialog = $rendered.children().first();
      var $detach_help = $rendered.children().last();

      this.detachDialog = $detach_dialog.eucadialog({
         id: 'volumes-detach',
         title: volume_dialog_detach_title,
         buttons: {
           'detach': {text: volume_dialog_detach_btn, disabled: true, domid: thisObj.detachButtonId, click: function() { thisObj._detachVolume(); $detach_dialog.eucadialog("close");}},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $detach_dialog.eucadialog("close");}} 
         },
         help: {title: help_volume['dialog_detach_title'], content: $detach_help, url: help_volume.dialog_detach_content_url},
         on_open: {spin: true, callback: function(args) {
           var dfd = $.Deferred();
           thisObj._initDetachDialog(dfd); // pulls instance info from server
           return dfd.promise();
         }},
       });
      // volume detach dialog end
      // launch more instances like this dialog
      $tmpl = $('html body').find('.templates #instanceLaunchMoreDlgTmpl').clone();
      $rendered = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $launchmore_dialog = $rendered.children().first();
      var $launchmore_help = $rendered.children().last();  
    
      this.launchMoreDialog = $launchmore_dialog.eucadialog({
         id: 'launch-more-instances',
         title: instance_dialog_launch_more_title,
         buttons: {
           'launch': {text: instance_dialog_launch_btn, domid: 'btn-launch-more', click: function() { thisObj._launchMore(); $launchmore_dialog.eucadialog("close");}},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $launchmore_dialog.eucadialog("close");}} 
         },
         help: {title: null, content: $launchmore_help, url: help_instance.dialog_launchmore_content_url, pop_height: 600},
         on_open: {callback: function(args) {
           thisObj._initLaunchMoreDialog(); // pulls instance info from server
         }},
         width: 750
      });
      // end launch more dialog
    },

    _destroy : function() { },

    _reloadData : function() {
      var thisObj = this;
      $('html body').eucadata('addCallback', 'volume', 'instance-vol-map', function(){return thisObj._mapVolumeState();});
      $('html body').eucadata('addCallback', 'eip', 'instance-ip-map', function(){return thisObj._mapIp();});
      $('html body').eucadata('refresh','volume');
      $('html body').eucadata('refresh','eip');
    },

    _createMenuActions : function(args) {
      var thisObj = this;
      if(!thisObj.tableWrapper)
        return [];
    // # selected rows, instance_state, attach state, inst type
      var selectedRows = thisObj.tableWrapper.eucatable('getSelectedRows');  
      var numSelected = selectedRows.length;
      var stateMap = {};
      var rootTypeMap = {}; 
      var instIds = [];
      $.each(selectedRows, function(rowIdx, row){
        $.each(row, function(key, val){
          instIds.push(row['id'].toLowerCase());
          if(key==='state' || key=='_state') {
            if (key=='_state') {
                val = val.name;
            }
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
       menuItems['tag'] = {"name":'Tag Resource', callback: function(key, opt){;}, disabled: function(){ return true; }};
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
       menuItems['launchmore'] = {"name":instance_action_launch_more, callback: function(key, opt){ thisObj._launchMoreAction(); }}
     }
  
     if(numSelected >= 1){ // TODO: no state check for terminate? A: terminating terminated instances will delete records
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
         if( state==='attached' && !isRootVolume(instIds[0], vol))
           attachedFound = true;
       });   
       if(attachedFound)
         menuItems['detach'] = {"name":instance_action_detach, callback: function(key, opt) { thisObj._detachAction(); }}
     }

     // TODO: assuming associate-address is valid for only running/pending instances
     if(numSelected === 1 && ('running' in stateMap || 'pending' in stateMap) && ($.inArray(instIds[0], stateMap['running']>=0) || $.inArray(instIds[0], stateMap['pending'] >=0)))
       menuItems['associate'] = {"name":instance_action_associate, callback: function(key, opt){thisObj._associateAction(); }}
  
     // TODO: assuming disassociate-address is for only one selected instance
     // ADJUSTED: More than 1 instance can be selected for disassociate action  --- Kyo 041513 
     if(numSelected  >= 1 ){
       var associatedCount = 0;
       $.each(selectedRows, function(rowIdx, row){
//         console.log("InstanceIPMap: " + thisObj.instIpMap[row['id'].toLowerCase()]);
         if( thisObj.instIpMap[row['id'].toLowerCase()] != null ){
           associatedCount++;
         } 
       });
       if( numSelected == associatedCount ){
         menuItems['disassociate'] = {"name":instance_action_disassociate, callback: function(key, opt){thisObj._disassociateAction();}}
       };
     }

     if(numSelected == 1){
       menuItems['tag'] = {"name":'Tag Resource', callback: function(key, opt){ thisObj._tagResourceAction(); }}
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
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 17);
      var display_ids = thisObj.tableWrapper.eucatable('getSelectedRows', 18);
      var rootType = thisObj.tableWrapper.eucatable('getSelectedRows', 11);
      if ( instances.length > 0 ) {
        var matrix = [];
        // Push the instance id and its display_id into the matrix
        $.each(instances, function(idx,id){
          this_display_id = id;
          if( display_ids[idx] != null )
            this_display_id = display_ids[idx];
          matrix.push([id, this_display_id]);
        });
        if ($.inArray('ebs',rootType)>=0){
          thisObj.termDialog.eucadialog('addNote','ebs-backed-warning', instance_dialog_ebs_warning); 
        }
        // included_display_id : true is added to attach instance ID to the second column in the eucadialog resource table -- Kyo 040113   No Joke
        thisObj.termDialog.eucadialog('setSelectedResources', {title: [instance_label], contents: matrix, included_display_id: true});
        thisObj.termDialog.eucadialog('open');
       }
    },

    _terminateInstances : function(){
      var thisObj = this;
      var instances = thisObj.termDialog.eucadialog('getSelectedResources', 1);    // Now eucadialog's column[1] contains the instance id 
      var toTerminate = instances.slice(0);
      var instIds = '';
      for(i=0; i<instances.length; i++)
        instIds+= '&InstanceId.'+parseInt(i+1)+'='+instances[i];
      $.ajax({
          type:"POST",
          url:"/ec2?Action=TerminateInstances",
          data:"_xsrf="+$.cookie('_xsrf')+instIds,
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if(data.results){
              $.each(data.results, function(idx, instance){
                var toDel = toTerminate.indexOf(instance.id);
                if(toDel>=0)
                  toTerminate.splice(toDel, 1);
              });

              if(toTerminate.length <=0){
                notifySuccess(null, $.i18n.prop('instance_terminate_success',  DefaultEncoder().encodeForHTML(instances.toString())));
                thisObj.tableWrapper.eucatable('refreshTable');
              }else{
                notifyError($.i18n.prop('instance_terminate_error',  DefaultEncoder().encodeForHTML(toTerminate.toString())), undefined_error);
              }
            } else {
              notifyError($.i18n.prop('instance_terminate_error',  DefaultEncoder().encodeForHTML(instances.toString())), undefined_error);
            }
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('instance_terminate_error',  DefaultEncoder().encodeForHTML(instances.toString())), getErrorMessage(jqXHR));
          }
        });
    },
    _rebootAction : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 17);
      var display_ids = thisObj.tableWrapper.eucatable('getSelectedRows', 18);
      if ( instances.length > 0 ) {
        var matrix = [];
        $.each(instances, function(idx,id){
          this_display_id = id;
          if( display_ids[idx] != null )
            this_display_id = display_ids[idx];
          matrix.push([id, this_display_id]);
        });
        thisObj.rebootDialog.eucadialog('setSelectedResources', {title: [instance_label], contents: matrix, included_display_id: true});
        thisObj.rebootDialog.eucadialog('open');
       }
    },
    _rebootInstances : function(){
      var thisObj = this;
      var instances = thisObj.rebootDialog.eucadialog('getSelectedResources', 1);
      //instances = instances.join(' ');
      var instIds = '';
      for(i=0; i<instances.length; i++)
        instIds+= '&InstanceId.'+parseInt(i+1)+'='+instances[i];
     
      $.ajax({
          type:"POST",
          url:"/ec2?Action=RebootInstances",
          data:"_xsrf="+$.cookie('_xsrf')+instIds,
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if ( data.results && data.results == true ) {
              notifySuccess(null, $.i18n.prop('instance_reboot_success',  DefaultEncoder().encodeForHTML(instances.toString())));
              thisObj.tableWrapper.eucatable('refreshTable');
            } else {
              notifyError($.i18n.prop('instance_reboot_error',  DefaultEncoder().encodeForHTML(instances.toString())), undefined_error);
            }
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('instance_reboot_error',  DefaultEncoder().encodeForHTML(instances.toString())), getErrorMessage(jqXHR));
          }
        });
    },
    _stopAction : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 17);
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 18);
      if ( instances.length > 0 ) {
        var matrix = [];
        $.each(instances, function(idx,id){
          this_display_id = id;
          if( display_ids[idx] != null )
            this_display_id = display_ids[idx];
          matrix.push([id, this_display_id]);
        });
        thisObj.rebootDialog.eucadialog('setSelectedResources', {title: [instance_label], contents: matrix, included_display_id: true});
        thisObj.stopDialog.eucadialog('open');
       }
    },
    _stopInstances : function(){
      var thisObj = this;
      var instances = thisObj.stopDialog.eucadialog('getSelectedResources', 1);
      var toStop = instances.slice(0);
      var instIds = '';
      for(i=0; i<instances.length; i++)
        instIds+= '&InstanceId.'+parseInt(i+1)+'='+instances[i];
      $.ajax({
          type:"POST",
          url:"/ec2?Action=StopInstances",
          data:"_xsrf="+$.cookie('_xsrf')+instIds,
          dataType:"json",
          async:true,
          success: function(data, textStatus, jqXHR){
            if(data.results){
              $.each(data.results, function(idx, instance){
                var stopIdx = toStop.indexOf(instance.id);
                if(stopIdx>=0)
                  toStop.splice(stopIdx, 1);
              });
              if(toStop.length <=0){
                notifySuccess(null, $.i18n.prop('instance_stop_success',  DefaultEncoder().encodeForHTML(instances.toString())));
                thisObj.tableWrapper.eucatable('refreshTable');
              }else{
                notifyError($.i18n.prop('instance_stop_error',  DefaultEncoder().encodeForHTML(toStop.toString())), undefined_error);
              }
            }else
              notifyError($.i18n.prop('instance_stop_error',  DefaultEncoder().encodeForHTML(instances.toString())), undefined_error);
          },
          error: function(jqXHR, textStatus, errorThrown){
            notifyError($.i18n.prop('instance_stop_error',  DefaultEncoder().encodeForHTML(instances.toString())), getErrorMessage(jqXHR));
          }
        });
    },
    _startInstances : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 17);
//      $.each(instances, function(idx, instance){
//        instances[idx] = $(instance).html();   // After dataTable 1.9 integration, this operation is no longer needed. 030413
//      });
      var toStart = instances.slice(0);
      var instIds = '';
      for(i=0; i<instances.length; i++)
        instIds+= '&InstanceId.'+parseInt(i+1)+'='+(instances[i]);

      $.ajax({
        type:"POST",
        url:"/ec2?Action=StartInstances",
        data:"_xsrf="+$.cookie('_xsrf')+instIds,
        dataType:"json",
        async:true,
        success: function(data, textStatus, jqXHR){
          if(data.results){
            $.each(data.results, function(idx, instance){
              var startIdx = toStart.indexOf(instance.id);
              if(startIdx>=0)
                toStart.splice(startIdx, 1);
            });
            if(toStart.length <=0){
              notifySuccess(null, $.i18n.prop('instance_start_success',  DefaultEncoder().encodeForHTML(instances.toString())));
              thisObj.tableWrapper.eucatable('refreshTable');
            }else{
              notifyError($.i18n.prop('instance_start_error',  DefaultEncoder().encodeForHTML(toStart.toString())), undefined_error);
            }
          }else {
            notifyError($.i18n.prop('instance_start_error',  DefaultEncoder().encodeForHTML(instances.toString())), undefined_error);
          }
        },
        error: function(jqXHR, textStatus, errorThrown){
          notifyError($.i18n.prop('instance_start_error',  DefaultEncoder().encodeForHTML(instances.toString())), getErrorMessage(jqXHR));
        }
      });
    },
    _connectAction : function(){
      var thisObj = this;
      var instances = thisObj.tableWrapper.eucatable('getSelectedRows', 17);
      var oss = thisObj.tableWrapper.eucatable('getSelectedRows', 1);
      var keyname = thisObj.tableWrapper.eucatable('getSelectedRows', 8);
      var ip = thisObj.tableWrapper.eucatable('getSelectedRows', 6);
      var group = thisObj.tableWrapper.eucatable('getSelectedRows', 9);
      

      if ( instances.length > 0 ) {
        // connect is for one instance 
        var instance = instances[0];
//        instance = $(instance).html();   // After dataTable 1.9 integration, this operation is no longer needed. 030413
        var os = oss[0]; 

        // XSS Note: Need to encode the input strings before rendered as HTML
        keyname = DefaultEncoder().encodeForHTML(keyname[0]);
        ip = DefaultEncoder().encodeForHTML(ip[0]);
        group = DefaultEncoder().encodeForHTML(group[0]);

        if(os === 'windows'){ 
          thisObj.connectDialog.eucadialog('addNote','instance-connect-text',$.i18n.prop('instance_dialog_connect_windows_text', group, keyname));
          thisObj.connectDialog.eucadialog('addNote','instance-connect-uname-password', 
            ' <table> <thead> <tr> <th> <span>'+instance_dialog_connect_username+'</span> </th> <th> <span>'+instance_dialog_connect_password+'</span> </th> </tr> </thead> <tbody> <tr> <td> <span>'+ip+'\\Administrator </span></td> <td> <span> <a id="password-link" href="#">'+$.i18n.prop('instance_dialog_connect_getpassword', keyname)+'</a></span></td> </tr> </tbody> </table>');
          if (!thisObj.instPassword[instance]){
            var $fileSelector = thisObj.connectDialog.find('input#fileupload');
            $fileSelector.fileupload( {
              dataType: 'json',
              url: "../ec2",
              formData: [{name: 'Action', value: 'GetPassword'}, {name:'InstanceId', value:instance}, {name:'_xsrf', value:$.cookie('_xsrf')}],  
              done: function (e, data) {
                $.each(data.result, function (index, result) {
                  thisObj.instPassword[result.instance] = result.password;
                  var parent = thisObj.connectDialog.find('a#password-link').parent();
                  parent.find('a').remove();
                  parent.html(DefaultEncoder().encodeForHTML(result.password));
                  thisObj.connectDialog.find('a').unbind('click');
                });
              },
              fail : function (e, data) {
                var parent = thisObj.connectDialog.find('a#password-link').parent();
   //             parent.html('<span class="on-error">'+instance_dialog_password_error+'</span>');
                parent.append($('<span>').addClass('on-error').text(instance_dialog_password_error));
		thisObj.connectDialog.find('a').unbind('click');
              },
            });
            thisObj.connectDialog.find('a').click( function(e) {
              $fileSelector.trigger('click'); 
            });
          }else {
            var parent = thisObj.connectDialog.find('a#password-link').parent();
            parent.find('a').remove();
            parent.html(DefaultEncoder().encodeForHTML(thisObj.instPassword[instance]));
            thisObj.connectDialog.find('a').unbind('click');
          }
        }
        else{
          thisObj.connectDialog.eucadialog('addNote','instance-connect-text',$.i18n.prop('instance_dialog_connect_linux_text', group, keyname, ip));
        }
        thisObj.connectDialog.eucadialog('open');
       }
    },
    _consoleAction : function() {
      var thisObj = this;
      var instance = thisObj.tableWrapper.eucatable('getSelectedRows', 17)[0];
      var display_id = thisObj.tableWrapper.eucatable('getSelectedRows', 18)[0];
      $.when( 
        $.ajax({
          type:"POST",
          url:"/ec2?Action=GetConsoleOutput",
          data:"_xsrf="+$.cookie('_xsrf')+"&InstanceId="+instance,
          dataType:"json",
          async:true,
        })).done(function(data){
          // Convert the value 'instance' to its display_id 
          if( display_id != null ){
            instance = display_id;
          }
          if(data && data.results){
            var newTitle = $.i18n.prop('instance_dialog_console_title',  DefaultEncoder().encodeForHTML(instance));
            thisObj.consoleDialog.data('eucadialog').option('title', newTitle);
            thisObj.consoleDialog.find('#instance-console-output').children().detach();
            thisObj.consoleDialog.find('#instance-console-output').append(
              $('<textarea>').attr('id', 'instance-console-output-text').addClass('console-output').text(data.results.output));
            thisObj.consoleDialog.eucadialog('open');
          }else{
            notifyError($.i18n.prop('instance_console_error',  DefaultEncoder().encodeForHTML(instance)), undefined_error);
          }
        }).fail(function(out){
          notifyError($.i18n.prop('instance_console_error',  DefaultEncoder().encodeForHTML(instance)), getErrorMessage(out));
        });
    },
    _attachAction : function() {
      var thisObj = this;
      var instanceToAttach = thisObj.tableWrapper.eucatable('getSelectedRows', 17)[0];
//      instanceToAttach=$(instanceToAttach).html();   // After dataTable 1.9 integration, this operation is no longer needed. 030413
      attachVolume(null, instanceToAttach);
    },

    _initDetachDialog : function(dfd) {  // should resolve dfd object
      var thisObj = this;
      var results = describe('volume');
      var instance = this.tableWrapper.eucatable('getSelectedRows', 17)[0];
 //     instance = $(instance).html();   // After dataTable 1.9 integration, this operation is no longer needed. 030413
      $msg = this.detachDialog.find('#volume-detach-msg');
      $msg.html($.i18n.prop('inst_volume_dialog_detach_text', DefaultEncoder().encodeForHTML(instance)));
      var $p = this.detachDialog.find('#volume-detach-select-all');
      $p.children().detach();
      $p.html('');
      var $selectAll = $('<a>').text(inst_volume_dialog_select_txt).attr('href', '#');
      $selectAll.click(function() {
        $.each(thisObj.detachDialog.find(":input[type='checkbox']"), function(idx, checkbox){
          $(checkbox).attr('checked', 'checked');
        });
        thisObj.detachDialog.eucadialog('enableButton',thisObj.detachButtonId);
      });
      $p.append(inst_volume_dialog_select_all_msg, '&nbsp;', $selectAll);
      volumes = [];
      $.each(results, function(idx, volume){
        if ( volume.attach_data && volume.attach_data['status'] ){
          var inst = volume.attach_data['instance_id'];
          var state = volume.attach_data['status'];
          if( state === 'attached' && inst === instance && !isRootVolume(inst, volume.id) ){
            volumes.push(volume.id);
          }
        }
      });
      var $table = this.detachDialog.find('#volume-detach-grid');
      $table.html('');
      COL_IN_ROW = 3;
      for (i=0; i<Math.ceil(volumes.length/COL_IN_ROW); i++) {
        $row = $('<tr>');
        for (j=0; j<COL_IN_ROW;j++) {
          inx = i*COL_IN_ROW + j;
          if (volumes.length > inx) {
            volId = volumes[inx];
	    volId = DefaultEncoder().encodeForHTML(volId);
            $cb = $('<input>').attr('type', 'checkbox').attr('value', volId);
            $row.append($('<td>').append($cb,'&nbsp;', volId));
            $cb.click( function() {
              if ( thisObj.detachDialog.find("input:checked").length > 0 )
                thisObj.detachDialog.eucadialog('enableButton',DefaultEncoder().encodeForHTML(thisObj.detachButtonId));
              else
                thisObj.detachDialog.eucadialog('disableButton',DefaultEncoder().encodeForHTML(thisObj.detachButtonId));
            });
          } else {
            $row.append($('<td>'));
          }
        }
        $table.append($row);
      }
      dfd.resolve();
    },

    _detachAction : function(){
      var instance = this.tableWrapper.eucatable('getSelectedRows', 17)[0];
//      instance = $(instance).html();   // After dataTable 1.9 integration, this operation is no longer needed. 030413
      $instId = this.detachDialog.find('#volume-detach-instance-id');
      $instId.val(instance);
      this.detachDialog.eucadialog('open');
    },

    _detachVolume : function (force) {
      var thisObj = this;
      var checkedVolumes = thisObj.detachDialog.find("input:checked"); 
      var selectedVolumes = [];
      $.each(checkedVolumes, function(idx, vol){ 
        selectedVolumes.push($(vol).val());
      }); 
      var done = 0;
      var all = selectedVolumes.length;
      var error = [];
      doMultiAjax(selectedVolumes, function(item, dfd){
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
              }else{
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
          })(volumeId),
        });
      });
    },
    _associateAction : function(){
      var thisObj = this;
      var instance = thisObj.tableWrapper.eucatable('getSelectedRows', 17)[0];
      associateIp(instance);
    },
    _disassociateAction : function(){
      var thisObj = this;
      var ips = thisObj.tableWrapper.eucatable('getSelectedRows', 16);
      var results = describe('eip');
      var addrs = [];
      _.each(ips, function(ip){
        for(i in results){
          if (results[i].public_ip === ip){
            addrs.push(results[i]);
          }
        }
      });
      if(addrs.length >= 1){
        disassociateIp(addrs);
      }
    },

    _tagResourceAction : function(){
      var thisObj = this;
      var instance = thisObj.tableWrapper.eucatable('getSelectedRows', 17);
      if ( instance.length > 0 ) {
        require(['app'], function(app) {
           app.dialog('edittags', app.data.instance.get(instance[0]));
        });
       }
    },

    _launchMoreAction : function(){
      this.launchMoreDialog.eucadialog('open');
    },

    _launchMore : function(){
      var thisObj = this;
      var id = this.tableWrapper.eucatable('getSelectedRows', 17)[0];
//      id = $(id).html();    // After dataTable 1.9 integration, this operaiton is no longer needed. 030413
      var filter = {};
      var result = describe('instance');
      var instance = null;
      for( i in result){
        var inst = result[i];
        if(inst.id === id){
          instance = inst;
          break;
        }
      }
      if (!instance)
        return;

      var emi = instance.image_id;
      var type = thisObj.launchMoreDialog.find('#summary-type-insttype').children().last().text();
      var zone = thisObj.launchMoreDialog.find('#summary-type-zone').children().last().text();
      var inst_num = thisObj.launchMoreDialog.find('input#launch-more-num-instance').val();
      var keyname = thisObj.launchMoreDialog.find('#summary-security-keypair').children().last().text();
      if (keyname==='None')
        keyname = null;
      var sgroup = thisObj.launchMoreDialog.find('#launch-more-sgroup-input');
      if (!sgroup || sgroup.length <= 0)
        sgroup = thisObj.launchMoreDialog.find('#summary-security-sg').children().last().text(); 
      else
        sgroup = sgroup.val();  

      $('html body').find(DOM_BINDING['hidden']).launcher('updateLaunchParam', 'emi', emi);
      $('html body').find(DOM_BINDING['hidden']).launcher('updateLaunchParam', 'type', type);
      $('html body').find(DOM_BINDING['hidden']).launcher('updateLaunchParam', 'zone', zone);
      $('html body').find(DOM_BINDING['hidden']).launcher('updateLaunchParam', 'number', inst_num);
      $('html body').find(DOM_BINDING['hidden']).launcher('updateLaunchParam', 'keypair', keyname);
      $('html body').find(DOM_BINDING['hidden']).launcher('updateLaunchParam', 'sgroup', sgroup);
     
      var deviceMap = [] 
      thisObj.launchMoreDialog.find('#launch-wizard-advanced-storage').find('table tbody tr').each(function(){
        var $selectedRow = $(this);
        var $cells = $selectedRow.find('td');
        var volume = $($cells[0]).find('select').val();
        var mapping = '/dev/'+$($cells[1]).find('input').val();
        var snapshot = $($cells[2]).find('select').val();
        var size = $($cells[3]).find('input').val(); 
        var delOnTerm = $($cells[4]).find('input').is(':checked') ? true : false;
        
        snapshot = (snapshot ==='none') ? null : snapshot; 
        if(volume ==='ebs'){
          var mapping = {
            'volume':'ebs',
            'dev':mapping,
            'snapshot':snapshot, 
            'size':size,
            'delOnTerm':delOnTerm
          };
          deviceMap.push(mapping);
      $('html body').find(DOM_BINDING['hidden']).launcher('launch');
        }else if(volume.indexOf('ephemeral')>=0){
          var mapping = {
            'volume':volume,
            'dev':mapping
          }
          deviceMap.push(mapping);
        }else if(snapshot !== 'none') { // root volume
          var mapping = {
            'volume':'ebs',
            'dev': mapping,
            'snapshot':snapshot,
            'size':size,
            'delOnTerm':delOnTerm
          }
          deviceMap.push(mapping);
        }
      });
      if(deviceMap.length > 0)
        $('html body').find(DOM_BINDING['hidden']).launcher('updateLaunchParam', 'device_map', deviceMap);
      $('html body').find(DOM_BINDING['hidden']).launcher('launch');
    },
    _initLaunchMoreDialog : function(){
      var thisObj = this;
      var id = this.tableWrapper.eucatable('getSelectedRows', 17)[0];
//      id = $(id).html();       // After dataTable 1.9 integration, this operation is no longer needed.  030413 
      var filter = {};
      var result = describe('instance');
      var instance = null;
      for( i in result){
        var inst = result[i];
        if(inst.id === id){
          instance = inst;
          break;
        }
      }
      if (!instance)
        return;
      result = describe('image');
      var image = null;
      for (i in result){
        if(instance.image_id === result[i].id){
          image = result[i];
          break;
        }
      }
      if (!image)
        return;

      var $header = thisObj.launchMoreDialog.find('#launch-more-summary-header');
      $header.children().detach();
      $header.append($('<span>').text(instance.image_id));

      $header = thisObj.launchMoreDialog.find('#launch-wizard-advanced-header');
      $header.children().detach();
      $header.append($('<a>').attr('href', '#').text(launch_instance_section_header_advanced).click( function(e) {
        var $advSection = thisObj.launchMoreDialog.find('#launch-wizard-advanced-contents');
        $advSection.slideToggle('fast');
        $header.toggleClass('expanded');
      }));

      if($header.hasClass('expanded'))
        $header.find('a').trigger('click');

      var platform = image.platform ? image.platform : 'linux';
      var imgName = inferImage(image.location, image.description, platform);
      var $summary = $('<div>').append($('<div>').text(launch_instance_summary_platform), $('<span>').text(getImageName(imgName)));;
      var $image = thisObj.launchMoreDialog.find('#launch-more-summary-image');
      $image.removeClass().addClass('launch-more-summary-section').addClass(imgName);
      $image.children().detach();
      $image.append($summary.children());

      var selectedType = instance.instance_type;
      var zone = instance.placement;
      if (zone == undefined) {
        zone = oObj.aData._placement.zone;
      }
      $summary = $('<div>').append(
          $('<div>').attr('id','summary-type-insttype').append($('<div>').text(launch_instance_summary_type), $('<span>').text(selectedType)),
          $('<div>').attr('id','summary-type-zone').append($('<div>').text(launch_instance_summary_zone), $('<span>').text(zone)),
          $('<div>').attr('id','summary-type-numinst').addClass('form-row').addClass('clearfix').append(
            $('<label>').attr('for','launch-more-num-instance').text(launch_instance_summary_instances),
            $('<input>').attr('type','text').attr('id','launch-more-num-instance').val('1')));
      var $type = thisObj.launchMoreDialog.find('#launch-more-summary-type');
      $type.addClass(selectedType);
      $type.children().detach();
      $type.append($summary.children());
      $type.find('#launch-more-num-instance').focus();

      var selectedKp = instance.key_name ? instance.key_name : "None";
      var selectedSg = instance.group_name;
      var $sg = $('<div>').attr('id','summary-security-sg');
      if(selectedSg && selectedSg.length > 0)
        $sg.append($('<div>').text(launch_instance_summary_sg), $('<span>').text(selectedSg));
      else{
        var groupList = [];
        var result = describe('sgroup');
        for (i in result){
          groupList.push(result[i].name);
        }
        $sg.append($('<label>').attr('for','launch-more-sgroup-input').text(launch_instance_summary_sg), 
                   $('<select>').attr('id','launch-more-sgroup-input'));
        $sg.find('select').watermark(instance_dialog_launch_more_enter_sgroup);
        $.each(groupList, function(idx, group){
          $sg.find('select').append($('<option>').val(group).text(group));
        });
      }
      $summary = $('<div>').append(
                   $('<div>').attr('id','summary-security-keypair').append($('<div>').text(launch_instance_summary_keypair),$('<span>').text(selectedKp)),
                   $sg);
      var $security = thisObj.launchMoreDialog.find('#launch-more-summary-security');
      $security.children().detach();
      $security.append($summary.children());
      // summary area
      $('html body').find(DOM_BINDING['hidden']).children().detach();
      $('html body').find(DOM_BINDING['hidden']).launcher();

      var $advanced = thisObj.launchMoreDialog.find('#launch-wizard-advanced');
      $advanced.find('#launch-wizard-advanced-userdata').children().detach();
      $advanced.find('#launch-wizard-advanced-kernelramdisk').children().detach();
      $advanced.find('#launch-wizard-advanced-network').children().detach();
      $advanced.find('#launch-wizard-advanced-storage').children().detach();
      advHeader = $advanced.find('.wizard-section-label')[0];
      if (advHeader)
        $(advHeader).detach();
      $('html body').find(DOM_BINDING['hidden']).launcher('makeAdvancedSection', $advanced);
      $advanced.find('#launch-wizard-image-emi').val(image.id).trigger('change');
    },
    _expandCallback : function(row){ 
      var thisObj = this;
      var $el = $('<div />');
      require(['app', 'views/expandos/instance'], function(app, expando) {
         new expando({el: $el, model: app.data.instance.get(row[17]) });
      });
      return $el;
    },
/**** Public Methods ****/
    close: function() {
      $('html body').eucadata('removeCallback', 'instance','dashboard-summary');
      $('html body').eucadata('removeCallback', 'volume','dashboard-summary');
      $('html body').eucadata('removeCallback', 'eip','dashboard-summary');
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },

    launchInstance : function(){
      var $container = $('html body').find(DOM_BINDING['main']);
      $container.maincontainer("changeSelected", null, {selected:'launcher'});
    },
   
    glowNewInstance : function(inst_ids){
      var thisObj = this;
      for( i in inst_ids){
        thisObj.tableWrapper.eucatable('glowRow', inst_ids[i], 2); 
      }
    } 
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
