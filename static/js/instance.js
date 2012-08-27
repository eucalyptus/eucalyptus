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
    emiToPlatform : {},
    // TODO: is _init() the right method to instantiate everything? 
    _init : function() {
      var thisObj = this;
      var $tmpl = $('html body').find('.templates #instanceTblTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_instance)));
      var $instTable = $wrapper.children().first();
      var $instHelp = $wrapper.children().last();
      this.element.add($instTable);
      $.when(
        thisObj._getEmi()
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
            // dimension: #selected, inst_state, vol_state, type            
 
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
    },

    _destroy : function() {
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

/**** Public Methods ****/
    close: function() {
      this._super('close');
    },
/**** End of Public Methods ****/
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
