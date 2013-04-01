define([
   './eucaexpandoview',
   'text!./snapshot.html!strip',
], function(EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            this.id = args && args.id ? args.id : undefined;
            this.template = template;
            this.scope = {
            }
            this._do_init();
        },
	});
});
