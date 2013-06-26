define([
 'app',
 'underscore',
 'backbone',
 './eucaexpandoview',
 'text!./image.html!strip',
], function(app, _, Backbone, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      var self = this;
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('image', tmp);
      var mappings = [];
      var bdm = tmp.get('block_device_mapping');
      if (bdm) {
        if (bdm['/dev/sda']) {
          this.model.set('snapshot_id', bdm['/dev/sda'].snapshot_id);
        }
        for (var device in bdm) {
          var tmp = bdm[device];
          mappings.push({device:device, snapshot_id:tmp.snapshot_id, size:tmp.size, delete_on_terminate:tmp.delete_on_termination});
        }
      }
      this.model.set('bdm', mappings);
      this.model.set('kernel', app.data.images.get(this.model.get('image').get('kernel_id')));
      this.model.set('ramdisk', app.data.images.get(this.model.get('image').get('ramdisk_id')));
      this.scope = this.model;
      this._do_init();
    
      var tmptags = this.model.get('image').get('tags');
      tmptags.on('add remove reset sort sync', function() {
        self.render();
      });
    },
    remove : function() {
      this.model.destroy();
    }
  });
});
