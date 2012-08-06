(function($, eucalyptus) {
  $.widget('eucalyptus.keypair', $.eucalyptus.eucawidget, {
    options : { },
    keytable : null,
    
    _init : function() {
         var $tmpl = $('html body').find('.templates #keypairTblTmpl').clone();
         var $wrapper = $($tmpl.render($.i18n.map));
         this.element.add($wrapper);
         
         var $base_table = $wrapper.find('table'); 
         $wrapper.eucatable({
         id : 'keys', // user of this widget should customize these options,
         base_table : $base_table,
         dt_arg : {
                "bProcessing": true,
                "sAjaxSource": "../ec2?type=key&Action=DescribeKeyPairs",
                "sAjaxDataProp": "results",
                "bAutoWidth" : false,
                "sPaginationType": "full_numbers",
                "sDom": '<"table_keys_header">f<"clear"><"table_keys_top">rtp<"clear">',
                "aoColumns": [
                  {
                    "bSortable": false,
                    "fnRender": function(oObj) { return '<input type="checkbox" onclick="updateActionMenu(\'keys\')"/>' },
                    "sWidth": "20px",
                  },
                  { "mDataProp": "name" },
                  { "mDataProp": "fingerprint", "bSortable": false }
                ],
                "fnDrawCallback": function( oSettings ) {
                $('#table_keys_count').html(oSettings.fnRecordsTotal());
                }
              },
         header_title : keypair_h_title, 
         search_refresh : search_refresh, 
         txt_create : keypair_create,
         txt_found : keypair_found
      }).appendTo(this.element);
    },

    _create : function() { 
    },

    _destroy : function() {
    },

    close: function() {
      this._super('close');
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
