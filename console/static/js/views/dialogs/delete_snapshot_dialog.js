define([
  './eucadialogview',
  'text!./delete_snapshot_dialog.html!strip',
  'text!./delete_snapshot_registered.html!strip',
  'app',
], function(EucaDialogView, template, template2, app) {
  return EucaDialogView.extend({
    initialize : function(args) {
      var self = this;
      var matrix = [];
      // this function in support.js uses eucadata, but it's debugged, so there.
      var snapToImageMap = generateSnapshotToImageMap();
      var deregisterImages = false;
      $.each(args.items,function(idx, key){
        matrix.push([key, snapToImageMap[key] != undefined ? 'Yes' : 'No']);
        if (snapToImageMap[key] != undefined)
          deregisterImages = true;
      });
      this.template = deregisterImages ? template2 : template;

      this.scope = {
        status: '',
        items: args.items, 
        matrix : matrix,

        cancelButton: {
          click: function() {
            self.close();
          }
        },
        deleteButton: {
          click: function() {
              // deregister images associated with snapshots first
              var images = [];
              // this function in support.js uses eucadata, but it's debugged, so there.
              var snapToImageMap = generateSnapshotToImageMap();
              $.each(self.scope.items,function(idx, key){
                if (snapToImageMap[key] != undefined) {
                  $.each(snapToImageMap[key],function(idx, imageId){
                    images.push(imageId);
                  });
                }
              });
              doMultiAction(images, app.data.images,
                            function(model, options) {
                              model.destroy(options);
                            },
                            'snapshot_delete_image_progress', 'snapshot_delete_image_done', 'snapshot_delete_image_fail');
              doMultiAction(self.scope.items, app.data.snapshots,
                            function(model, options) {
                              options['wait'] = true;
                              if (images.length > 0) {
                                // first delete will fail, following deregister.
                                model.destroy(null);
                              }
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

