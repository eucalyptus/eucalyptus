define([
   './eucaexpandoview',
   'text!./snapshot.html!strip',
], function(EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            this.template = template;
            this.model = this.model ? this.model : {};
            this.scope = _.extend(this.model, {});
            this._do_init();
        },
	});
});
