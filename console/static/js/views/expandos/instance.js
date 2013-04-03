define([
 'app',
 'underscore',
 './eucaexpandoview',
 'text!./instance.html!strip',
], function(app, _, EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            this.template = template;
            this.model = this.model ? this.model : {};
            var id = this.model.get('id');
            this.model.set('volumes', app.data.volume.reduce(function(c, v) {
                        //return v.get('attach_data').instance_id = id ? c.add(v) : c;
                        return c.add(v);
                      }, new Backbone.Collection()));
            this.scope = _.extend(this.model, {});
            this._do_init();
        },
	});
});
