define([
   './eucadialogview',
   'text!./create_security_group.html!strip',
   'app',
   'backbone',
   'models/sgroup'
], function(EucaDialogView, template, App, Backbone, SecurityGroup) {
    return EucaDialogView.extend({

        initialize : function(args) {
            var self = this;
            this.template = template;

            this.scope = {
                securityGroup: new SecurityGroup(),
                status: 'Ignore me for now',
                cancelButton: {
                  id: 'button-dialog-createsecuritygroup-cancel',
                  click: function() {
                    self.close();
                  }
                },

                createButton: {
                  id: 'button-dialog-createsecuritygroup-save',
                  click: function() {
                    self.close();
       		      }
	            }
            }

            this._do_init();
        },
	});
});
