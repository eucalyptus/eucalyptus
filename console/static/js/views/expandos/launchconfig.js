define([
  'app',
  './eucaexpandoview',
  'text!./launchconfig.html!strip',
], function(app, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('config', tmp);
      this.model.set('image', app.data.image.get(this.model.get('config').get('image_id')));
      this.scope = this.model;
      this._do_init();
    },
  });
});
