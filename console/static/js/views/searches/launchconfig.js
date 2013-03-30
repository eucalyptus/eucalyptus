define([
  'views/searches/generic',
], function(Search) {
  return function(images) {
    return new Search(images, ['all_text', 'name', 'spot_price', 'instance_type'], {}, null);
  }
});
