define([
    'views/searches/generic',
], function(Search) {
    return function(images) {
      return new Search(images, ['snapshot_id', 'display_id', 'zone', 'region'], {}, null);
    }
});
