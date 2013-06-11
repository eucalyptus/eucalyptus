define([
    './eucadialogview', 
    'app', 
    'text!./scalinggroupmanageinstances.html'
  ], 
  function(EucaDialogView, app, tpl) {
    return EucaDialogView.extend({
      initialize: function(args) {
        var self = this;
        this.template = tpl;
        this.model = args.model.at(0);

        // health state label choices
        this.model.set('healthstates', new Backbone.Collection([{state: 'Healthy'}, {state: 'Unhealthy'}]));

        this.scope = {
          sgroup: this.model,

          status: function(obj) {
          },

          submitButton: new Backbone.Model({
            disabled: false, //!this.model.isValid(),
            click: function() {
              sgroup.save();
              self.close();
            }
          }),

          cancelButton: {
            click: function() {
              self.close();
            }
          },

          activateButton: function() {

          }
        };

        this._do_init();
      },

    });
});
