define([
  'app',
  'underscore',
  './eucaexpandoview',
  'text!./volume.html!strip',
], function(app, _, EucaExpandoView, template) {
  return EucaExpandoView.extend({
    initialize : function(args) {
      var self = this;
      this.template = template;
      var tmp = this.model ? this.model : new Backbone.Model();
      this.model = new Backbone.Model();
      this.model.set('volume', tmp);
      this.model.set('snapshot', app.data.snapshots.get(this.model.get('volume').get('snapshot_id')));
      this.scope = this.model;
      this._do_init();

      var tmptags = this.model.get('volume').get('tags');
      tmptags.on('add remove reset sort sync', function() {
        self.render();
      });
},
    remove : function() {
      this.model.destroy();
    }
  });
});
