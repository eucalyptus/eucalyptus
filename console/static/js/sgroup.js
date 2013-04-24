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
  $.widget('eucalyptus.sgroup', $.eucalyptus.eucawidget, {
    options : { },
    baseTable : null,
    tableWrapper : null,
    delDialog : null,
    addDialog : null,
    editDialog : null,
    rulesList : null,
    user_id : null,
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #sgroupTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_sgroup)));
      var $sgroupTable = $wrapper.children().first();
      var $sgroupHelp = $wrapper.children().last();
      this.baseTable = $sgroupTable;
      this.tableWrapper = $sgroupTable.eucatable({
        id : 'sgroups', // user of this widget should customize these options,
        data_deps: ['groups'],
        hidden: thisObj.options['hidden'],
        dt_arg : {
          "sAjaxSource": 'sgroup',
          "aoColumnDefs": [
            {
	      // Display the checkbox button in the main table
              "bSortable": false,
              "aTargets":[0],
              "mData": function(source) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell",
            },
            {
	      // Display the name of the security group in the main table
	      // Allow the name to be clickable
	      // Use CSS 'twist'
	      "aTargets":[1],
              "mRender" : function(data) {
                return eucatableDisplayColumnTypeTwist(data, data, 75);
              },
              "mData": "name",
              "iDataSort": 7,
            },
            { 
	      // Display the description of the security group in the main table
	      "aTargets":[2],
              "mRender": function(data) {
		 return eucatableDisplayColumnTypeText(data, data, 50);
	      },
              "mData": "description",
              "iDataSort": 6,
            },
            { 
	      // Invisible column for storing the protocol variable, used in search
              "bVisible": false,
              "aTargets":[3],
              "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
               },
              "mData": function(source){
                 var groupName = source.name;
                 var results = describe('sgroup');
                 var group = null;
                 for(i in results){
                   if (results[i].name === groupName){
                     group = results[i];
                     break;
                   }
                 }
                 if(!group || !group.rules || group.rules.length <= 0)
                   return '';
                 var protocol = '';
                 $.each(group.rules, function (idx, rule){
                   protocol += rule['ip_protocol']+';';
                   /*var port = rule['from_port'];
                   if(rule['to_port'] !== rule['from_port'])
                     port += '-'+rule['to_port']; 
                   var type = '';
                   if(protocol === 'icmp'){
                    // TODO : define icmp type
                       ;
                   }*/
                });
                return protocol;
              },
            },  
            { 
	      // Invisible Column for storing the port varible, used in search
              "bVisible": false,
              "aTargets":[4],
              "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
               },
              "mData": function(source){
                 var groupName = source.name;
                 var results = describe('sgroup');
                 var group = null;
                 for(i in results){
                   if (results[i].name === groupName){
                     group = results[i];
                     break;
                   }
                 }
                 if(!group || !group.rules || group.rules.length <= 0)
                   return '';
                 var port = '';
                 $.each(group.rules, function (idx, rule){
                   if(rule['from_port'])
                     port += rule['from_port']+';';
                   if(rule['to_port'])
                     port += rule['to_port']+';';
                 });
                 return port;
               },
            },
            { 
	      // Invisible Column for storing the source variable, used in search
              "bVisible": false,
              "aTargets":[5],
              "mRender": function(data) {
                return DefaultEncoder().encodeForHTML(data);
               },
              "mData" : function(source){
                 var groupName = source.name;
                 var results = describe('sgroup');
                 var group = null;
                 for(i in results){
                   if (results[i].name === groupName){
                     group = results[i];
                     break;
                   }
                 }
                 if(!group || !group.rules || group.rules.length <= 0)
                   return '';
                 var src = '';
                 $.each(group.rules, function (idx, rule){
                   if (rule.grants && rule.grants.length > 0){
                     for(i in rule.grants){
                       var grant = rule.grants[i];
                       if(grant['cidr_ip'])
                         src += grant['cidr_ip'] + ';';
                       else if(grant['groupName'] && grant['owner_id'])
                         src += grant['owner_id'] + '/' + grant['groupName'] +';';
                     }
                   } 
                 });
                 return src;
               },
            },
	    {
	      // Invisible column for storing the description variable, used for sorting 
	      "bVisible": false,
              "aTargets":[6],
               "mData": "description",
	    },
	    { 
	      // Invisible column for storing the name variable, used for sorting
	      "bVisible": false,
              "aTargets":[7],
               "mData": "name",
	    },
          ],
        },
        text : {
          header_title : sgroup_h_title,
          create_resource : sgroup_create,
          resource_found : 'sgroup_found',
          resource_search : sgroup_search,
          resource_plural : sgroup_plural,
        },
        menu_actions : function(){
          return thisObj._createMenuActions(); 
        },
        expand_callback : function(row){ // row = [col1, col2, ..., etc]
          return thisObj._expandCallback(row);
        },
        menu_click_create : function (args) { thisObj._createAction(); },
        help_click : function(evt) {
          thisObj._flipToHelp(evt, {content: $sgroupHelp, url: help_sgroup.landing_content_url});
        },
      });
      this.tableWrapper.appendTo(this.element);
      $('html body').eucadata('addCallback', 'sgroup', 'sgroup-landing', function() {
        thisObj.tableWrapper.eucatable('redraw');
      });
    },

    _createMenuActions : function() {
      var thisObj = this;
      if(!thisObj.tableWrapper)
        return [];
      var selectedRows = thisObj.tableWrapper.eucatable('getSelectedRows');  
      var numSelected = selectedRows.length;
      var menuItems = {};
      (function(){
         menuItems['edit'] = {"name":sgroup_action_edit, callback: function(key, opt) { ; }, disabled: function(){ return true; }};
         menuItems['delete'] = {"name":sgroup_action_delete, callback: function(key, opt) { thisObj._deleteAction(); }};
         menuItems['tag'] = {"name":'Tag Resource', callback: function(key, opt) { thisObj._tagResourceAction(); }};
      })();
      if(numSelected == 1){
        menuItems['edit'] = {"name":sgroup_action_edit, callback: function(key, opt) { thisObj._editAction(); }}
        menuItems['tag'] = {"name":'Tag Resource', callback: function(key, opt) { thisObj._tagResourceAction(); }}
      }
      return menuItems;
    },

    _create : function() {
      var thisObj = this;
      $("#sgroups-selector").change( function() { thisObj.reDrawTable() } );

      var $tmpl = $('html body').find('.templates #sgroupDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_sgroup)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();

      this.delDialog = $del_dialog.eucadialog({
         id: 'sgroups-delete',
         title: sgroup_dialog_del_title,
         buttons: {
           'delete': {text: sgroup_dialog_del_btn, click: function() {
             var groupsToDelete = thisObj.delDialog.eucadialog('getSelectedResources',1);
             $del_dialog.eucadialog("close");
             thisObj._deleteSelectedSecurityGroups(groupsToDelete);
           }},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $del_dialog.eucadialog("close");}} 
         },
         help: { content: $del_help, url: help_sgroup.dialog_delete_content_url },
       });

      var createButtonId = 'sgroup-add-btn';
      var $tmpl = $('html body').find('.templates #sgroupAddDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_sgroup)));
      var $add_dialog = $rendered.children().first();
      var $add_help = $rendered.children().last();

      this.addDialog = $add_dialog.eucadialog({
        id: 'sgroups-add',
        title: sgroup_dialog_add_title,
        buttons: { 
        'create': { domid: createButtonId, text: sgroup_dialog_create_btn, disabled: true,  click: function() {
              var name = $add_dialog.eucadialog("get_validate_value", "sgroup-name",
                                                SGROUP_NAME_PATTERN, alphanum_warning);
              if (name == null) return;
              var desc = toBase64($add_dialog.eucadialog("getValue", "#sgroup-description"));
              if (desc == null) return;

              if (thisObj._storeRule(thisObj.addDialog) == false) {    // flush rule from form into array
                return;
              }
              var fromPort = new Array();
              var toPort = new Array();
              var protocol = new Array();
              var cidr = new Array();
              var fromGroup = new Array();
              var fromUser = new Array();
              for (rule in thisObj.rulesList){
                  if (thisObj.rulesList[rule].isnew == true) {
                      fromPort.push(thisObj.rulesList[rule].from_port);
                      toPort.push(thisObj.rulesList[rule].to_port);
                      protocol.push(thisObj.rulesList[rule].protocol);
                      if (thisObj.rulesList[rule].group) {
                        fromGroup.push(thisObj.rulesList[rule].group);
                        if (thisObj.rulesList[rule].user) {
                            fromUser.push(thisObj.rulesList[rule].user);
                        }
                        else {
                            fromUser.push(null);
                        }
                        cidr.push(null);
                      }
                      else {
                        cidr.push(thisObj.rulesList[rule].ipaddr);
                        fromGroup.push(null);
                        fromUser.push(null);
                      }
                  }
              }
              $add_dialog.eucadialog("close");
              $.ajax({
                  type:"POST",
                  url:"/ec2?Action=CreateSecurityGroup",
                  data:"_xsrf="+$.cookie('_xsrf') + "&GroupName=" + name + "&GroupDescription=" + desc,
                  dataType:"json",
                  async:"false",
                  success: function (data, textstatus, jqXHR) {
                      if (data.results && data.results.status == true) {
                          if (fromPort.length > 0) {
                              notifySuccess(null, $.i18n.prop('sgroup_create_success', DefaultEncoder().encodeForHTML(name)));
                              thisObj._addIngressRule($add_dialog, name, fromPort, toPort, protocol, cidr, fromGroup, fromUser);
                              thisObj._getTableWrapper().eucatable('refreshTable');
                              require(['app'], function(app) { app.data.sgroup.fetch(); });
                          }
                          else {
                              notifySuccess(null, $.i18n.prop('sgroup_create_success', DefaultEncoder().encodeForHTML(name)));
                              thisObj._getTableWrapper().eucatable('refreshTable');
                              thisObj._getTableWrapper().eucatable('glowRow', name);
                          }
                          // FIXME This is a hack to simulate the lifecycle of the new backbone dialogs
                          thisObj.addDialog.rscope.securityGroup.trigger('confirm');

                          var tmpSecGroup = thisObj.addDialog.rscope.securityGroup.clone();
                          tmpSecGroup.set('id', data.results.id);
                          tmpSecGroup.trigger('request');
                          tmpSecGroup.trigger('sync');
                      } else {
                          notifyError($.i18n.prop('sgroup_add_rule_error', DefaultEncoder().encodeForHTML(name)), getErrorMessage(jqXHR));
                      }
                  },
                  error: function (jqXHR, textStatus, errorThrown) {
                    notifyError($.i18n.prop('sgroup_create_error', DefaultEncoder().encodeForHTML(name)), getErrorMessage(jqXHR));
                  }
              });
            }},
        'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $add_dialog.eucadialog("close");}},
        },
        help: { content: $add_help, url: help_sgroup.dialog_add_content_url, pop_height: 600 },
        user_val : function(index) {
                    thisObj.rulesList.splice(index, 1);
                    thisObj._refreshRulesList(thisObj.addDialog);
        },
      });
      this._setupDialogFeatures(this.addDialog, createButtonId);

      require([
        'rivets', 
        'models/sgroup', 
        'text!views/dialogs/create_security_group_fixup.html!strip'
        ], function(rivets, SecurityGroup, TabbedDialogTmpl) {
            var $content = thisObj.addDialog.find('.content-sections-wrapper');
            $content.append($(TabbedDialogTmpl));
            $content.find('h3').remove();
            $content.css('background', 'none');
            $content.find('#tabs-1').append($content.find('.group.content-section'));
            $content.find('#tabs-2').append($content.find('.rules.content-section'));
            thisObj.addDialog.rivets = rivets;
            thisObj.addDialog.SecurityGroup = SecurityGroup;
            thisObj.addDialog.rscope = {
                securityGroup: new SecurityGroup()
            }
            thisObj.addDialog.rview = thisObj.addDialog.rivets.bind($content, thisObj.addDialog.rscope);
      });

      var $tmpl = $('html body').find('.templates #sgroupEditDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_sgroup)));
      var $edit_dialog = $rendered.children().first();
      var $edit_help = $rendered.children().last();
      this.editDialog = $edit_dialog.eucadialog({
        id: 'sgroups-edit',
        title: sgroup_dialog_edit_title,
        buttons: { 
        'save': { domid: createButtonId, text: sgroup_dialog_save_btn, click: function() {
              if (thisObj._storeRule(thisObj.editDialog) == false) {    // flush rule from form into array
                return;
              }
              // need to remove rules flagged for deletion, then add new ones to avoid conflicts
              $edit_dialog.eucadialog("close");
              var name = thisObj.editDialog.find('#sgroups-hidden-name').text();
              var fromPort = new Array();
              var toPort = new Array();
              var protocol = new Array();
              var cidr = new Array();
              var fromGroup = new Array();
              var fromUser = new Array();
              for (rule in thisObj.rulesList){
                  if (thisObj.rulesList[rule].deletethis == true) {
                      fromPort.push(thisObj.rulesList[rule].from_port);
                      toPort.push(thisObj.rulesList[rule].to_port);
                      protocol.push(thisObj.rulesList[rule].protocol);
                      if (thisObj.rulesList[rule].group) {
                        fromGroup.push(thisObj.rulesList[rule].group);
                        if (thisObj.rulesList[rule].user) {
                            fromUser.push(thisObj.rulesList[rule].user);
                        }
                        else {
                            fromUser.push(null);
                        }
                        cidr.push(null);
                      }
                      else {
                        cidr.push(thisObj.rulesList[rule].ipaddr);
                        fromGroup.push(null);
                        fromUser.push(null);
                      }
                  }
              }
              if (fromPort.length > 0) {
                  
                  thisObj._removeIngressRule($edit_dialog, name, fromPort, toPort, protocol, cidr, fromGroup, fromUser);
              }
              var fromPort = new Array();
              var toPort = new Array();
              var protocol = new Array();
              var cidr = new Array();
              var fromGroup = new Array();
              var fromUser = new Array();
              for (rule in thisObj.rulesList){
                  if (thisObj.rulesList[rule].isnew == true) {
                      fromPort.push(thisObj.rulesList[rule].from_port);
                      toPort.push(thisObj.rulesList[rule].to_port);
                      protocol.push(thisObj.rulesList[rule].protocol);
                      if (thisObj.rulesList[rule].group) {
                        fromGroup.push(thisObj.rulesList[rule].group);
                        if (thisObj.rulesList[rule].user) {
                            fromUser.push(thisObj.rulesList[rule].user);
                        }
                        else {
                            fromUser.push(null);
                        }
                        cidr.push(null);
                      }
                      else {
                        cidr.push(thisObj.rulesList[rule].ipaddr);
                        fromGroup.push(null);
                        fromUser.push(null);
                      }
                  }
              }
              if (fromPort.length > 0) {
                  thisObj._addIngressRule($edit_dialog, name, fromPort, toPort, protocol, cidr, fromGroup, fromUser);
              }
              // this handled in the _add and _remove functions
              //thisObj._getTableWrapper().eucatable('refreshTable');
            }},
        'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $edit_dialog.eucadialog("close");}},
        },
        help: { content: $edit_help , url: help_sgroup.dialog_edit_content_url, pop_height: 600},
        user_val : function(index) {
                    if (thisObj.rulesList[index].isnew) {
                        thisObj.rulesList.splice(index, 1);
                    }
                    else {
                        thisObj.rulesList[index].deletethis = true;
                    }
                    thisObj._refreshRulesList(thisObj.editDialog);
        },
      });
      this._setupDialogFeatures(this.editDialog, createButtonId);
    },

    _destroy : function() {
    },

    _setupDialogFeatures : function(dialog, createButtonId) {
      var thisDialog = dialog;
      var thisObj = this;
      var groupSelector = dialog.find('#allow-group');
      groupSelector.watermark(sgroup_group_name);
      dialog.eucadialog('buttonOnKeyup', dialog.find('#sgroup-name'), createButtonId, function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.eucadialog('buttonOnKeyup', dialog.find('#sgroup-description'), createButtonId, function() {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-template').change(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-ports').keyup(function () {
         thisObj._validateFormNoWarn(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-ports').change(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-type').change(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#allow-ip').keyup(function () {
         thisObj._validateFormNoWarn(createButtonId, thisDialog);
      });
      dialog.find('#allow-group').keyup(function () {
         thisObj._validateFormNoWarn(createButtonId, thisDialog);
      });
      dialog.find('#allow-ip').change(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#allow-group').change(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.eucadialog('onChange', 'sgroup-template', 'unused', function () {
         var thediv = dialog.find('#sgroup-more-rules');
         var sel = dialog.find('#sgroup-template');
         var templ = sel.val();
         if (templ == 'none') {
            thediv.css('display','none')
            thisDialog.find('#sgroup-ports').val('');
         }
         else {
            thediv.css('display','block')
            if (templ.indexOf('Custom', 0) == -1) {
                var idx = templ.indexOf('port', 0);
                var part = templ.substr(idx+5);
                thisDialog.find('#sgroup-ports').val(parseInt(part));
            }
            else
                thisDialog.find('#sgroup-ports').val('');
            if (templ.indexOf('TCP') > -1)
                thisObj._setPortOption(thisDialog);
            else {
                if (templ.indexOf('UDP') > -1)
                    thisObj._setPortOption(thisDialog);
                else {
                    if (templ.indexOf('ICMP') > -1) {
                        thisObj._setTypeOption(thisDialog);
                    }
                }
            }
         }
         thisObj._validateForm('sgroup-add-btn', thisDialog);
      });
      dialog.find('#sgroup-allow-ip').change(function () {
        thisDialog.find('#allow-ip').prop('disabled', false);
        thisDialog.find('#sgroup-ip-check').prop('disabled', false);
        thisDialog.find('#allow-group').prop('disabled', true);
        thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-allow-group').change(function () {
        thisDialog.find('#allow-ip').prop('disabled', true);
        thisDialog.find('#sgroup-ip-check').prop('disabled', true);
        thisDialog.find('#allow-group').prop('disabled', false);
        thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-ip-check').click(function () {
        $.ajax({
            type: 'GET',
            url: '/checkip',
            contentType: 'text/plain; charset=utf-8',
            dataType: "text",
            success: function(data, textStatus, jqXHR) {
                         thisDialog.find('#allow-ip').val(jqXHR.responseText+"/32");
                         thisObj._validateForm(createButtonId, thisDialog);
                     }
        });
      });
      dialog.find('#sgroup-add-rule').click(function () {
        if (thisObj._storeRule(thisDialog) == false) {
          return;
        }
        // now reset form
        thisDialog.find('#sgroup-template').val('none');
        thisDialog.find('#sgroup-ports').val('');
        thisDialog.find('#allow-ip').val('');
        thisDialog.find('#allow-group').val('');
        thisObj._refreshRulesList(thisDialog);
      });
    },

    _validateForm : function(createButtonId, dialog) {
        return this._validateFormInt(createButtonId, dialog, false);
    },

    _validateFormNoWarn : function(createButtonId, dialog) {
        return this._validateFormInt(createButtonId, dialog, true);
    },

    _validateFormInt : function(createButtonId, dialog, noWarn) {
      var enable = true;
      var valid = true;
      if (dialog == this.addDialog) {
        var name = dialog.eucadialog("get_validate_value", "sgroup-name",
                                          SGROUP_NAME_PATTERN, alphanum_warning);
        var desc = dialog.eucadialog("getValue", "#sgroup-description");
        if (desc && desc.length>MAX_DESCRIPTION_LEN)
            dialog.eucadialog("showFieldError", "#sgroup-description", long_description);
        var $button = dialog.parent().find('#' + createButtonId);
        if ( name == null || desc == null || name.length == 0 || desc.length == 0 ) {
          enable = false;
          valid = false;
        }
      }
      if (enable == true) {
        var $button = dialog.parent().find('#' + createButtonId);
        var template = dialog.find('#sgroup-template').val();
        var ports = dialog.find('#sgroup-ports').val();
        var type = dialog.find('#sgroup-type').val();
        var allow_ip = dialog.find('#allow-ip').val();
        var allow_group = dialog.find('#allow-group').val();
        dialog.find('#sgroup-ports-error').text("");
        if (template.indexOf('TCP') > -1 || template.indexOf('UDP') > -1) {
          if (ports == '') {
            enable = false;
            valid = false;
          }
          else {
            var port_list = ports.split('-');
            if (/^\d{1,5}$/.test(port_list[0]) == false) {
              if (noWarn == false) {
                dialog.find('#sgroup-ports-error').text(sgroup_error_from_port);
              }
              valid = false;
            }
            else if (ports.indexOf('-') > -1) {  // we should have 2 numbers
              if (ports.length == 1 || /^\d{1,5}$/.test(port_list[1]) == false) {
                if (noWarn == false) {
                  dialog.find('#sgroup-ports-error').text(sgroup_error_to_port);
                }
                valid = false;
              }
            }
          }
        }
        else if (template.indexOf('ICMP') > -1) {
          if (type == '') {
            enable = false;
            valid = false;
          }
          else if (type != parseInt(type)) {
            valid = false;
          }
        }

        if (template != 'none') {
            if (dialog.find("input[name='allow-radio']:checked").val() == 'ip') {
              if (allow_ip == "") {
                enable = false;
                valid = false;
              }
              else if (/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\/([0-9]|[1-3][0-9])$/.test(allow_ip))
                dialog.find('#allow-ip-error').text("");
              else {
                if (noWarn == false) {
                  dialog.find('#allow-ip-error').text(sgroup_error_address_range);
                }
                valid = false;
              }
            }
            else if (dialog.find("input[name='allow-radio']:checked").val() == 'group') {
              if (allow_group == '')
                valid = false;
            }
        }
      }

      if (enable == true) {
        $button.prop("disabled", false).removeClass("ui-state-disabled");
        dialog.find("#sgroup-add-rule").prop("disabled", false).removeClass("ui-state-disabled");
      }
      else {
        $button.prop("disabled", true).addClass("ui-state-disabled");
        dialog.find("#sgroup-add-rule").prop("disabled", true).addClass("ui-state-disabled");
      }
      return valid
    },

    _setPortOption : function(dialog) {
        dialog.find('#sgroup-port-option').css('display','block')
        dialog.find('#sgroup-type-option').css('display','none')
    },

    _setTypeOption : function(dialog) {
        dialog.find('#sgroup-port-option').css('display','none')
        dialog.find('#sgroup-type-option').css('display','block')
    },

    // this function is used to take an ingress rule from the form and move it to the rulesList
    _storeRule : function(dialog) {
        if (this._validateForm('sgroup-add-btn', dialog) == false) {
            return false;
        }

        if (this.rulesList == null) {
            this.rulesList = new Array();
        }
        // if nothing selected, don't save
        asText(template = dialog.find('#sgroup-template').val());
        if (template == 'none')
            return;
        var rule = new Object();
        if (template.indexOf('TCP') > -1)
            rule.protocol = 'tcp';
        else {
            if (template.indexOf('UDP') > -1)
                rule.protocol = 'udp';
            else {
                if (template.indexOf('ICMP') > -1)
                    rule.protocol = 'icmp';
            }
        }
        if (rule.protocol == 'icmp') {
            var icmp_type = dialog.find('#sgroup-type').val();
            rule.from_port = icmp_type;
            rule.to_port = icmp_type;
        }
        else { // gather port details
            var port_range = dialog.find('#sgroup-ports').val();
            // if no port named, don't save
            if (port_range == '')
                return;
            var ports = port_range.split('-');
            rule.from_port = ports[0];
            rule.to_port = ports[ports.length-1];
        }
        if (dialog.find("input[name='allow-radio']:checked").val() == 'ip') {
            rule.ipaddr = dialog.find('#allow-ip').val();
        }
        else if (dialog.find("input[name='allow-radio']:checked").val() == 'group') {
            rule.group = dialog.find('#allow-group').val();
            var user_group = rule.group.split('/');
            if (user_group.length > 1) {
                rule.user = user_group[0];
                rule.group = user_group[1];
            }
            else {  // user is current user
                rule.user = this.user_id
            }
        }
        rule.isnew = true;
        this.rulesList.push(rule);
        return true;
    },

    // this function populates the div where rules are listed based on the rulesList
    _refreshRulesList : function(dialog) {
        if (this.rulesList != null) {
            var msg = "<ul class='sg-rules-list'>";
            var i=0;
            for (rule in this.rulesList) {
                if (this.rulesList[rule].deletethis == true) continue;
                var ports = this.rulesList[rule].from_port;
                if (this.rulesList[rule].from_port != this.rulesList[rule].to_port) {
                    ports += "-"+this.rulesList[rule].to_port;
                }
                msg += "<li><a href='#' id='sgroup-rule-number-"+i+"'>"+DefaultEncoder().encodeForHTML(delete_label)+"</a>"+DefaultEncoder().encodeForHTML(rule_label)+"&nbsp;"+DefaultEncoder().encodeForHTML(this.rulesList[rule].protocol)+" ("+ DefaultEncoder().encodeForHTML(ports) +"), "

                if (this.rulesList[rule].group) {
                    if (this.rulesList[rule].user) {
                        msg += DefaultEncoder().encodeForHTML(this.rulesList[rule].user)+"/";
                    }
                    msg += DefaultEncoder().encodeForHTML(this.rulesList[rule].group);
                }
                else {
                    msg += DefaultEncoder().encodeForHTML(this.rulesList[rule].ipaddr);
                }
                msg += "</li>";
                i += 1;
            }
            msg += "</ul>";
            dialog.find('#sgroup-rules-list').html(msg);
            i=0;
            j=0;
            for (rule in this.rulesList) {
                if (this.rulesList[rule].deletethis == true) {
                    i += 1;
                    continue;
                }
                dialog.find('#sgroup-rule-number-'+j).on('click', {index: i, source: dialog}, function(event) {
                      event.data.source.dialog('option', 'user_val')(event.data.index);
                });
                j += 1;
                i += 1;
            }
        }
    },

    // this function takes rules returned from an API call and populates the rulesList
    _fillRulesList : function(groupRecord) {
        this.rulesList = new Array();
        rules = groupRecord.rules;
        for (i=0; i<rules.length; i++) {
            var rule = new Object();
            rule.protocol = rules[i].ip_protocol;
            rule.from_port = rules[i].from_port;
            rule.to_port = rules[i].to_port;
            if (rules[i].grants[0].cidr_ip != '')
                rule.ipaddr = rules[i].grants[0].cidr_ip;
            if (rules[i].grants[0].group_id != '')
                rule.user = rules[i].grants[0].owner_id;
                rule.group = rules[i].grants[0].groupName;
            this.rulesList.push(rule);
        }
    },

    _getGroupName : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },

    _reDrawTable : function() {
      this.tableWrapper.eucatable('reDrawTable');
    },

    _tagResourceAction : function(){
      var selected = this.tableWrapper.eucatable('getSelectedRows', 7);
      if ( selected.length > 0 ) {
        require(['app'], function(app) {
           app.dialog('edittags', app.data.sgroup.get(selected[0]));
        });
       }
    },

    _deleteSelectedSecurityGroups : function (groupsToDelete) {
      var thisObj = this;
      var done = 0;
      var all = groupsToDelete.length;
      var error = [];
      doMultiAjax(groupsToDelete, function(item, dfd){
        var sgroupName = item;
        $.ajax({
          type:"POST",
          url:"/ec2?Action=DeleteSecurityGroup",
          data:"_xsrf="+$.cookie('_xsrf')+"&GroupName="+encodeURIComponent(sgroupName),
          dataType:"json",
          async:"true",
          success: (function(sgroupName, refresh_table) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                ;
              } else {
                error.push({id:sgroupName, reason: undefined_error});
              }
            }
          })(sgroupName),
          error: (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
              error.push({id:sgroupName, reason:  getErrorMessage(jqXHR)});
            }
          })(sgroupName),
          complete: (function(sgroupName) {
            return function(jqXHR, textStatus){
              done++;
              if(done < all)
                notifyMulti(100*(done/all), $.i18n.prop('sgroup_delete_progress', all));
              else {
	        // XSS Node:: 'sgroup_delete_fail' would contain a chunk HTML code in the failure description string.
	     	// Message Example - Failed to send release request to Cloud for {0} IP address(es). <a href="#">Click here for details. </a>
	        // For this reason, the message string must be rendered as html()
                var $msg = $('<div>').addClass('multiop-summary').append(
                  $('<div>').addClass('multiop-summary-success').html($.i18n.prop('sgroup_delete_done', (all-error.length), all)));
                if (error.length > 0)
                  $msg.append($('<div>').addClass('multiop-summary-failure').html($.i18n.prop('sgroup_delete_fail', error.length)));
                notifyMulti(100, $msg.html(), error);
                thisObj._getTableWrapper().eucatable('refreshTable');
              }
              dfd.resolve();
            }
          })(sgroupName),
        });
      });
    },

    _addIngressRule : function(dialog, groupName, fromPort, toPort, protocol, cidr, fromGroup, fromUser) {
      var thisObj = this;
      var req_params = "&GroupName=" + encodeURIComponent(groupName);
      for (i=0; i<fromPort.length; i++) {
          req_params += "&IpPermissions."+(i+1)+".IpProtocol=" + protocol[i];
          req_params += "&IpPermissions."+(i+1)+".FromPort=" + fromPort[i];
          req_params += "&IpPermissions."+(i+1)+".ToPort=" + toPort[i];
          if (cidr[i])
              req_params += "&IpPermissions."+(i+1)+".IpRanges.1.CidrIp=" + cidr[i];
          if (fromGroup[i])
              req_params += "&IpPermissions."+(i+1)+".Groups.1.GroupName=" + fromGroup[i];
          if (fromUser[i])
              req_params += "&IpPermissions."+(i+1)+".Groups.1.UserId=" + fromUser[i];
      }
      var sgroupName = groupName;
      dialog.eucadialog("close");
      $.ajax({
        type:"POST",
        url:"/ec2?Action=AuthorizeSecurityGroupIngress",
        data:"_xsrf="+$.cookie('_xsrf') + req_params,
        dataType:"json",
        async:"true",
        success: (function(sgroupName) {
            return function(data, textStatus, jqXHR){
                notifySuccess(null, $.i18n.prop('sgroup_add_rule_success', DefaultEncoder().encodeForHTML(addEllipsis(sgroupName, 75))));
                thisObj._getTableWrapper().eucatable('refreshTable');
            }
        })(sgroupName),
        error: (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
                notifyError($.i18n.prop('sgroup_add_rule_error', DefaultEncoder().encodeForHTML(addEllipsis(sgroupName, 75))), getErrorMessage(jqXHR));
            }
        })(sgroupName),
      });
    },

    _removeIngressRule : function(dialog, groupName, fromPort, toPort, protocol, cidr, fromGroup, fromUser) {
      var thisObj = this;
      var req_params = "&GroupName=" + encodeURIComponent(groupName);
      for (i=0; i<fromPort.length; i++) {
          req_params += "&IpPermissions."+(i+1)+".IpProtocol=" + protocol[i];
          req_params += "&IpPermissions."+(i+1)+".FromPort=" + fromPort[i];
          req_params += "&IpPermissions."+(i+1)+".ToPort=" + toPort[i];
          if (cidr[i]) {
              var tmp = $("<div/>").text(cidr[i]).text();
              req_params += "&IpPermissions."+(i+1)+".IpRanges.1.CidrIp=" + tmp;
          }
          if (fromGroup[i]) {
              var tmp = $("<div/>").text(fromGroup[i]).text();
              req_params += "&IpPermissions."+(i+1)+".Groups.1.GroupName=" + tmp
          }
          if (fromUser[i]) {
              var tmp = $("<div/>").text(fromUser[i]).text();
              req_params += "&IpPermissions."+(i+1)+".Groups.1.UserId=" + tmp;
          }
      }
      var sgroupName = groupName;
      $.ajax({
        type:"POST",
        url:"/ec2?Action=RevokeSecurityGroupIngress",
        data:"_xsrf="+$.cookie('_xsrf') + req_params,
        dataType:"json",
        async:"true",
        success: (function(sgroupName) {
            return function(data, textStatus, jqXHR){
                notifySuccess(null, $.i18n.prop('sgroup_revoke_rule_success', DefaultEncoder().encodeForHTML(addEllipsis(sgroupName, 75))));
                thisObj._getTableWrapper().eucatable('refreshTable');
            }
        })(sgroupName),
        error: (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
                notifyError($.i18n.prop('sgroup_revoke_rule_error', DefaultEncoder().encodeForHTML(addEllipsis(sgroupName, 75))), getErrorMessage(jqXHR));
            }
        })(sgroupName),
      });
    },

    _getTableWrapper : function() {
      return this.tableWrapper;
    },

    _newCreateAction : function() {
      require(['app'], function(app) {
        app.dialog('create_security_group', {});
      });
    },

    _deleteAction : function() {
      var thisObj = this;
      var $tableWrapper = this._getTableWrapper();
      rowsToDelete = $tableWrapper.eucatable('getSelectedRows', 7);
      var matrix = [];
      $.each(rowsToDelete,function(idx, group){
        matrix.push([group, group]);
      });

      if ( rowsToDelete.length > 0 ) {
        thisObj.delDialog.eucadialog('setSelectedResources', {title:[sgroup_dialog_del_resource_title], contents: matrix, limit:60, hideColumn: 1});
        thisObj.delDialog.dialog('open');
      }
    },

    _createAction : function() {
      var thisObj = this;
      thisObj.rulesList=null;
      $('#sgroup-rules-list').text('');
      thisObj.addDialog.find('#sgroup-description').val('');
      thisObj.addDialog.eucadialog('open');
      thisObj.addDialog.find('input[id=sgroup-name]').focus();
      thisObj.addDialog.find('input[id=sgroup-description]').focus();
      thisObj.addDialog.find('#sgroup-template').val('none');
      thisObj.addDialog.find('input[id=allow-ip]').prop('disabled', false);
      thisObj.addDialog.find('input[id=allow-group]').prop('disabled', true);
      thisObj.addDialog.find('input[id=sgroup-allow-ip]').prop('checked', 'yes');
      thisObj.addDialog.find('#sgroup-more-rules').css('display','none')
      thisObj.addDialog.find("#sgroup-name-error").text("");
      thisObj.addDialog.find("#sgroup-description-error").text("");
      thisObj.addDialog.find('#sgroup-ports-error').text("");
      thisObj.addDialog.find('#allow-ip-error').text("");
      thisObj.addDialog.find('a[href="#tabs-1"]').click();

      thisObj.addDialog.rscope.securityGroup.get('tags').reset([]);
      thisObj.addDialog.rview.sync();

      gAddDialog = thisObj.addDialog;

      group_ids = [];
      var results = describe('sgroup');
      if ( results ) {
        for( res in results) {
          var group = results[res];
          if (group.name == "default") {
            this.user_id = group.owner_id;
          }
          group_ids.push(group.name);
        }
      }
      var groupSelector = thisObj.addDialog.find('#allow-group');
      var sorted = sortArray(group_ids);
      groupSelector.autocomplete({
        source: sorted,
        select: function() {
        }
      });
    },

    _editAction : function() {
      var thisObj = this;
      var $tableWrapper = this._getTableWrapper();
      rowsToEdit = $tableWrapper.eucatable('getSelectedRows');
      firstRow = rowsToEdit[0];
      thisObj._fillRulesList(firstRow);
      thisObj.editDialog.dialog('open');
      thisObj.editDialog.find('#sgroups-edit-group-name').text(addEllipsis(firstRow.name, 70));
      thisObj.editDialog.find('#sgroups-hidden-name').text(firstRow.name);
      thisObj.editDialog.find('#sgroup-template').val('none');
      thisObj.editDialog.find('#sgroups-edit-group-desc').text(addEllipsis(firstRow.description, 70));
      thisObj.editDialog.find('input[id=allow-ip]').prop('disabled', false);
      thisObj.editDialog.find('input[id=allow-group]').prop('disabled', true);
      thisObj.editDialog.find('input[id=sgroup-allow-ip]').prop('checked', 'yes');
      thisObj.editDialog.find('#sgroup-more-rules').css('display','none')
      thisObj.editDialog.find('#sgroup-ports-error').text("");
      thisObj.editDialog.find('#allow-ip-error').text("");
      thisObj._refreshRulesList(thisObj.editDialog);
      // set autocomplete based on list containing groups other than current group
      group_ids = [];
      var results = describe('sgroup');
      if ( results ) {
        for( res in results) {
          var group = results[res];
          if (group.name == "default") {
            this.user_id = group.owner_id;
          }
          if (group.name != firstRow.name) {
            group_ids.push(group.name);
          }
        }
      }
      var groupSelector = thisObj.editDialog.find('#allow-group');
      var sorted = sortArray(group_ids);
      var idx = sorted.indexOf(firstRow.name);
      groupSelector.autocomplete({
        source: sorted,
        select: function() {
        }
      });
    },


    _expandCallback : function(row){ 
      var $el = $('<div />');
      require(['app', 'views/expandos/sgroup'], function(app, expando) {
        new expando({el: $el, model: app.data.sgroup.get(row[7])});
      });
      return $el;
    },


