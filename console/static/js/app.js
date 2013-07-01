define(['dataholder', 'plugins'], function(dataholder, plugIns) {
    return {
        plugins: plugIns,
        data: dataholder,
        dialog: function(dialogname, scope) {
            require(['views/dialogs/' + dialogname], function(dialog) { 
                new dialog({model: scope}); 
            });
        },
        msg: function(keypath) {
          var value = $.i18n.prop(keypath);
          return value ? value : '';
        }
    }
});
