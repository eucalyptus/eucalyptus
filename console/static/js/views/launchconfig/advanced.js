define([
  'app',
  'text!./advanced.html!strip',
  'rivets',
  '../shared/model/blockmap',
  '../shared/advanced'
	], function( app, template, rivets, blockmap, Advanced ) {
	return Advanced.extend({
    tpl: template,
  });
});
