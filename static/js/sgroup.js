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
    rulesList : null,
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #sgroupTblTmpl').clone();
      var $wrapper = $($tmpl.render($.i18n.map));
      var $sgroupTable = $wrapper.children().first();
      var $sgroupHelp = $wrapper.children().last();
      this.baseTable = $sgroupTable;
      this.element.add($wrapper);
      this.tableWrapper = $wrapper.eucatable({
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
        },
        menu_actions : function(){
          return{"edit": {"name": sgroup_action_edit, callback: function(key, opt) { thisObj._editAction();}},
                 "delete" : { "name": sgroup_action_delete, callback: function(key, opt) { thisObj._deleteAction();}}};
        },
        context_menu : function(state) { 
          return{"edit": {"name": sgroup_action_edit, callback: function(key, opt) { thisObj._editAction();}},
                 "delete" : { "name": sgroup_action_delete, callback: function(key, opt) { thisObj._deleteAction();}}};
        },
        menu_click_create : function (args) { thisObj.rulesList=null; thisObj.addDialog.eucadialog('open')},
        help_click : function(evt) {
          thisObj._flipToHelp(evt, $sgroupHelp);
        },
      });
      this.tableWrapper.appendTo(this.element);

      // attach action
      $("#sgroups-selector").change( function() { thisObj.reDrawTable() } );

      $tmpl = $('html body').find('.templates #sgroupDelDlgTmpl').clone();
      $del_dialog = $($tmpl.render($.i18n.map));

      this.delDialog = $del_dialog.eucadialog({
         id: 'sgroups-delete',
         title: sgroup_dialog_del_title,
         buttons: {
           'delete': {text: sgroup_dialog_del_btn, click: function() { thisObj._deleteSelectedSecurityGroups(); $del_dialog.dialog("close");}},
           'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
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
              thisObj._storeRule();    // flush rule from form into array
              var actions = new Array();
              for (rule in thisObj.rulesList)
                  alert("adding rule for port: "+thisObj.rulesList[rule].port);
                  actions.push(function() {
                                thisObj._addIngressRule(name,
                                       thisObj.rulesList[rule].port,
                                       thisObj.rulesList[rule].port,
                                       thisObj.rulesList[rule].protocol,
                                       thisObj.rulesList[rule].ipaddr,
                                       thisObj.rulesList[rule].fromGroup
                                )
                              });
//              $(function() {
//                 $.when.apply($, actions).done(function() {
//                     alert("all done");
//                 });
//              });
              var dfd = $.Deferred();
              $.when(thisObj._addSecurityGroup(name, desc))
                  $.ajax({
                      type:"GET",
                      url:"/ec2?Action=CreateSecurityGroup",
                      data:"_xsrf="+$.cookie('_xsrf') + "&GroupName=" + name + "&GroupDescription=" + desc,
                      dataType:"json",
                      async:"false",
                      success: function (data, textstatus, jqXHR) {
                      },
                      error: function (jqXHR, textStatus, errorThrown) {
                        nofityError(null, error_creating_group_msg);
                        dfd.reject();
                      }
                  });
               .then(function(data) {
                         if (data.results && data.results.status == true) {
                             alert("num actions = "+actions.length);
                             $.when.apply($,actions).done(function() {
                                 notifySuccess(sgroup_create_success + ' ' + name);
                                 thisObj.tableWrapper.eucatable('refreshTable');
                             });
                         } else {
                             notifyError(sgroup_create_error + ' ' + name);
                         }
                     },
                     function(jqXHR, textStatus, errorThrown){
                         this.addDialog.eucadialog('showError',sgroup_delete_error + ' ' + name);
                     }
               );
              $add_dialog.dialog("close");
            }},
        'cancel': {text: dialog_cancel_btn, focus:true, click: function() { $add_dialog.dialog("close");}},
        },
        help: {title: help_volume['dialog_add_title'], content: $add_help},
      });
      this.addDialog.eucadialog('onKeypress', 'sgroup-name', createButtonId, function () {
         thisObj._validateForm(createButtonId);
      });
      this.addDialog.eucadialog('onKeypress', 'sgroup-description', createButtonId, function () {
         thisObj._validateForm(createButtonId);
      });
      this.addDialog.eucadialog('onChange', 'sgroup-template', 'unused', function () {
         var thediv = $('#sgroup-morerools');
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
            url: 'http://checkip.amazonaws.com/',
            contentType: 'text/plain; charset=utf-8',
            crossDomain: true,
            dataType: "text",
            success: function(data, textStatus, jqXHR) {
                        alert(jqXHR.responseText);
                         $('#allow-ip').val(jqXHR.responseText)
                     }
        });
      });
      this.addDialog.find('#sgroup-add-rule').click(function () {
        thisObj._storeRule();
        // now reset form
        $('#sgroup-template').val('none');
        $('#sgroup-ports').val('');
        $('#allow-ip').val('');
        $('#allow-group').val('');
        thisObj._refreshRulesList();
      });
    },

    _create : function() { 
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

    _storeRule : function() {
        if (this.rulesList == null) {
            this.rulesList = new Array();
        }
        // if nothing selected, don't save
        if ($('#sgroup-template').val() == 'none')
            return
        var rule = new Object();
        rule.protocol = 'tcp';
        rule.port = $('#sgroup-ports').val();
        if ($("input[@name='allow-group']:checked").val() == 'ip') {
            rule.ipaddr = $('#allow-ip').val();
        }
        else if ($("input[@name='allow-group']:checked").val() == 'group') {
            rule.group = $('#allow-group').val();
        }
        this.rulesList.push(rule);
    },

    _refreshRulesList : function() {
        if (this.rulesList != null) {
            var theDiv = $('#sgroup-rules-list')
            theDiv.html("loading...");
            var msg = "";
            for (rule in this.rulesList)
                msg += "<a href='#'>Delete</a> Rule: "+this.rulesList[rule].protocol+" ("+
                             this.rulesList[rule].port+"), "+
                             this.rulesList[rule].ipaddr+"<br/>";
            theDiv.html(msg);
        }
    },

    _getGroupName : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },

    _reDrawTable : function() {
      this.tableWrapper.eucatable('reDrawTable');
    },

    _addSecurityGroup : function(groupName, groupDesc) {
      var thisObj = this;
      return $.ajax({
        type:"GET",
        url:"/ec2?Action=CreateSecurityGroup",
        data:"_xsrf="+$.cookie('_xsrf') + "&GroupName=" + groupName + "&GroupDescription=" + groupDesc,
        dataType:"json",
        async:"false",
      });
    },

    _addIngressRule : function(groupName, fromPort, toPort, protocol, cidr, fromGroup) {
      var thisObj = this;
      var req_params = "&GroupName=" + groupName +
                       "&IpPermissions.1.IpProtocol=" + protocol +
                       "&IpPermissions.1.FromPort=" + fromPort +
                       "&IpPermissions.1.ToPort=" + toPort;
      if (fromGroup) {
        req_params = req_params + "&IpPermissions.1.Groups.1.GroupName=" + fromGroup;
      }
      if (cidr) {
        req_params = req_params + "&IpPermissions.1.IpRanges.1.CidrIp=" + cidr;
      }
      $.ajax({
        type:"GET",
        url:"/ec2?Action=AuthorizeSecurityGroupIngress",
        data:"_xsrf="+$.cookie('_xsrf') + req_params,
        dataType:"json",
        async:"false",
      });
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

    _editAction : function(rowsToEdit) {
      //TODO: add hide menu

      if ( rowsToEdit.length > 0 ) {
        // show edit dialog box
        /*
        $deleteNames = this.delDialog.find("span.delete-names")
        $deleteNames.html('');
        for ( i = 0; i<rowsToDelete.length; i++ ) {
          t = escapeHTML(rowsToDelete[i]);
          $deleteNames.append(t).append("<br/>");
        }
        this.delDialog.dialog('open');
        */
      }
    },

/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
