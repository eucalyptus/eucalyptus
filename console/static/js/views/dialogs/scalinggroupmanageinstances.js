define([
    './eucadialogview', 
    'app', 
    'text!./scalinggroupmanageinstances.html',
    'models/scalinginst'
  ], 
  function(EucaDialogView, app, tpl, ScalingInst) {
    return EucaDialogView.extend({
      initialize: function(args) {
        var self = this;
        this.template = tpl;
        this.model = args.model.at(0);
        // health state label choices
        this.model.set('healthstates', new Backbone.Collection([{state: 'Healthy'}, {state: 'Unhealthy'}]));
        
        var clone = this.model.clone();
        var instances = new Backbone.Collection();

        _.each(clone.get('instances'), function(inst, i) {
          instances.add(new ScalingInst(inst));
        });
        clone.set('instances', instances);


        this.scope = {
          sgroup: clone,

          status: function(obj) {
          },

          submitButton: new Backbone.Model({
            disabled: false, //!this.model.isValid(),
            click: function() {
              self.scope.sgroup.get('instances').each( function(inst) {
                if (inst.get('_deleted') == true) {
                  inst.destroy();
                } else if(inst.hasChanged()) {
                  inst.save(); 
                }
              });

              self.close();
            }
          }),

          cancelButton: {
            click: function() {
              self.close();
            }
          },

          delete: function(e, obj) {
            obj.instance.set("_deleted", true);
          },

          switchToQScale: function() {
            var col = new Backbone.Collection();
            col.add(self.model);
            var qs = app.dialog('quickscaledialog', col);
            self.close();
          },

          showControls: function(e, obj) {
            obj.instance.set('hasFocus', true);
          },

          hideControls: function(e, obj) {
            obj.instance.unset('hasFocus');
          },

          activateButton: function() {

          }
        };

        this._do_init();
      },

    });
});
