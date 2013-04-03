define([
	'models/tags',
], 
function(Tags) {
    var tags = new Tags();
	tags.fetch();
	return tags;
});
