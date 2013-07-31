define([
    './eucadialogview', 
    'underscore',
    'app', 
    'text!./scalinggroupmanageinstances.html',
    'models/scalinginst',
    'views/searches/scalinginst'
  ], 
  function(EucaDialogView, _, app, tpl, ScalingInst, Search) {
    return EucaDialogView.extend({
      initialize: function(args) {
        var self = this;
        var me = this;
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
          help: {title: null, content: help_scaling.dialog_manage_instances, url: help_scaling.dialog_manage_instances_url, pop_height: 600},
          width: 800,
          sgroup: clone,

          status: function(obj) {
          },

          submitButton: new Backbone.Model({
            id: 'button-dialog-scalingmanageinst-save',
            disabled: false, //!this.model.isValid(),
            click: function() {
              var toDelete = self.scope.sgroup.get('instances').pluck('instance_id');
              _.each(toDelete, function(id) {
                var targetModel = self.scope.sgroup.get('instances').findWhere({'instance_id': id});
                if (targetModel.get('_deleted') == true) {
                  targetModel.destroy({
                    success: function(model, response, options){
                      if(model != null){
                        notifySuccess(null, $.i18n.prop('manage_scaling_group_terminate_success', id));
                      }else{
                        notifyError($.i18n.prop('manage_scaling_group_terminate_error', id), undefined_error);
                      }
                    },
                    error: function(model, jqXHR, options){
                      notifyError($.i18n.prop('manage_scaling_group_terminate_error', id), getErrorMessage(jqXHR));
                    }
                  });
                } else if(targetModel.hasChanged()) {
                  targetModel.save({}, {
                    success: function(model, response, options){
                      if(model != null){
                        notifySuccess(null, $.i18n.prop('manage_scaling_group_set_health_success', id));
                      }else{
                        notifyError($.i18n.prop('manage_scaling_group_set_health_error', id), undefined_error);
                      }
                    },
                    error: function(model, jqXHR, options){
                      notifyError($.i18n.prop('manage_scaling_group_set_health_error', id), getErrorMessage(jqXHR));
                    }
                  });
                }
              });

              self.close();
            }
          }),

          cancelButton: {
            id: 'button-dialog-scalingmanageinst-cancel',
            click: function() {
              self.close();
            }
          },

          delete: function(e, obj) {
            e.stopPropagation();
            obj.instance.set("_deleted", true);
          },

          undoDelete: function(e, obj) {
            e.stopPropagation();
            obj.instance.unset("_deleted");
          },

          switchToQScale: function() {
            var col = new Backbone.Collection();
            col.add(self.model);
            var qs = app.dialog('quickscaledialog', col);
            self.close();
          },

          search: new Search(clone.get('instances')),
        };

        this.scope.instances = this.scope.search.filtered;
        this.scope.search.filtered.on('add remove sync reset', function() {
            self.render();
        });
        this._do_init();
      },

    });
});