/**** Public Methods ****/
    close: function() {
   //   this.tableWrapper.eucatable('close');
      cancelRepeat(tableRefreshCallback);
      this._super('close');
    },

    dialogAddGroup : function(callback) {
      var thisObj = this;
      thisObj.rulesList=null; 
      $('#sgroup-rules-list').text(''); 

      thisObj.addDialog.find('#sgroup-name').val('');
      thisObj.addDialog.find('#sgroup-description').val('');

      thisObj.addDialog.find('input[id=sgroup-name]').focus();
      thisObj.addDialog.find('input[id=sgroup-description]').focus();
      thisObj.addDialog.find('#sgroup-template').val('none');
      thisObj.addDialog.find('input[id=allow-ip]').prop('disabled', false);
      thisObj.addDialog.find('input[id=allow-group]').prop('disabled', true);
      thisObj.addDialog.find('input[id=sgroup-allow-ip]').prop('checked', 'yes');
      thisObj.addDialog.find('#sgroup-more-rules').css('display','none')
      thisObj.addDialog.find("#sgroup-name-error").text("");
      thisObj.addDialog.find("#sgroup-description-error").text("");
      thisObj.addDialog.find('#sgroup-ports-error').text("");
      thisObj.addDialog.find('#allow-ip-error').text("");
      thisObj.addDialog.find('a[href="#tabs-1"]').click();

      if (thisObj.addDialog.rscope && thisObj.addDialog.rscope.securityGroup != null) {
          thisObj.addDialog.rscope.securityGroup.get('tags').reset([]);
          thisObj.addDialog.rview.sync();
      }

      if(callback)
        thisObj.addDialog.data('eucadialog').option('on_close', {callback: callback});
      thisObj.addDialog.eucadialog('open')
    },
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
