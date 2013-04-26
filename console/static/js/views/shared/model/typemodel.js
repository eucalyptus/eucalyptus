define(['app'], function(app) {
  return Backbone.Model.extend({
    
    initialize: function() {

    },

    validation: {
      type_number: {
        required: true,
        pattern: 'number',
        min: 1,
        max: 99,
        msg: app.msg("launch_instance_error_number_required")
      },
    
      type_names_count: function(value, attr, computedState) {
          if (value != undefined && value > 0 && value != computedState.type_number) {
            return app.msg("launch_instance_error_name_number_inequality");
          }
      },

      instance_type: {
        required: true,
        msg: app.msg("launch_instance_error_size_required")
      },
    },

    finish: function(outputModel) {
      outputModel.set('names', this.get('type_names'));
      outputModel.set('instance_type', this.get('instance_type'));
      outputModel.set('tags', this.get('tags'));

      // presently the UI doesn't accept two numbers - setting them the same for now
      outputModel.set('min_count', this.get('type_number'));
      outputModel.set('max_count', this.get('type_number'));
    }

  });
});
