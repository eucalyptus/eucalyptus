define([
  './eucadialogview',
  'text!./delete_snapshot_dialog.html!strip',
  'text!./delete_snapshot_registered.html!strip',
  'models/snapshot',
  'app',
  'backbone',
], function(EucaDialogView, template, template_registered, Snapshot, App, Backbone) {
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

      var images = [];
      var matrix = [];
      var image_hashmap = [];
      var deregisterImages = false;

      // SCAN THE IMAGE COLLECTION TO DISCOVER THE CONNECTION TO THE SNAPSHOTS
      App.data.images.each(function(img){
        _.each(img.get('block_device_mapping'), function(device){
          if( device.snapshot_id != null ){
            images.push(img.get('id'));
            image_hashmap[device.snapshot_id] = img.get('id');
            console.log("Image: " + img.get('id') + " Snapshot: " + device.snapshot_id);
          }
        });
      });

      // SNAPSHOT LIST TO CONTAIN DISPLAY_ID IF EXISTS --- Kyo 041713
      var snapshot_list = [];
      _.each(args.items, function(snapshot_id){
        var nameTag = self.findNameTag(App.data.snapshot.get(snapshot_id));
        console.log("Snapshot: " + snapshot_id + " Name Tag: " + nameTag);
        var snapshot_string = snapshot_id;
        if( nameTag != null ){
          snapshot_string = nameTag;
        }
        snapshot_list.push(snapshot_string);

        matrix.push([snapshot_string, image_hashmap[snapshot_id] != undefined ? 'Yes' : 'No']);
        if( image_hashmap[snapshot_id] != undefined ){
          deregisterImages = true;
        } 
      });

      // IF ANY SNAPSHOT IS REGISTERED, LOAD THE ALTERNATIVE VIEW 
      this.template = deregisterImages ? template_registered : template;

      this.scope = {
        status: '',
        snapshots: snapshot_list,
        matrix : matrix,
        images: images,

        cancelButton: {
          click: function() {
            self.close();
          }
        },
        deleteButton: {
          click: function() {

              // DEREGISTER IMAGE FIRST
              doMultiAction(images, App.data.images,
                            function(model, options) {
                              model.destroy(options);
                            },
                            'snapshot_delete_image_progress', 'snapshot_delete_image_done', 'snapshot_delete_image_fail');
              // DELETE SNAPSHOT
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

