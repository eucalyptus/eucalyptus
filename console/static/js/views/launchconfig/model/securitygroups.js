define(['./securitygroup'], function(sgroup) {
  return Backbone.Collection.extend({
    model: sgroup,
    initialize: function() {

    }
  });
});
