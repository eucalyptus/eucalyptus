// instance model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
        namedColumns: ['id']
    });
    return model;
});
