// volume model
//

define([
    './eucamodel'
], function(EucaModel) {
    var model = EucaModel.extend({
          namedColumns: ['id','snapshot_id']
    });
    return model;
});
