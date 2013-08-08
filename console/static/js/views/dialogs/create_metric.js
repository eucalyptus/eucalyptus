define([
   'backbone',
   'app',
   './eucadialogview',
   'text!./create_metric.html!strip',
], function(Backbone, app, EucaDialogView, template) {
    return EucaDialogView.extend({
        initialize : function(args) {
            var self = this;
            this.template = template;
            var metric = args.model.metric;

            var scope = {
                metric: metric,
                error: new Backbone.Model(),
                status: '',

                cancelButton: new Backbone.Model({
                    id: 'button-dialog-createmetric-cancel',
                    click: function() {
                       self.close();
                    }
                }),

                submitButton: new Backbone.Model({
                  id: 'button-dialog-createmetric-save',
                  click: function() {
                      metric.trigger('submit');
                      self.close();
                  }
                })
            }
            this.scope = scope;

            this._do_init();
        },
	});
});
