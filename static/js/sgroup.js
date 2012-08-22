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
          "bProcessing": true,
          "sAjaxSource": "../ec2?Action=DescribeSecurityGroups",
          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "sPaginationType": "full_numbers",
          "sDom": '<"table_sgroups_header">f<"clear"><"table_sgroups_top">rt<"table-sgroups-legend">p<"clear">',
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
        menu_actions : function(){ return thisObj._buildActionsMenu()},
        context_menu : {build_callback : function(state) { return thisObj.buildContextMenu(state);}},
        menu_click_create : function (args) { thisObj.addDialog.eucadialog('open')},
        help_click : function(evt) {
          var $helpHeader = $('<div>').addClass('euca-table-header').append(
                              $('<span>').text(help_sgroup['landing_title']).append(
                                $('<div>').addClass('help-link').append(
                                  $('<a>').attr('href','#').html('&larr;'))));
          thisObj._flipToHelp(evt,$helpHeader, $sgroupHelp);
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
           'cancel': {text: sgroup_dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
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
              thisObj._addSecurityGroup(name, desc);
              $add_dialog.dialog("close");
            }},
        'cancel': {text: sgroup_dialog_cancel_btn, focus:true, click: function() { $add_dialog.dialog("close");}},
        },
        help: {title: help_volume['dialog_add_title'], content: $add_help},
      });
      this.addDialog.eucadialog('onKeypress', 'sgroup-name', createButtonId, function () {
         thisObj._validateForm(createButtonId);
      });
      this.addDialog.eucadialog('onKeypress', 'sgroup-description', createButtonId, function () {
         thisObj._validateForm(createButtonId);
      });
      this.addDialog.eucadialog('onChange', 'sgroup-template', 'morerools', function () {
         thediv = $.trim(this.addDialog.find('morerools'));
         alert("hey, look at me!");
         if (thediv.style.display == 'none')
             thediv.style.display = 'block'
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

    _whatsup : function() {
      var isValid = true;
      $notification = $add_dialog.find('div.dialog-notifications');
      if ( size == parseInt(size) ) {
        if ( $snapshot.val() != '' && parseInt($snapshot.attr('title')) > parseInt(size) ) {
          isValid = false;
          $notification.html(volume_dialog_snapshot_error_msg);
        }
      } else {
        isValid = false; 
        $notification.html(volume_dialog_size_error_msg);
      }
      if ( az === '' ) {
        isValid = false;
        $notification.html($notification.html + "<br/>" + volume_dialog_size_error_msg);
      }
      if ( isValid ) {
        thisObj._createVolume(size, az, $snapshot.val());
        $add_dialog.dialog("close");
      } 
    },

    _getGroupName : function(rowSelector) {
      return $(rowSelector).find('td:eq(1)').text();
    },

    buildContextMenu : function(row) {
     // var thisObj = this; ==> this causes the problem..why?
      var thisObj = $('html body').find(DOM_BINDING['main']).data("sgroup");
      return {
          "edit": { "name": sgroup_action_edit, callback: function(key, opt) { thisObj.editAction(thisObj._getGroupName(opt.selector)); } },
          "delete": { "name": sgroup_action_delete, callback: function(key, opt) { thisObj.deleteAction(thisObj._getGroupName(opt.selector)); } },
          };
    },

    reDrawTable : function() {
      this.tableWrapper.eucatable('reDrawTable');
    },

    _addSecurityGroup : function(groupName, groupDesc) {
      thisObj = this;
      $.ajax({
        type:"GET",
        url:"/ec2?Action=CreateSecurityGroup",
        data:"_xsrf="+$.cookie('_xsrf') + "&GroupName=" + groupName + "&GroupDescription=" + groupDesc,
        dataType:"json",
        async:"false",
        success:
        function(data, textStatus, jqXHR){
          $notification = thisObj.addDialog.find('div.dialog-notifications');
          if (data.results && data.results.status == true) {
            notifySuccess(sgroup_create_success + ' ' + groupName);
            thisObj.tableWrapper.eucatable('refreshTable');
          } else {
            notifyFailure(sgroup_create_error + ' ' + groupName);
          }
        },
        error:
        function(jqXHR, textStatus, errorThrown){
          $notification(sgroup_delete_error + ' ' + groupName);
        }
      });
    },

    _deleteSelectedSecurityGroups : function () {
      thisObj = this;
      var rowsToDelete = thisObj._getTableWrapper().eucatable('getAllSelectedRows');
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
              $notification(sgroup_delete_error + ' ' + sgroupName);
            }
          })(sgroupName)
        });
      }
    },

    close: function() {
      this._super('close');
    },

    _getTableWrapper : function() {
      return this.tableWrapper;
    },

    _buildActionsMenu : function() {
      thisObj = this;
      itemsList = {};
      // add edit action
      itemsList['edit'] = { "name": sgroup_action_edit, callback: function(key, opt) { thisObj.editAction(); } }
      // add delete action
      itemsList['delete'] = { "name": sgroup_action_delete, callback: function(key, opt) { thisObj.deleteAction(); } }
      return itemsList;
    },

    deleteAction : function() {
      $tableWrapper = this._getTableWrapper();
      rowsToDelete = $tableWrapper.eucatable('getAllSelectedRows');
      if ( rowsToDelete.length > 0 ) {
        // show delete dialog box
        $deleteNames = this.delDialog.find("span.resource-ids")
        $deleteNames.html('');
        for ( i = 0; i<rowsToDelete.length; i++ ) {
          t = escapeHTML(rowsToDelete[i]);
          $deleteNames.append(t).append("<br/>");
        }
        this.delDialog.dialog('open');
      }
    },

    editAction : function(rowsToEdit) {
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
    }

  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
