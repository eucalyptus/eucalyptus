define([
 'app',
 'underscore',
 'backbone',
 './eucaexpandoview',
 'text!./instance.html!strip',
], function(app, _, Backbone, EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            this.template = template;
            var tmp = this.model ? this.model : new Backbone.Model();
            var id = tmp.get('id');
            this.model = new Backbone.Model();
            this.model.set('instance', tmp);
            this.model.set('volumes', app.data.volume.reduce(function(c, v) {
                          //return v.get('attach_data').instance_id = id ? c.add(v) : c;
                          return c.add(v);
                        }, new Backbone.Collection()));
            this.model.set('image', app.data.image.get(this.model.get('instance').get('image_id')));
            this.model.set('scaling', app.data.scalinginsts.get(id));
            this.model.set('instHealth', app.data.instHealths.get(id));
            this.scope = this.model;//_.extend(this.model, {});
            this._do_init();
        },
	});
});
