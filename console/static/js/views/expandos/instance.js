define([
   'underscore',
   './eucaexpandoview',
   'text!./instance.html!strip',
], function(_, EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            this.template = template;
            this.model = this.model ? this.model : {};
            this.scope = _.extend(this.model, {
            });
            this._do_init();
        },
	});
});
