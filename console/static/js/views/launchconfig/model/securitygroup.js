define(['views/shared/model/securitygroup'], function(sgroup) {
  return sgroup.extend({

      finish: function(outputModel) {
        outputModel.set('security_groups', this.toJSON());
      }
  });
});
