define([
   './eucadialogview',
   'text!./testdialog.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function() {
            this.$el.html(template);
            this._do_init();
        },
	});
});
