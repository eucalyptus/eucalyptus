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
        dt_arg : {
          "sAjaxSource": "../ec2?Action=DescribeSecurityGroups",
          "fnServerData": function (sSource, aoData, fnCallback) {
                $.ajax( {
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": "_xsrf="+$.cookie('_xsrf'),
                    "success": fnCallback
                });

          },
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sClass": "checkbox-cell",
            },
            {
              "fnRender" : function(oObj) {
                 shortName = addEllipsis(oObj.aData.name, 75);
                 $a = $('<a>').attr('href','#').attr('title', oObj.aData.name).addClass('twist').text(shortName);
                 return $('<div>').append($a).html();
              },
              "iDataSort": 7,
            },
            { 
              "fnRender": function(oObj) { return oObj.aData.description == null ? "" : "<span title='"+oObj.aData.description+"'>"+addEllipsis(oObj.aData.description, 50)+"</span>" },
              "iDataSort": 6,
            },
            { // protocol to appear in search result
              "bVisible": false,
              "fnRender" : function(oObj){
                 var groupName = oObj.aData.name;
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
            { // port to appear in search result
              "bVisible": false,
              "fnRender" : function(oObj){
                 var groupName = oObj.aData.name;
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
               }
            },
            { // source to appear in search result
              "bVisible": false,
              "fnRender" : function(oObj){
                 var groupName = oObj.aData.name;
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
                         src += grant['cidr_ip'].replace('&#x2f;','/') + ';';
                       else if(grant['groupName'] && grant['owner_id'])
                         src += grant['owner_id'] + '/' + grant['groupName'] +';';
                     }
                   } 
                 });
                 return src;
               }
            },
            { "mDataProp": "description", "bVisible": false },
            { "mDataProp": "name", "bVisible": false },
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
      })();
      if(numSelected == 1){
        menuItems['edit'] = {"name":sgroup_action_edit, callback: function(key, opt) { thisObj._editAction(); }}
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

              thisObj._storeRule(thisObj.addDialog);    // flush rule from form into array
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
                              notifySuccess(null, $.i18n.prop('sgroup_create_success', name));
                              thisObj._addIngressRule($add_dialog, name, fromPort, toPort, protocol, cidr, fromGroup, fromUser);
                              thisObj._getTableWrapper().eucatable('refreshTable');
                          }
                          else {
                              notifySuccess(null, $.i18n.prop('sgroup_create_success', name));
                              thisObj._getTableWrapper().eucatable('refreshTable');
                              thisObj._getTableWrapper().eucatable('glowRow', name);
                          }
                      } else {
                          notifyError($.i18n.prop('sgroup_add_rule_error', name), getErrorMessage(jqXHR));
                      }
                  },
                  error: function (jqXHR, textStatus, errorThrown) {
                    notifyError($.i18n.prop('sgroup_create_error', name), getErrorMessage(jqXHR));
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

      var $tmpl = $('html body').find('.templates #sgroupEditDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_sgroup)));
      var $edit_dialog = $rendered.children().first();
      var $edit_help = $rendered.children().last();
      this.editDialog = $edit_dialog.eucadialog({
        id: 'sgroups-edit',
        title: sgroup_dialog_edit_title,
        buttons: { 
        'save': { domid: createButtonId, text: sgroup_dialog_save_btn, click: function() {
              thisObj._storeRule(thisObj.editDialog);    // flush rule from form into array
              // need to remove rules flagged for deletion, then add new ones to avoid conflicts
              $edit_dialog.eucadialog("close");
              var name = thisObj.editDialog.find('#sgroups-hidden-name').html();
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
         thisObj._validateFormAdd(createButtonId, thisDialog);
      });
      dialog.eucadialog('buttonOnKeyup', dialog.find('#sgroup-description'), createButtonId, function() {
         thisObj._validateFormAdd(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-template').change(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-ports').keyup(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#sgroup-type').change(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#allow-ip').keyup(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.find('#allow-group').keyup(function () {
         thisObj._validateForm(createButtonId, thisDialog);
      });
      dialog.eucadialog('onChange', 'sgroup-template', 'unused', function () {
         var thediv = dialog.find('#sgroup-more-rules');
         var sel = dialog.find('#sgroup-template');
         var templ = asText(sel.val());
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
        thisObj._storeRule(thisDialog);
        // now reset form
        thisDialog.find('#sgroup-template').val('none');
        thisDialog.find('#sgroup-ports').val('');
        thisDialog.find('#allow-ip').val('');
        thisDialog.find('#allow-group').val('');
        thisObj._refreshRulesList(thisDialog);
      });
    },

    _validateFormAdd : function(createButtonId, dialog) {
      var name = dialog.eucadialog("get_validate_value", "sgroup-name",
                                        SGROUP_NAME_PATTERN, alphanum_warning);
      var desc = dialog.eucadialog("getValue", "#sgroup-description");
      if (desc && desc.length>MAX_DESCRIPTION_LEN)
          dialog.eucadialog("showFieldError", "#sgroup-description", long_description);
      var $button = dialog.parent().find('#' + createButtonId);
      if ( name == null || desc == null || name.length == 0 || desc.length == 0 )     
        $button.prop("disabled", false).addClass("ui-state-disabled");
      else {
        $button.prop("disabled", false).removeClass("ui-state-disabled");
        this._validateForm(createButtonId, dialog);
      }
    },

    _validateForm : function(createButtonId, dialog) {
      var enable = true;
      var $button = dialog.parent().find('#' + createButtonId);

      var template = asText(dialog.find('#sgroup-template').val());
      var ports = asText(dialog.find('#sgroup-ports').val());
      var type = asText(dialog.find('#sgroup-type').val());
      var allow_ip = asText(dialog.find('#allow-ip').val());
      var allow_group = asText(dialog.find('#allow-group').val());
      dialog.find('#sgroup-ports-error').html("");
      if (template.indexOf('TCP') > -1 || template.indexOf('UDP') > -1) {
        if (ports == '') {
          enable = false;
        }
        else {
          var port_list = ports.split('-');
          if (/^\d{1,5}$/.test(port_list[0]) == false) {
            dialog.find('#sgroup-ports-error').html(sgroup_error_from_port);
            enable = false;
          }
          else if (ports.indexOf('-') > -1) {  // we should have 2 numbers
            if (ports.length == 1 || /^\d{1,5}$/.test(port_list[1]) == false) {
              dialog.find('#sgroup-ports-error').html(sgroup_error_to_port);
              enable = false;
            }
          }
        }
      }
      else if (template.indexOf('ICMP') > -1) {
        if (type == '')
          enable = false;
        else if (type != parseInt(type)) {
          enable = false;
        }
      }

      if (template != 'none') {
          if (dialog.find("input[name='allow-group']:checked").val() == 'ip') {
            if (allow_ip == "") {
              enable = false;
            }
            else if (/^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\/([0-9]|[1-3][0-9])$/.test(allow_ip))
              dialog.find('#allow-ip-error').html("");
            else {
              dialog.find('#allow-ip-error').html(sgroup_error_address_range);
              enable = false;
            }
          }
          else if (dialog.find("input[name='allow-group']:checked").val() == 'group') {
            if (allow_group == '')
              enable = false;
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
            var icmp_type = asText(dialog.find('#sgroup-type').val());
            rule.from_port = icmp_type;
            rule.to_port = icmp_type;
        }
        else { // gather port details
            var port_range = asText(dialog.find('#sgroup-ports').val());
            // if no port named, don't save
            if (port_range == '')
                return;
            var ports = port_range.split('-');
            rule.from_port = ports[0];
            rule.to_port = ports[ports.length-1];
        }
        if (dialog.find("input[name='allow-group']:checked").val() == 'ip') {
            rule.ipaddr = asText(dialog.find('#allow-ip').val());
        }
        else if (dialog.find("input[name='allow-group']:checked").val() == 'group') {
            rule.group = asText(dialog.find('#allow-group').val());
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
                msg += "<li><a href='#' id='sgroup-rule-number-"+i+"'>"+delete_label+"</a>"+rule_label+"&nbsp;"+this.rulesList[rule].protocol+
                            " ("+ ports+"), "

                if (this.rulesList[rule].group) {
                    if (this.rulesList[rule].user) {
                        msg += this.rulesList[rule].user+"/";
                    }
                    msg += this.rulesList[rule].group
                }
                else {
                    msg += this.rulesList[rule].ipaddr;
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
          data:"_xsrf="+$.cookie('_xsrf')+"&GroupName="+sgroupName,
          dataType:"json",
          async:"true",
          success: (function(sgroupName, refresh_table) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                ;
              } else {
                error.push({id:sgroupName, reason: undefined_reason});
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
      var req_params = "&GroupName=" + groupName;
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
                notifySuccess(null, $.i18n.prop('sgroup_add_rule_success', addEllipsis(sgroupName, 75)));
                thisObj._getTableWrapper().eucatable('refreshTable');
            }
        })(sgroupName),
        error: (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
                notifyError($.i18n.prop('sgroup_add_rule_error', addEllipsis(sgroupName, 75)), getErrorMessage(jqXHR));
            }
        })(sgroupName),
      });
    },

    _removeIngressRule : function(dialog, groupName, fromPort, toPort, protocol, cidr, fromGroup, fromUser) {
      var thisObj = this;
      var req_params = "&GroupName=" + groupName;
      for (i=0; i<fromPort.length; i++) {
          req_params += "&IpPermissions."+(i+1)+".IpProtocol=" + protocol[i];
          req_params += "&IpPermissions."+(i+1)+".FromPort=" + fromPort[i];
          req_params += "&IpPermissions."+(i+1)+".ToPort=" + toPort[i];
          if (cidr[i]) {
              var tmp = $("<div/>").html(cidr[i]).text();
              req_params += "&IpPermissions."+(i+1)+".IpRanges.1.CidrIp=" + tmp;
          }
          if (fromGroup[i]) {
              var tmp = $("<div/>").html(fromGroup[i]).text();
              req_params += "&IpPermissions."+(i+1)+".Groups.1.GroupName=" + tmp
          }
          if (fromUser[i]) {
              var tmp = $("<div/>").html(fromUser[i]).text();
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
                notifySuccess(null, $.i18n.prop('sgroup_revoke_rule_success', addEllipsis(sgroupName, 75)));
                thisObj._getTableWrapper().eucatable('refreshTable');
            }
        })(sgroupName),
        error: (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
                notifyError($.i18n.prop('sgroup_revoke_rule_error', addEllipsis(sgroupName, 75)), getErrorMessage(jqXHR));
            }
        })(sgroupName),
      });
    },

    _getTableWrapper : function() {
      return this.tableWrapper;
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
      $('#sgroup-rules-list').html('');
      thisObj.addDialog.find('#sgroup-description').val('');
      thisObj.addDialog.eucadialog('open');
      thisObj.addDialog.find('input[id=sgroup-name]').focus();
      thisObj.addDialog.find('input[id=sgroup-description]').focus();
      thisObj.addDialog.find('#sgroup-template').val('none');
      thisObj.addDialog.find('input[id=allow-ip]').prop('disabled', false);
      thisObj.addDialog.find('input[id=allow-group]').prop('disabled', true);
      thisObj.addDialog.find('input[id=sgroup-allow-ip]').prop('checked', 'yes');
      thisObj.addDialog.find('#sgroup-more-rules').css('display','none')
      thisObj.addDialog.find("#sgroup-name-error").html("");
      thisObj.addDialog.find("#sgroup-description-error").html("");

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
      thisObj.editDialog.find('#sgroups-edit-group-name').html($('<span>').attr('title', firstRow.name).text(addEllipsis(firstRow.name, 70)));
      thisObj.editDialog.find('#sgroups-hidden-name').html(firstRow.name);
      thisObj.editDialog.find('#sgroup-template').val('none');
      thisObj.editDialog.find('#sgroups-edit-group-desc').html($('<span>').attr('title', firstRow.description).html(addEllipsis(firstRow.description, 70)));
      thisObj.editDialog.find('input[id=allow-ip]').prop('disabled', false);
      thisObj.editDialog.find('input[id=allow-group]').prop('disabled', true);
      thisObj.editDialog.find('input[id=sgroup-allow-ip]').prop('checked', 'yes');
      thisObj.editDialog.find('#sgroup-more-rules').css('display','none')
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
      var thisObj = this;
      var groupName = row[7];
      var results = describe('sgroup');
      var group = null;
      for(i in results){
        if (results[i].name === groupName){
          group = results[i];
          break;
        }
      }
      if(!group || !group.rules || group.rules.length <= 0){
        return null;
      }
      var $wrapper = $('<div>').addClass('sgroup-table-expanded-group').addClass('clearfix').append(
          $('<div>').addClass('expanded-section-label').text(sgroup_table_expanded_title), 
          $('<div>').addClass('expanded-section-content').addClass('clearfix'));
      if(group.rules && group.rules.length > 0){
        var $list = $wrapper.find('div').last();
        $.each(group.rules, function (idx, rule){
          var protocol = rule['ip_protocol'];
          var port = rule['from_port'];
          if(rule['to_port'] !== rule['from_port'])
            port += '-'+rule['to_port']; 
          var type = '';
          if(protocol === 'icmp'){
            // TODO : define icmp type
            ;
          }
          var portOrType = type ? type: port;
          var portOrTypeTitle = type ? sgroup_table_expanded_type : sgroup_table_expanded_port;

          var src = [];
          var grants = rule['grants'];
          $.each(grants, function(idx, grant){
            if(grant.cidr_ip && grant.cidr_ip.length>0){
              src.push(grant.cidr_ip);
            }else if(grant.owner_id && grant.owner_id.length>0){
              if(group.owner_id === grant.owner_id)
                src.push(grant.groupName);
              else
                src.push(grant.owner_id+'/'+grant.groupName);
            }
          });
          src = src.join(', '); 
 
          $list.append(
            $('<div>').addClass('sgroup-expanded-rule').append(
              $('<div>').addClass('rule-label').text(sgroup_table_expanded_rule),
              $('<ul>').addClass('rule-expanded').addClass('clearfix').append(
                $('<li>').append( 
                  $('<div>').addClass('expanded-title').text(sgroup_table_expanded_protocol),
                  $('<div>').addClass('expanded-value').text(protocol)),
                $('<li>').append( 
                  $('<div>').addClass('expanded-title').text(portOrTypeTitle),
                  $('<div>').addClass('expanded-value').text(portOrType)),
                $('<li>').append( 
                  $('<div>').addClass('expanded-title').text(sgroup_table_expanded_source),
                  $('<div>').addClass('expanded-value').html(src).text()))));
        });
      }
      return $('<div>').append($wrapper);
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
      $('#sgroup-rules-list').html(''); 
      thisObj.addDialog.find('#sgroup-description').val('');
      if(callback)
        thisObj.addDialog.data('eucadialog').option('on_close', {callback: callback});
      thisObj.addDialog.eucadialog('open')
    },
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
