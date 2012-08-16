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
    delDialog : null,
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #instanceTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $instTable = $wrapper.children().first();
      var $instHelp = $wrapper.children().last();
      this.element.add($instTable);
      var $base_table = $instTable.find('table');

      tableWrapper = $instTable.eucatable({
        id : 'instances', // user of this widget should customize these options,
        base_table : $base_table,
        dt_arg : {
          "bProcessing": true,
          "sAjaxSource": "../ec2?Action=DescribeInstances",
          "sAjaxDataProp": "results",
          "bAutoWidth" : false,
          "sPaginationType": "full_numbers",
          "sDom": '<"table_instances_header"><"table-instance-filter">f<"clear"><"table_instances_top">rt<"table-instances-legend">p<"clear">',
          "aoColumns": [
            {
              "bSortable": false,
              "fnRender": function(oObj) { return '<input type="checkbox"/>' },
              "sWidth": "20px",
            },
            { "mDataProp": "platform" },
            { "mDataProp": "id" },
            { "mDataProp": "state" },
            { "mDataProp": "image_id" }, // TODO: this should be mapped to manifest 
            { "mDataProp": "placement" }, // TODO: placement==zone?
            { "mDataProp": "ip_address" },
            { "mDataProp": "private_ip_address" },
            { "mDataProp": "key_name" },
            { "mDataProp": "group_name" },
            // output creation time in browser format and timezone
            { "fnRender": function(oObj) { d = new Date(oObj.aData.launch_time); return d.toLocaleString(); } },
          ]
        },
        header_title : instance_h_title,
        search_refresh : search_refresh,
        txt_create : instance_create,
        txt_found : instance_found,
        menu_text : table_menu_main_action,
        menu_actions : { delete: [table_menu_delete_action, function (args) { thisObj.deleteAction(args) } ] },
        row_click : function (args) { thisObj.handleRowClick(args); },
        //context_menu : { value_column_inx: 8, build_callback: function (state) { return thisObj.buildContextMenu(state) } },
      //  td_hover_actions : { instance: [4, function (args) { thisObj.handleInstanceHover(args); }], snapshot: [5, function (args) { thisObj.handleSnapshotHover(args); }] }
        help_click : function(evt) {
          var $helpHeader = $('<div>').addClass('euca-table-header').append(
                              $('<span>').text(help_instance['landing_title']).append(
                                $('<div>').addClass('help-link').append(
                                  $('<a>').attr('href','#').html('&larr;'))));
          thisObj._flipToHelp(evt,$helpHeader, $instHelp);
        },
      });
      tableWrapper.appendTo(this.element);

      //add filter to the table
      $tableFilter = $('div.table-instance-filter');
      $tableFilter.addClass('euca-table-filter');
      $tableFilter.append(
        $('<span>').addClass("filter-label").html(table_filter_label),
        $('<select>').attr('id', 'instances-selector'));

      filterOptions = ['linux', 'windows'];
      $sel = $tableFilter.find("#instances-selector");
      for (o in filterOptions)
        $sel.append($('<option>').val(filterOptions[o]).text($.i18n.map['instance_selecter_' + filterOptions[o]]));

      $.fn.dataTableExt.afnFiltering.push(
	function( oSettings, aData, iDataIndex ) {
          // first check if this is called on a volumes table
          if (oSettings.sInstance != 'instances')
            return true;
          selectorValue = $("#instances-selector").val();
          switch (selectorValue) {
            case 'linux':
              //return attachedStates[aData[8]] == 1;
              break;
            case 'windows':
              //return detachedStates[aData[8]] == 1;
              break;
          }
          return true;
        }
      );

      // attach action
      $("#instances-selector").change( function() { thisObj.reDrawTable() } );

      // TODO: should be a template in html
      //add leged to the volumes table
