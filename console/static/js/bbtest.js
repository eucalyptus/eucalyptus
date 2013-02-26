$(function() {
    require([
	    'models/volumes'
    ], function(Volumes) {
	    var myVols = new Volumes();
	    myVols.on('reset', function() {
		_.each(myVols.toJSON(), function(vol) {
		    console.log('VOL', vol);
		});
	    });
	    myVols.fetch();
    });
});
