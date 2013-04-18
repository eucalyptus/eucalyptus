define([
  './eucadialogview',
  'text!./delete_snapshot_dialog.html!strip',
  'text!./delete_snapshot_registered.html!strip',
  'models/snapshot',
  'app',
  'backbone',
], function(EucaDialogView, template, template2, Snapshot, App, Backbone) {
  return EucaDialogView.extend({

    findNameTag: function(model){
      var nameTag = null;
      model.get('tags').each(function(tag){
        if( tag.get('name').toLowerCase() == 'name' ){
          nameTag = tag.get('value');
        };
      });
      return nameTag;
    },

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

      // SNAPSHOT LIST TO CONTAIN DISPLAY_ID IF EXISTS --- Kyo 041713
      var snapshot_list = [];
      _.each(args.items, function(snapshot_id){
        var nameTag = self.findNameTag(App.data.snapshot.get(snapshot_id));
        console.log("Snapshot: " + snapshot_id + " Name Tag: " + nameTag);
        if( nameTag == null ){
          snapshot_list.push(snapshot_id);
        }else{
          snapshot_list.push(nameTag);
        }
      });

      this.scope = {
        status: '',
        snapshots: snapshot_list,
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
              $.each(args.items,function(idx, key){
                if (snapToImageMap[key] != undefined) {
                  $.each(snapToImageMap[key],function(idx, imageId){
                    images.push(imageId);
                  });
                }
              });

              doMultiAction(images, App.data.images,
                            function(model, options) {
                              model.destroy(options);
                            },
                            'snapshot_delete_image_progress', 'snapshot_delete_image_done', 'snapshot_delete_image_fail');

              doMultiAction(args.items, App.data.snapshots,
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

