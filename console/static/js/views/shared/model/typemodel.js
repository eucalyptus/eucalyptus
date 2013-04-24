define([], function() {
  return Backbone.Model.extend({
    
    initialize: function() {

    },

    validation: {
      type_number: {
        required: true,
        pattern: 'number',
        min: 1,
        max: 99,
        msg: 'This field is required, and must be a number between 1 and 99.'
      },
/*    
      type_names: {
        required: true,
        msg: 'This field is required.'
      }
*/
      instance_type: {
        required: true,
        msg: 'You must select an instance size.'
      },
    },

    finish: function(outputModel) {
      outputModel.set('name', this.get('type_names'));
      outputModel.set('instance_type', this.get('instance_type'));

      // presently the UI doesn't accept two numbers - setting them the same for now
      outputModel.set('min_count', this.get('type_number'));
      outputModel.set('max_count', this.get('type_number'));
    }

  });
});
