define([
	'models/tags',
], 
function(Tags) {
    var tags = new Tags();
	tags.fetch({merge: true, add: true, remove: true});
	return tags;
});
