define([
  './eucadialogview',
  'text!./delete_snapshot_dialog.html!strip',
  'app',
  'backbone',
], function(EucaDialogView, template, App, Backbone) {
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
              // deregister images associated with snapshots first
              var images = [];
              doMultiAction(images, App.data.images,
                            function(model, options) {
                              model.deregister(options);
                            },
                            'snapshot_delete_image_progress', 'snapshot_delete_image_done', 'snapshot_delete_image_fail');
              doMultiAction(self.scope.items, App.data.snapshots,
                            function(model, options) {
                              options['wait'] = true;
                              model.destroy(options);
                            },
                            'snapshot_delete_progress', 'snapshot_delete_done', 'snapshot_delete_fail');
              self.close();
            }
         }
      }
      this._do_init();
    },
  });
});

