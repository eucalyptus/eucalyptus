define(['views/shared/model/typemodel'], function(Type) {
  return Type.extend({
    

    validation: {
      lc_name: {
        required: true,
        msg: 'This field is required.'
      }
    },

    finish: function(outputModel) {
      outputModel.set('name', this.get('lc_name'));
      outputModel.set('instance_type', this.get('instance_type'));
    }

  });
});
