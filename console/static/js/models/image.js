// image model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
        namedColumns: ['id', 'image'], 
    });
    return model;
});
