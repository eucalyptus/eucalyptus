/*************************************************************************
 * Copyright 2011-2012 Eucalyptus Systems, Inc.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

(function($, eucalyptus) {
  $.widget('eucalyptus.sgroup', $.eucalyptus.eucawidget, {
    options : { },
    tableWrapper : null,
    delDialog : null,
    $addDialog : null,
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #sgroupTblTmpl').clone();
      var $wrapper = $($tmpl.render($.i18n.map));
      this.element.add($wrapper);
      var $base_table = $wrapper.find('table');
      this.tableWrapper = $wrapper.eucatable({
        id : 'sgroups', // user of this widget should customize these options,
        base_table : $base_table,
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
          ]
        },
        header_title : sgroup_h_title,
        search_refresh : search_refresh,
        txt_create : sgroup_create,
        txt_found : sgroup_found,
        menu_text : table_menu_main_action,
        menu_actions : { delete: { name: table_menu_delete_action, callback: function(key, opt) { thisObj.deleteAction(thisObj); } } },
        row_click : function (args) { thisObj.handleRowClick(args); },
        menu_click_create : function (args) { thisObj.$addDialog.eucadialog('open')},
        context_menu : { value_column_inx: 4, build_callback: function (state) { return thisObj.buildContextMenu(state) } },
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
      $tmpl = $('html body').find('.templates #sgroupAddDlgTmpl').clone();
      $rendered = $($tmpl.render($.i18n.map));
      $add_dialog = $rendered.children().first();

      // add custom event handler to dialog elements
      // when calling eucadialog, the buttons should have domid to attach the specific domid that's used by event handler written here 
      $add_dialog.find('#sgroup-name').keypress( function(e){
        var $createButton = $('#'+createButtonId);
        if( e.which === RETURN_KEY_CODE || e.which === RETURN_MAC_KEY_CODE ) 
           $createButton.trigger('click');
        else if ( e.which === 0 ) {
        } else if ( e.which === BACKSPACE_KEY_CODE && $(this).val().length == 1 ) 
           $createButton.prop("disabled", true).addClass("ui-state-disabled");
        else if ( $(this).val().length == 0 )
           $createButton.prop("disabled", true).addClass("ui-state-disabled");
        else 
           $createButton.prop("disabled", false).removeClass("ui-state-disabled");
      });

      this.$addDialog = $add_dialog.eucadialog({
        id: 'sgroups-add',
        title: sgroup_dialog_add_title,
        buttons: { 
        // e.g., add : { domid: sgroup-add-btn, text: "Add new group", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
        'create': { domid: createButtonId, text: sgroup_dialog_create_btn, disabled: true,  click: function() { $add_dialog.dialog("close"); }},
        'cancel': {text: sgroup_dialog_cancel_btn, focus:true, click: function() { $add_dialog.dialog("close");}}
      }});
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    buildContextMenu : function(state) {
      //TODO: update it with more states
      return {
        "what?": { "name": volume_con_menu_attach },
        "how?": { "name": volume_con_menu_create_snapshot },
        "who?": { "name": volume_con_menu_delete }
      }
    },
/*
    handleInstanceHover : function(e) {
      switch(e.type) {
        case 'mouseleave':
          $(e.currentTarget).removeClass("hoverCell");
          break;
        case 'mouseenter':
          $(e.currentTarget).addClass("hoverCell");
          break;
      }
    },

    handleSnapshotHover : function(e) {
      switch(e.type) {
        case 'mouseleave':
          $(e.currentTarget).removeClass("hoverCell");
          $(e.currentTarget).off('click');
          break;
        case 'mouseenter':
          $(e.currentTarget).addClass("hoverCell");
          break;
      }
    },
*/
    reDrawTable : function() {
      this.tableWrapper.eucatable('reDrawTable');
    },

    handleRowClick : function(args) {
      if ( this.tableWrapper.eucatable('countSelectedRows') == 0 )
        this.tableWrapper.eucatable('deactivateMenu');
      else
        this.tableWrapper.eucatable('activateMenu');
    },

    _addSecurityGroup : function(groupName, groupDesc) {
      $.ajax({
        type:"GET",
        url:"/ec2?Action=CreateSecurityGroup",
        data:"_xsrf="+$.cookie('_xsrf') + "&GroupName=" + groupName + "&GroupDescription=" + groupDesc,
        dataType:"json",
        async:"false",
        success:
        function(data, textStatus, jqXHR){
          if (data.results && data.results.status == true) {
            successNotification(keypair_create_success + ' ' + keyName);
            tableWrapper.eucatable('refreshTable');
          } else {
            errorNotification(keypair_create_error + ' ' + keyName);
          }
        },
        error:
        function(jqXHR, textStatus, errorThrown){
          errorNotification(keypair_delete_error + ' ' + keyName);
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
                successNotification(sgroup_delete_success + ' ' + sgroupName);
                thisObj._getTableWrapper().eucatable('refreshTable');
              } else {
                errorNotification(sgroup_delete_error + ' ' + sgroupName);
              }
           }
          })(sgroupName),
          error:
          (function(sgroupName) {
            return function(jqXHR, textStatus, errorThrown){
              errorNotification(sgroup_delete_error + ' ' + sgroupName);
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

    deleteAction : function(thisObj) {
      $tableWrapper = thisObj._getTableWrapper();
      rowsToDelete = $tableWrapper.eucatable('getAllSelectedRows');
      if ( rowsToDelete.length > 0 ) {
        // show delete dialog box
        $deleteNames = this.delDialog.find("span.delete-names")
        $deleteNames.html('');
        for ( i = 0; i<rowsToDelete.length; i++ ) {
          t = escapeHTML(rowsToDelete[i]);
          $deleteNames.append(t).append("<br/>");
        }
        this.delDialog.dialog('open');
      }
    }

  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
