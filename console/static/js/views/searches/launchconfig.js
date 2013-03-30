define([
    'views/searches/generic',
], function(Search) {
    return function(images) {
      return new Search(images, ['name', 'spot_price', 'instance_type'], {}, null);
    }
});
