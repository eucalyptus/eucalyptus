define([
   './eucadialogview',
   'text!./quickscaledialog.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        scope: {
            qscale: new Backbone.Model({
                name: 'some name',
                size: 4,
                minimum: 1,
                maximum: 10,
                desired: 4,
            }),
        },
        initialize : function() {
            this.$el.html(template);
            this._do_init();
        },
	});
});
