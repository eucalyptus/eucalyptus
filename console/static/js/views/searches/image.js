define([
    'views/searches/generic',
], function(Search) {
    return function(images) {
      return new Search(images, ['architecture', 'description', 'name', 'owner', 'platform', 'root_device'], {}, null);
    }
});
