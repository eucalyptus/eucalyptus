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
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { "mDataProp": "name" },
            { "mDataProp": "description" },
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<a href="#">Show rules</a>' },
              "sWidth": "200px",
              "sClass": "table_center_cell",
            }
          ],
        },
        text : {
          header_title : sgroup_h_title,
          create_resource : sgroup_create,
          resource_found : sgroup_found,
          resource_search : sgroup_search,
        },
        menu_actions : function(){
          return{"edit": {"name": sgroup_action_edit, callback: function(key, opt) { thisObj._editAction();}},
                 "delete" : { "name": sgroup_action_delete, callback: function(key, opt) { thisObj._deleteAction();}}};
        },
        context_menu_actions : function(state) { 
          return{"edit": {"name": sgroup_action_edit, callback: function(key, opt) { thisObj._editAction();}},
                 "delete" : { "name": sgroup_action_delete, callback: function(key, opt) { thisObj._deleteAction();}}};
        },
        menu_click_create : function (args) { thisObj.rulesList=null; $('#sgroup-rules-list').html(''); thisObj.addDialog.eucadialog('open')},
        help_click : function(evt) {
          thisObj._flipToHelp(evt, $sgroupHelp);
        },
      });
      this.tableWrapper.appendTo(this.element);
    },

    _create : function() { 
      var thisObj = this;
      $("#sgroups-selector").change( function() { thisObj.reDrawTable() } );

      var $tmpl = $('html body').find('.templates #sgroupDelDlgTmpl').clone();
      $del_dialog = $($tmpl.render($.i18n.map));

      this.delDialog = $del_dialog.eucadialog({
         id: 'sgroups-delete',
         title: sgroup_dialog_del_title,
         buttons: {
           'delete': {text: sgroup_dialog_del_btn, click: function() { thisObj._deleteSelectedSecurityGroups(); $del_dialog.eucadialog("close");}},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $del_dialog.eucadialog("close");}} 
         }
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
        // e.g., add : { domid: sgroup-add-btn, text: "Add new group", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
        'create': { domid: createButtonId, text: sgroup_dialog_create_btn, disabled: true,  click: function() {
              var name = $.trim($add_dialog.find('#sgroup-name').val());
              var desc = $.trim($add_dialog.find('#sgroup-description').val());
              thisObj._storeRule(thisObj.addDialog);    // flush rule from form into array
              var fromPort = new Array();
              var toPort = new Array();
              var protocol = new Array();
              var cidr = new Array();
              var fromGroup = new Array();
              for (rule in thisObj.rulesList){
                  alert("adding rule for port "+thisObj.rulesList[rule].from_port+" new:"+thisObj.rulesList[rule].isnew);
                  if (thisObj.rulesList[rule].isnew == true) {
                      fromPort.push(thisObj.rulesList[rule].from_port);
                      toPort.push(thisObj.rulesList[rule].to_port);
                      protocol.push(thisObj.rulesList[rule].protocol);
                      cidr.push(thisObj.rulesList[rule].ipaddr);
                      fromGroup.push(thisObj.rulesList[rule].fromGroup);
                  }
              }
              $.ajax({
                  type:"GET",
                  url:"/ec2?Action=CreateSecurityGroup",
                  data:"_xsrf="+$.cookie('_xsrf') + "&GroupName=" + name + "&GroupDescription=" + desc,
                  dataType:"json",
                  async:"false",
                  success: function (data, textstatus, jqXHR) {
                      if (data.results && data.results.status == true) {
                          if (fromPort.length > 0) {
                              alert("adding rules to group");
                              thisObj._addIngressRule($add_dialog, name, fromPort, toPort, protocol, cidr, fromGroup);
                          }
                          else {
                              notifySuccess(null, sgroup_create_success(name));
                              thisObj._getTableWrapper().eucatable('refreshTable');
                              thisObj._getTableWrapper().eucatable('glowRow', name);
                              $add_dialog.eucadialog("close");
                          }
                      } else {
                          notifyFailure(sgroup_add_rule_error + ' ' + name);
                          $add_dialog.eucadialog("close");
                      }
                  },
                  error: function (jqXHR, textStatus, errorThrown) {
                    nofityError(null, sgroup_create_error(name));
                    dfd.reject();
                    $add_dialog.eucadialog("close");
                  }
              });
            }},
        'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $add_dialog.eucadialog("close");}},
        },
        help: {title: help_volume['dialog_add_title'], content: $add_help},
        user_val : function(index) {
                    alert("add: deleting rule "+index);
                    thisObj.rulesList.splice(index, 1);
                    thisObj._refreshRulesList(thisObj.addDialog);
        },
      });
      this.addDialog.eucadialog('onKeypress', 'sgroup-name', createButtonId, function () {
         thisObj._validateForm(createButtonId);
      });
      this.addDialog.eucadialog('onKeypress', 'sgroup-description', createButtonId, function () {
         thisObj._validateForm(createButtonId);
      });
      this.addDialog.eucadialog('onChange', 'sgroup-template', 'unused', function () {
         var thediv = $('#sgroup-more-rules');
         var sel = $('#sgroup-template');
         var templ = sel.val();
         if (templ == 'none') {
            thediv.css('display','none')
            $('#sgroup-ports').val('');
         }
         else {
            thediv.css('display','block')
            if (templ.indexOf('Custom', 0) == -1) {
                var idx = templ.indexOf('port', 0);
                var part = templ.substr(idx+5);
                $('#sgroup-ports').val(parseInt(part));
            }
            else
                $('#sgroup-ports').val('');
         }
      });
      this.addDialog.find('#sgroup-ip-check').click(function () {
        $.ajax({
            type: 'GET',
            url: '/checkip',
            contentType: 'text/plain; charset=utf-8',
            dataType: "text",
            success: function(data, textStatus, jqXHR) {
                         thisObj.addDialog.find('#allow-ip').val(jqXHR.responseText)
                     }
        });
      });
      this.addDialog.find('#sgroup-add-rule').click(function () {
        thisObj._storeRule(thisObj.addDialog);
        // now reset form
        $('#sgroup-template').val('none');
        $('#sgroup-ports').val('');
        $('#allow-ip').val('');
        $('#allow-group').val('');
        thisObj._refreshRulesList(thisObj.addDialog);
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
              thisObj._storeRule(thisObj.editDialog);    // flush rule from form into array
              // need to remove rules flagged for deletion, then add new ones to avoid conflicts
              var fromPort = new Array();
              var toPort = new Array();
              var protocol = new Array();
              var cidr = new Array();
              var fromGroup = new Array();
              for (rule in thisObj.rulesList){
                  if (thisObj.rulesList[rule].deletethis == true) {
                      fromPort.push(thisObj.rulesList[rule].from_port);
                      toPort.push(thisObj.rulesList[rule].to_port);
                      protocol.push(thisObj.rulesList[rule].protocol);
                      cidr.push(thisObj.rulesList[rule].ipaddr);
                      fromGroup.push(thisObj.rulesList[rule].fromGroup);
                  }
              }
              if (fromPort.length > 0) {
                  thisObj._removeIngressRule($edit_dialog, name, fromPort, toPort, protocol, cidr, fromGroup);
              }
              var fromPort = new Array();
              var toPort = new Array();
              var protocol = new Array();
              var cidr = new Array();
              var fromGroup = new Array();
              for (rule in thisObj.rulesList){
                  if (thisObj.rulesList[rule].isnew == true) {
                      fromPort.push(thisObj.rulesList[rule].from_port);
                      toPort.push(thisObj.rulesList[rule].to_port);
                      protocol.push(thisObj.rulesList[rule].protocol);
                      cidr.push(thisObj.rulesList[rule].ipaddr);
                      fromGroup.push(thisObj.rulesList[rule].fromGroup);
                  }
              }
              if (fromPort.length > 0) {
                  thisObj._addIngressRule($edit_dialog, name, fromPort, toPort, protocol, cidr, fromGroup);
              }
            }},
        'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $edit_dialog.eucadialog("close");}},
        },
        help: {title: help_volume['dialog_add_title'], content: $add_help},
        user_val : function(index) {
                    alert("edit: deleting rule "+index);
                    if (thisObj.rulesList[index].isnew) {
                        thisObj.rulesList.splice(index, 1);
                    }
                    else {
                        thisObj.rulesList[index].deletethis = true;
                    }
                    thisObj._refreshRulesList(thisObj.editDialog);
        },
      });
      this.editDialog.eucadialog('onChange', 'sgroup-template', 'unused', function () {
         var thediv = thisObj.editDialog.find('#sgroup-more-rules');
         var sel = thisObj.editDialog.find('#sgroup-template');
         var templ = sel.val();
         if (templ == 'none') {
            thediv.css('display','none')
            thisObj.editDialog.find('#sgroup-ports').val('');
         }
         else {
            thediv.css('display','block')
            if (templ.indexOf('Custom', 0) == -1) {
                var idx = templ.indexOf('port', 0);
                var part = templ.substr(idx+5);
                thisObj.editDialog.find('#sgroup-ports').val(parseInt(part));
            }
            else
                thisObj.editDialog.find('#sgroup-ports').val('');
         }
      });
      this.editDialog.find('#sgroup-add-rule').click(function () {
        thisObj._storeRule(thisObj.editDialog);
        // now reset form
        thisObj.editDialog.find('#sgroup-template').val('none');
        thisObj.editDialog.find('#sgroup-ports').val('');
        thisObj.editDialog.find('#allow-ip').val('');
        thisObj.editDialog.find('#allow-group').val('');
        thisObj._refreshRulesList(thisObj.editDialog);
      });
      this.editDialog.find('#sgroup-ip-check').click(function () {
        $.ajax({
            type: 'GET',
            url: '/checkip',
            contentType: 'text/plain; charset=utf-8',
            dataType: "text",
            success: function(data, textStatus, jqXHR) {
                         thisObj.editDialog.find('#allow-ip').val(jqXHR.responseText)
                     }
        });
      });
    },

    _destroy : function() {
    },

    _validateForm : function(createButtonId) {
       name = $.trim(this.addDialog.find('#sgroup-name').val());
       desc = $.trim(this.addDialog.find('#sgroup-description').val());
       $button = this.addDialog.parent().find('#' + createButtonId);
       if ( name.length > 0 && desc.length > 0 )     
         $button.prop("disabled", false).removeClass("ui-state-disabled");
       else
         $button.prop("disabled", false).addClass("ui-state-disabled");
    },

    // this function is used to take an ingress rule from the form and move it to the rulesList
    _storeRule : function(dialog) {
        if (this.rulesList == null) {
            this.rulesList = new Array();
        }
        // if nothing selected, don't save
        if (dialog.find('#sgroup-template').val() == 'none')
            return
        var rule = new Object();
        rule.protocol = 'tcp';
        var port_range = dialog.find('#sgroup-ports').val();
        var ports = port_range.split('-');
        rule.from_port = ports[0];
        rule.to_port = ports[ports.length-1];
        if (dialog.find("input[@name='allow-group']:checked").val() == 'ip') {
            rule.ipaddr = dialog.find('#allow-ip').val();
        }
        else if (dialog.find("input[@name='allow-group']:checked").val() == 'group') {
            rule.group = dialog.find('#allow-group').val();
        }
        rule.isnew = true;
        this.rulesList.push(rule);
    },

    // this function populates the div where rules are listed based on the rulesList
    _refreshRulesList : function(dialog) {
        if (this.rulesList != null) {
            var msg = "";
            var i=0;
            for (rule in this.rulesList) {
                var ports = this.rulesList[rule].from_port;
                if (this.rulesList[rule].from_port != this.rulesList[rule].to_port) {
                    ports += "-"+this.rulesList[rule].to_port;
                }
                msg += "<a href='#' id='sgroup-rule-number-"+i+"'>Delete</a> Rule: "+this.rulesList[rule].protocol+
                            " ("+ ports+"), "+
                            this.rulesList[rule].ipaddr+"<br/>";
                i += 1;
            }
            dialog.find('#sgroup-rules-list').html(msg);
            i=0;
            for (rule in this.rulesList) {
                dialog.find('#sgroup-rule-number-'+i).on('click', {index: i, source: dialog}, function(event) {
                      event.data.source.dialog('option', 'user_val')(event.data.index);
                });
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
                rule.group = rules[i].grants[0].group_id;
            this.rulesList.push(rule);
        }
    },

    _getGroupName : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },

    _reDrawTable : function() {
      this.tableWrapper.eucatable('reDrawTable');
    },

    _deleteSelectedSecurityGroups : function () {
      var thisObj = this;
      var rowsToDelete = thisObj._getTableWrapper().eucatable('getSelectedRows', 1);
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var sgroupName = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?Action=DeleteSecurityGroup&GroupName=" + sgroupName,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"true",
          success:
          (function(sgroupName) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                notifySuccess(sgroup_delete_success + ' ' + sgroupName);
                thisObj._getTableWrapper().eucatable('refreshTable');
              } else {
                notifyFailure(sgroup_delete_error + ' ' + sgroupName);
              }
           }
          })(sgroupName),
          error:
          (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
              thisObj.delDialog.eucadialog('showError', sgroup_delete_error + ' ' + sgroupName);
            }
          })(sgroupName)
        });
      }
    },

    _addIngressRule : function(dialog, groupName, fromPort, toPort, protocol, cidr, fromGroup) {
      var thisObj = this;
      var req_params = "&GroupName=" + groupName;
      for (i=0; i<fromPort.length; i++) {
          req_params += "&IpPermissions."+(i+1)+".IpProtocol=" + protocol[i];
          req_params += "&IpPermissions."+(i+1)+".FromPort=" + fromPort[i];
          req_params += "&IpPermissions."+(i+1)+".ToPort=" + toPort[i];
          if (cidr[i])
              req_params += "&IpPermissions."+(i+1)+".IpRanges.1.CidrIp=" + cidr[i];
          if (fromGroup[i])
              req_params += "&IpPermissions."+(i+1)+".Groups.1.Groupname=" + fromGroup[i];
      }
      alert("req_params = "+req_params);
      $.ajax({
        type:"GET",
        url:"/ec2?Action=AuthorizeSecurityGroupIngress",
        data:"_xsrf="+$.cookie('_xsrf') + req_params,
        dataType:"json",
        async:"false",
        success: (function(sgroupName) {
            return function(data, textStatus, jqXHR){
                notifySuccess(sgroup_add_rule_success + ' ' + sgroupName);
                dialog.eucadialog("close");
            }
        }),
        error: (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
                notifySuccess(sgroup_add_rule_error + ' ' + sgroupName);
                dialog.eucadialog("close");
            }
        }),
      });
    },

    _removeIngressRule : function(dialog, groupName, fromPort, toPort, protocol, cidr, fromGroup) {
      var thisObj = this;
      var req_params = "&GroupName=" + groupName;
      for (i=0; i<fromPort.length; i++) {
          req_params += "&IpPermissions."+(i+1)+".IpProtocol=" + protocol[i];
          req_params += "&IpPermissions."+(i+1)+".FromPort=" + fromPort[i];
          req_params += "&IpPermissions."+(i+1)+".ToPort=" + toPort[i];
          if (cidr[i])
              req_params += "&IpPermissions."+(i+1)+".IpRanges.1.CidrIp=" + cidr[i];
          if (fromGroup[i])
              req_params += "&IpPermissions."+(i+1)+".Groups.1.Groupname=" + fromGroup[i];
      }
      $.ajax({
        type:"GET",
        url:"/ec2?Action=RevokeSecurityGroupIngress",
        data:"_xsrf="+$.cookie('_xsrf') + req_params,
        dataType:"json",
        async:"false",
        success: (function(sgroupName) {
            return function(data, textStatus, jqXHR){
                notifySuccess(sgroup_add_rule_success + ' ' + sgroupName);
                dialog.eucadialog("close");
            }
        }),
        error: (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
                notifySuccess(sgroup_add_rule_error + ' ' + sgroupName);
                dialog.eucadialog("close");
            }
        }),
      });
    },

    _getTableWrapper : function() {
      return this.tableWrapper;
    },

    _deleteAction : function() {
      var thisObj = this;
      var $tableWrapper = this._getTableWrapper();
      rowsToDelete = $tableWrapper.eucatable('getSelectedRows', 1);
      var matrix = [];
      $.each(rowsToDelete,function(idx, group){
        matrix.push([group]);
      });

      if ( rowsToDelete.length > 0 ) {
        thisObj.delDialog.eucadialog('setSelectedResources', {title:[sgroup_dialog_del_resource_title], contents: matrix});
        thisObj.delDialog.dialog('open');
      }
    },

    _editAction : function() {
      //TODO: add hide menu
      var thisObj = this;
      var $tableWrapper = this._getTableWrapper();
      rowsToEdit = $tableWrapper.eucatable('getSelectedRows');
      firstRow = rowsToEdit[0];
      thisObj._fillRulesList(firstRow);
      thisObj.editDialog.dialog('open');
      thisObj.editDialog.find('#sgroups-edit-group-name').html(firstRow.name+" "+sgroup_dialog_edit_description);
      thisObj.editDialog.find('#sgroups-edit-group-desc').html(firstRow.description);
      thisObj._refreshRulesList(thisObj.editDialog);
    },

/**** Public Methods ****/
    close: function() {
      this._super('close');
    },

    dialogAddGroup : function() {
      var thisObj = this;
      thisObj.rulesList=null; 
      $('#sgroup-rules-list').html(''); 
      thisObj.addDialog.eucadialog('open')
    },
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
