define([], function() {
    return Backbone.Model.extend({

      validation: {
        image_id: {
            required: true,
            msg: 'Image selection is required.'
        }
      },

      initialize: function() {

      },

    });
});
