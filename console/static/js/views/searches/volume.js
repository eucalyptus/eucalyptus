define([
    'views/searches/generic',
], function(Search) {
    return function(images) {
      return new Search(images, ['attach_data.status'], {}, null);
    }
});
