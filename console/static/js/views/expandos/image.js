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
