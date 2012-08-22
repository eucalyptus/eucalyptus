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
    emiToManifest : {},
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #instanceTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_volume)));
      var $instTable = $wrapper.children().first();
      var $instHelp = $wrapper.children().last();
      this.element.add($instTable);
      $.when(
        thisObj._getManifest()
      ).done(function(out){
          thisObj.tableWrapper = $instTable.eucatable({
          id : 'instances', // user of this widget should customize these options,
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
          text : {
            header_title : instance_h_title,
            create_resource : instance_create,
            resource_found : instance_found,
          },
          menu_actions : function(args){ return { delete: {name:table_menu_delete_action, callback: function (args) { thisObj._deleteAction(args) }}}},
          help_click : function(evt) {
            // TODO: make this a reusable operation
            var $helpHeader = $('<div>').addClass('euca-table-header').append(
                              $('<span>').text(help_instance['landing_title']).append(
                                $('<div>').addClass('help-link').append(
                                  $('<a>').attr('href','#').html('&larr;'))));
            thisObj._flipToHelp(evt,$helpHeader, $instHelp);
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

      //add filter to the table TODO: make templates
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
        });
      }); // end of done()
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
    },
    _create : function() { 
    },

    _destroy : function() {
    },
    
    
    _reDrawTable : function() {
      tableWrapper.eucatable('reDrawTable');
    },

    close: function() {
      this._super('close');
    },

    _deleteAction : function(rowsToDelete) {
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
    },
 
    _getManifest : function() {
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
            });
            } else {
                  ;//TODO: how to notify errors?
            }
        },
        error: function(jqXHR, textStatus, errorThrown){ //TODO: need to call notification subsystem
           ;//TODO: how to notify errors?
        }
      });
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
