define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    return new Search(images, ['all_text', 'name', 'volume'], {}, null);
  }
});
