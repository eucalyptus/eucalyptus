define([
  'app',
	'dataholder',
  'text!./advanced.html!strip',
  'rivets',
  '../shared/model/blockmap',
  '../shared/advanced'
	], function( app, dataholder, template, rivets, blockmap, Advanced ) {
	return Advanced.extend({
    tpl: template,
  });
});
