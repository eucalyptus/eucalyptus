define([
  './eucadialogview',
  'text!./delete_volume_dialog.html!strip',
  'app',
], function(EucaDialogView, template, App) {
  return EucaDialogView.extend({
    initialize : function(args) {
      var self = this;
      this.template = template;

      this.scope = {
        status: '',
        items: args.items, 

        cancelButton: {
          click: function() {
            self.close();
          }
        },
        deleteButton: {
          click: function() {
              doMultiAction(self.scope.items, App.data.volumes,
                            function(model, options) {
                              options['wait'] = true;
                              model.destroy(options);
                            },
                            volume_delete_progress, volume_delete_done, volume_delete_fail);
              self.close();
          }
        },
      }
      this._do_init();
    },
  });
});
