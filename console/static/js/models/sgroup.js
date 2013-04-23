// sgroup model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
      validation: {},
      idAttribute: 'name'
    });
    return model;
});