/*
      $tableLegend = $("div.table-instances-legend");
      $tableLegend.append($('<span>').addClass('instance-legend').html(volume_legend));
      //TODO: this might not work in all browsers
      statuses = [].concat(Object.keys(attachedStates),Object.keys(detachedStates), otherStates);
      for (s in statuses)
        $tableLegend.append($('<span>').addClass('instance-status-legend').addClass('instance-status-' + statuses[s]).html($.i18n.map['instance_state_' + statuses[s]]));
*/
/*
      $tmpl = $('html body').find('.templates #volumeDelDlgTmpl').clone();
      var $rendered = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $del_dialog = $rendered.children().first();
      var $del_help = $rendered.children().last();
      this.delDialog = $del_dialog.eucadialog({
         id: 'volumes-delete',
         title: volume_dialog_del_title,
         buttons: {
           'delete': {text: volume_dialog_del_btn, click: function() { thisObj._deleteSelectedVolumes(); $del_dialog.dialog("close");}},
           'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $del_dialog.dialog("close");}} 
         },
         help: {title: help_volume['dialog_delete_title'], content: $del_help},
       });

      var createButtonId = 'volume-add-btn';
      $tmpl = $('html body').find('.templates #volumeAddDlgTmpl').clone();
      $add_dialog = $($tmpl.render($.i18n.map));
/*
      // add custom event handler to dialog elements
      // when calling eucadialog, the buttons should have domid to attach the specific domid that's used by event handler written here 
      $add_dialog.find('#key-name').keypress( function(e){
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
      $add_dialog.eucadialog({
        id: 'volumes-add',
        title: volume_dialog_add_title,
        buttons: { 
        // e.g., add : { domid: keys-add-btn, text: "Add new key", disabled: true, focus: true, click : function() { }, keypress : function() { }, ...} 
        'create': { domid: createButtonId, text: volume_dialog_create_btn, disabled: true,  click: function() { $add_dialog.dialog("close"); }},
        'cancel': {text: volume_dialog_cancel_btn, focus:true, click: function() { $add_dialog.dialog("close");}}
      }});
*/
    },
    _create : function() { 
    },

    _destroy : function() {
    },

    buildContextMenu : function(state) {
      //TODO: update it with more states
      switch (state) {
        case 'available':
          return {
            "attach": { "name": volume_con_menu_attach },
            "create_snapshot": { "name": volume_con_menu_create_snapshot },
            "delete": { "name": volume_con_menu_delete }
          }
        case 'attached':
          return {
            "detach": { "name": volume_con_menu_detach },
            "force_detach": { "name": volume_con_menu_force_detach },
            "create_snapshot": { "name": volume_con_menu_create_snapshot },
            "delete": { "name": "Delete" }
          }
        case 'attaching':
          return {
            "attach": { "name": volume_con_menu_attach },
            "detach": { "name": volume_con_menu_detach },
            "force_detach": { "name": volume_con_menu_force_detach },
            "create_snapshot": { "name": volume_con_menu_create_snapshot },
            "delete": { "name": volume_con_menu_delete }
          }
        default:
          return {
            "delete": { "name": volume_con_menu_delete }
          }
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
      tableWrapper.eucatable('reDrawTable');
    },

    handleRowClick : function(args) {
      count = tableWrapper.eucatable('countSelectedRows');
      if ( count == 0 )
        // disable menu
        tableWrapper.eucatable('deactivateMenu');
      else
        // enable delete menu
        tableWrapper.eucatable('activateMenu');
    },

/*
    _addKeyPair : function(keyName) {
      $.ajax({
        type:"GET",
        url:"/ec2?type=key&Action=CreateKeyPair",
        data:"_xsrf="+$.cookie('_xsrf') + "&KeyName=" + keyName,
        dataType:"json",
        async:"false",
        success:
        function(data, textStatus, jqXHR){
          if (data.results && data.results.material) {
            $.generateFile({
              filename    : keyName,
              content     : data.results.material,
              script      : '/support?Action=DownloadFile&_xsrf=' + $.cookie('_xsrf')
            });
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
*/
    _deleteSelectedVolumes : function () {
      var rowsToDelete = tableWrapper.eucatable('getAllSelectedRows');
      for ( i = 0; i<rowsToDelete.length; i++ ) {
        var volumeId = rowsToDelete[i];
        $.ajax({
          type:"GET",
          url:"/ec2?type=key&Action=DeleteVolume&VolumeId=" + volumeId,
          data:"_xsrf="+$.cookie('_xsrf'),
          dataType:"json",
          async:"true",
          success:
          (function(volumeId) {
            return function(data, textStatus, jqXHR){
              if ( data.results && data.results == true ) {
                successNotification(volume_delete_success + ' ' + volumeId);
                tableWrapper.eucatable('refreshTable');
              } else {
                errorNotification(volume_delete_error + ' ' + volumeId);
              }
           }
          })(volumeId),
          error:
          (function(volumeId) {
            return function(jqXHR, textStatus, errorThrown){
              errorNotification(volume_delete_error + ' ' + volumeId);
            }
          })(volumeId)
        });
      }
    },

    close: function() {
      this._super('close');
    },

    deleteAction : function(rowsToDelete) {
      //TODO: add hide menu

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
