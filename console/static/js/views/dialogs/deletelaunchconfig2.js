define([
   './eucadialogview',
   'text!./deletelaunchconfig2.html!strip',
], function(EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                status: '',
                items: args.items, 
                help: {title: null,
                       content: help_scaling.dialog_delete_content,
                       url: help_scaling.dialog_delete_content_url,
                       pop_height: 600},
 
                cancelButton: {
                    click: function() {
                       self.close();
                    }
                },
            }

            this._do_init();
        },
	});
});
