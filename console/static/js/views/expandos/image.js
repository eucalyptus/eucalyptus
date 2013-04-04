define([
   'underscore',
   'backbone',
   './eucaexpandoview',
   'text!./image.html!strip',
], function(_, Backbone, EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            this.template = template;
            this.model = this.model ? this.model : {};
            this.scope = this.model;
            this._do_init();
        },
	});
});
