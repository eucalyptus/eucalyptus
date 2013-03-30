define([
   './eucaexpandoview',
   'text!./ipaddress.html!strip',
], function(EucaExpandoView, template) {
    return EucaExpandoView.extend({
        initialize : function(args) {
            console.log('init');
            this.id = args && args.id ? args.id : undefined;
            this.template = template;
            this.scope = {
                button: {
                    click: function() { alert('ding dong'); }
                }
            }
            this._do_init();
        },
	});
});
