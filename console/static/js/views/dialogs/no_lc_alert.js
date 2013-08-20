define(['./eucadialogview', 'text!./no_lc_alert.html'], function(EucaDialogView, template) {
  return EucaDialogView.extend({
    initialize: function() {
      var self = this;
      this.template = template;
      var scope = {
        model: this.model,
        cancelButton: new Backbone.Model({
          click: function() {
            self.close();
          }
        }),
        linkClick: function(e) {
          e.stopPropagation();
          e.preventDefault();
          self.close();
          window.location = self.model.get('linkTarget');
        }
      };
      this.scope = scope;
      this._do_init();
    }, 
  });
});
