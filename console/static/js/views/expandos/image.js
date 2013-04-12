define([
 'app',
 'underscore',
 'backbone',
 './eucaexpandoview',
 'text!./image.html!strip',
], function(app, _, Backbone, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('image', tmp);
      var bdm = tmp.get('block_device_mapping');
      if (bdm) {
        if (bdm['/dev/sda1']) {
          this.model.set('snapshot_id', bdm['/dev/sda1'].snapshot_id);
        }
      }
      this.model.set('kernel', app.data.images.get(this.model.get('image').get('kernel_id')));
      this.model.set('ramdisk', app.data.images.get(this.model.get('image').get('ramdisk_id')));
      this.scope = this.model;
      this._do_init();
    },
    remove : function() {
      this.model.destroy();
    }
  });
});
