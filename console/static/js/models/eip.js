// eip model
//

define([
  './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    idAttribute: 'public_ip'
  });
  return model;
});
