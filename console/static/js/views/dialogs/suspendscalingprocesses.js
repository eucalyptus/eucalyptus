define([
   './eucadialogview',
   'text!./deletescalinggroup.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        scope: {
        },
        initialize : function(args) {
            this.scope.items = args.items;
            this.$el.html(template);
            this._do_init();
        },
	});
});
