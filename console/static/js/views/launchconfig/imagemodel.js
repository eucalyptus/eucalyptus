define([], function() {
    return Backbone.Model.extend({
      image_id: '',
      image_location: '',
      image_platform: '',
      image_description: '',
      image_iconclass: '',
      validation: {
        image_id: {
            required: true
        }
      },
      initialize: function() {

      },

/*
      validate: function(atts, options) {
          console.log('VALIDATOR', atts.image_id); 
        if (undefined == atts.image_id) {
          return "You must select an image from the list.";
        }
      }
*/
    });
});
