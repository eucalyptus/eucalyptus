// eip model
//

define([
  './eucamodel'
], function(EucaModel) {
  var model = EucaModel.extend({
    namedColumns: ['instance_id'],
    idAttribute: 'public_ip'
  });
  return model;
});
