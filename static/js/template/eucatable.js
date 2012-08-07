(function($, eucalyptus) {
  $.widget('eucalyptus.eucatable', {
    options : { 
                id : 'table', // user of this widget should customize these options
                base_table : null,
                dt_arg : null,
                header_title : 'Manage resources',
                search_refresh : 'Refresh',
                txt_create : 'Create resource',
                txt_found : 'resources found',
                menu_text : 'More actions',
                menu_actions : { delete: ['Delete', function () { deleteAction('table', 1); } ] }
    },

    table : null, // jQuery object to the table

    _init : function() {
      this.table = this.options.base_table.dataTable(this.options.dt_arg);
      // register table, so all support function can find it
      allTablesRef[this.options.id] = this.options.base_table;
      this.decorateHeader({title:this.options.header_title});
      this.decorateSearchBar({refresh: this.options.search_refresh});
      this.decorateTopBar({txt_create: this.options.txt_create, txt_found : this.options.txt_found});
      this.decorateActionMenu({text: this.options.menu_text, actions: this.options.menu_actions });
    },

    _create : function() {
      /*this.table = this.options.base_table.dataTable(this.options.dt_arg);
      this.decorateHeader({title:this.options.header_title});
      this.decorateSearchBar({refresh: this.options.search_refresh});
      this.decorateTopBar({txt_create: this.options.txt_create, txt_found : this.options.txt_found});*/
    },

    _destroy : function() {
    },

    // args.title = title in the header (e.g.,'Manage key pairs');
    decorateHeader : function(args) {
      var thisObj = this; // ref to widget instance
      $header = this.element.find('.table_' + thisObj.options.id + '_header');
      $header.addClass('euca-table-header');
      $header.append(
        $('<span>').text(args.title).append(
          $('<div>').addClass('help-link').append(
            $('<a>').attr('href','#').text('?'))));
      return $header;
    },

    // args.refresh = text 'Refresh'
    decorateSearchBar : function(args) {
      var $searchBar = this.element.find('#'+this.options.id+'_filter');
      $searchBar.append(
        $('<a>').addClass('table-refresh').attr('href','#').text(args.refresh).click(function(){
          thisObj.table.fnReloadAjax();
        }));
      return $searchBar;
    },   

    // args.txt_create (e.g., Create new key)
    // args.txt_found ('e.g., 12 keys found)
    decorateTopBar : function(args) {
      var thisObj = this; // ref to widget instance
      $tableTop = this.element.find('.table_' + this.options.id + '_top');
      $tableTop.addClass('euca-table-length');
      $tableTop.append(
        $('<div>').addClass('euca-table-add').append(
          $('<a>').attr('id','table-'+this.options.id+'-new').addClass('add-resource').attr('href','#').text(args.txt_create)),
        $('<div>').addClass('euca-table-action actionmenu inactive'),
        $('<div>').addClass('euca-table-size').append(
          $('<span>').attr('id','table_' + this.options.id + '_count'),
          $('<span>').attr('id','tbl_txt_found').html('&nbsp; '+args.txt_found),
          'Showing&nbsp;',
          $('<span>').addClass('show selected').text('10'),
          '&nbsp;|&nbsp;',
          $('<span>').addClass('show').text('25'),
          '&nbsp;|&nbsp;',
          $('<span>').addClass('show').text('50'),
          '&nbsp;|&nbsp;',
          $('<span>').addClass('show').text('all')));

      $tableTop.find('span.show').click( function () {
        $(this).parent().children('span').each( function() {
          $(this).removeClass('selected');
        });
        
        if ($(this).text() == 'all')
          thisObj.table.fnSettings()._iDisplayLength = -1;
        else
          thisObj.table.fnSettings()._iDisplayLength = parseInt($(this).text().replace('|',''));
        thisObj.table.fnDraw();
        $(this).addClass('selected');
      });
      // add action to create new
      this.element.find('#table-' + this.options.id + '-new').click( function() {
        $('#' + thisObj.options.id + '-add-dialog').dialog('open');
      });
      return $tableTop;
    },

    /*
      args.text : text (e.g., More actions)
      args.action : { action_key : [Text, click Callback] }
                   e.g., { delete : [Delete, function () { dialog('open'); } ] }
    */
    decorateActionMenu : function (args){
      var thisObj = this; // ref to widget object
      var $menuDiv = this.element.find('div.euca-table-action');
      if ($menuDiv === undefined)
        return undefined;
      if (!args.actions)
        return undefined;

      var $actionItems = $('<li>');
      $.each(args.actions, function (key, value){
        $('<a>').attr('href','#').attr('id', thisObj.options.id+'-'+key).text (value[0]).click( function() {
          value[1].call();  
        }).appendTo($actionItems);
      });

      $menuDiv.append(
        $('<ul>').append(
          $('<li>').append(
            $('<a>').attr('href','#').text(args.text).append(
              $('<span>').addClass('arrow'),
              $('<ul>').append($actionItems)
            ))));

      $menuDiv.find('ul > li > a').click( function(){
        parentUL = $(this).parent().parent();
        if ( !parentUL.parent().hasClass('inactive') ) {
          if ( parentUL.hasClass('activemenu') ){
            parentUL.removeClass('activemenu');
          } else {
            parentUL.addClass('activemenu');
          }
        }
      });

      return $menuDiv;
    }
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
