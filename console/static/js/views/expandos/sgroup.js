define([
  'app',
  'underscore',
  './eucaexpandoview',
  'text!./sgroup.html!strip',
], function(app, _, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      console.log("sgroup:"+this.model);
      console.log("sgroup:"+tmp.get('name'));
      this.model = new Backbone.Model();
      this.model.set('sgroup', tmp);
      this.scope = this.model;
      this._do_init();
    },
    remove : function() {
      this.model.destroy();
    }
  });
});
