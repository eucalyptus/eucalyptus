// region model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
        idAttribute: 'name'
    });
    return model;
});
