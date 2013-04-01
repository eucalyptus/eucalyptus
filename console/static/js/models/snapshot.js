// snapshot model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
        namedColumns: ['id', 'volume_id']
    });
    return model;
});
