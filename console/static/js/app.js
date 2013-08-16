define(['dataholder'], function(dataholder) {
    return {
        data: dataholder,
        dialog: function(dialogname, scope) {
            require(['views/dialogs/' + dialogname], function(dialog) { 
               var newDialog = new dialog({model: scope}); 
            });
        },
        msg: function(keypath) {
          var value = $.i18n.prop(keypath);
          return value ? value : '';
        },
        aws: { aws_login_enabled: false }
    }
});
