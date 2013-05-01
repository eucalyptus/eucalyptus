define(['app'], function(app) {
    return Backbone.Model.extend({

      validation: {
        id: {
            required: true,
            msg: app.msg("launch_instance_error_image_required")
        }
      },

      finish: function(outputModel) {
        outputModel.set('image_id', this.get('id'));
      }

    });
});
