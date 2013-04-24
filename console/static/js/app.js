define(['dataholder'], function(dataholder) {
    return {
        data: dataholder,
        dialog: function(dialogname, scope) {
            require(['views/dialogs/' + dialogname], function(dialog) { 
                new dialog({model: scope}); 
            });
        },
        msg: function(keypath) {
          var value = window[keypath];
          return value ? value : '';
        }
    }
});
