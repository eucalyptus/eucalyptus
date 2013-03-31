define([
   'underscore',
   './eucaexpandoview',
   'text!./instance.html!strip',
], function(_, EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            this.template = template;
            this.scope = _.extend(this.model, {
                button: {
                    click: function() { alert('ding dong'); }
                }
            });
            this._do_init();
        },
	});
});
