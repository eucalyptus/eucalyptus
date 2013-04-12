define(['views/shared/model/advancedmodel'], function(Advanced) {
  return Advanced.extend({
    finish: function(outputModel) {
      outputModel.set(this.toJSON());
    }
  });
});
