define([
    'models/eucacollection',
    'models/tag'
], function(EucaCollection, Tag) {
    return EucaCollection.extend({
	model: Tag,
	url: '/ec2?Action=DescribeTags'
    });
});
