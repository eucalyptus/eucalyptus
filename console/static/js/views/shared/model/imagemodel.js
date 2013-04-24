define([], function() {
    return Backbone.Model.extend({

      validation: {
        id: {
            required: true,
            msg: 'Image selection is required.'
        }
      },

      finish: function(outputModel) {
        outputModel.set('image_id', this.get('id'));
      }

    });
});
