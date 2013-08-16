define([
   'backbone',
   'rivets',
   'views/databox/databox',
], function(Backbone, rivets, DataBox) {
    return Backbone.View.extend({

        _do_init : function() {
          var self = this;
          $tmpl = $(this.template);

          this.$el.append($tmpl);
          $('#euca-main-container').children().remove();
          this.$el.appendTo($('#euca-main-container'));

          // ATTRIBUTES FOR PAGE DISPLAY. TAKEN FROM DATATABLES
          this.scope.iDisplayStart = 0;
          this.scope.iDisplayLength = 10;
          this.scope.iSortCol  = 0;
          this.scope.sSortDir  = "asc";

          // SET UP FUNCTION CALLS FOR THIS VIEW
          this.setup_scope_calls();
          this.setup_listeners();

          // INITIALIZE THE DATA COLLECTIONS
          this.scope.databox = new DataBox(this.scope.collection);
          this.scope.items = this.scope.databox.getCollectionBySlice(this.scope.iDisplayStart, this.scope.iDisplayLength);

          this.scope.pages = '';          
          this.setup_page_info();

          this.bind();
          this.render(); 

          // NOT SURE WHY THIS IS NOT WORKING - KYO 080613
          //this.scope.collection.on('change add remove reset', this.refresh_view());

        },
        setup_scope_calls: function() {
          var self = this;

          this.scope.clicked_check_all_callback = function(context, event) {
            self.scope.items.each(function(model){
              model.set('clicked', true);
            });
          };

          // TEMP. SOL: THIS SHOUOLD BE DONE VIA RIVETS TEMPLATE - KYO 080613
          this.scope.clicked_row_callback = function(context, event) {
            if( self.count_checked_items() === 0 ){
              $menu = $('#more-actions-'+self.scope.id);
              $menu.addClass("inactive-menu");
            }else{
              $menu = $('#more-actions-'+self.scope.id);
              $menu.removeClass("inactive-menu");
            }
          };

          this.scope.adjust_display_count = function(context, event){
            console.log("Clicked: " + context.srcElement.innerText);
            self.scope.iDisplayStart = 0;
            self.scope.iDisplayLength = context.srcElement.innerText; 
            self.refresh_view();
          };

          this.scope.adjust_display_page = function(context, event){
            console.log("Clicked: " + context.srcElement.innerText);
            var clicked_item = context.srcElement.innerText;
            if( clicked_item === "First" ){
              self.scope.iDisplayStart = 0;
            }else if( clicked_item === "Last" ){
              while( self.scope.collection.length > self.scope.iDisplayStart + self.scope.iDisplayLength ){
                self.scope.iDisplayStart = self.scope.iDisplayStart + self.scope.iDisplayLength;
              }
            }else if( clicked_item === "Previous" ){
              self.scope.iDisplayStart = self.scope.iDisplayStart - self.scope.iDisplayLength;
              if( self.scope.iDisplayStart < 0 ){
                self.scope.iDisplayStart = 0;
              }
            }else if( clicked_item === "Next" ){
              if( self.scope.collection.length > self.scope.iDisplayStart + self.scope.iDisplayLength){
                self.scope.iDisplayStart = self.scope.iDisplayStart + self.scope.iDisplayLength;
              }
            }else{
              self.scope.iDisplayStart = (parseInt(clicked_item) - 1) * self.scope.iDisplayLength; 
            }
            self.refresh_view();
          };

          this.scope.sort_items = function(context, event){
            console.log(context);
            console.log(event);
            var source = self.scope.id.slice(0,-1);   // REMOVE LAST CHAR; ex. eips to eip - KYO 080713
            self.scope.iSortCol = context.srcElement.cellIndex;
            if( self.scope.sSortDir === "asc" ){
              self.scope.sSortDir = "desc";
            }else{
              self.scope.sSortDir = "asc";
            }
            self.scope.databox.sortDataForDataTable(source, self.scope.iSortCol, self.scope.sSortDir);
            self.refresh_view();
          };
        },
        setup_listeners: function(){
          // REGISTER BUTTONS CALBACK - KYO 081613
          this.$el.find('div.euca-table-size').find('a.show').click(function () {
            if($(this).hasClass('selected'))
              return;
            $(this).parent().children('a').each( function() {
              $(this).removeClass('selected');
            });
            $(this).addClass('selected');
          });
        },
        setup_page_info: function(){
          var thisPageLength = this.scope.iDisplayLength;
          var totalCount = this.scope.collection.length;
          var thisIndex = 1;
          var thisCount = 0;

          this.scope.pages = new Backbone.Collection();          
          this.scope.pages.add( new Backbone.Model({index: thisIndex}) );
          thisIndex = thisIndex + 1;
          while( totalCount > thisCount + thisPageLength ){
            this.scope.pages.add( new Backbone.Model({index: thisIndex}) );
            thisIndex = thisIndex + 1;
            thisCount = thisCount + thisPageLength;
          }
          return;
        },
        close : function() {
          this.$el.empty();
        },
        bind: function() {
          this.rivetsView = rivets.bind(this.$el, this.scope);
        },
        render : function() {
          this.rivetsView.sync();
          return this;
        },
        get_element: function() {
          return this.$el;
        },
        refresh_view: function() {
          // PROB: REFRESHMENT OF THE COLLECTION ENDS UP REMOVING THE CHECKED LIST - KYO 081613
          this.adjust_page();
          this.setup_page_info();
          this.render();
          console.log("-- Landing Page View Refresh --");
        },
        adjust_page: function(){
          console.log("iDisplayStart: " + this.scope.iDisplayStart);
          console.log("iDisplayLength: " + this.scope.iDisplayLength);
          this.scope.items = this.scope.databox.getCollectionBySlice(this.scope.iDisplayStart, this.scope.iDisplayStart + this.scope.iDisplayLength);
        },
        count_checked_items: function(){
          var count = 0;
          this.scope.items.each(function(model){
            if( model.get('clicked') === true ){
              count++;
            }
          });
          return count;
        },
        get_checked_items_for_datatables: function(sourceName, columnIdx){
          // TRY TO MATCH THE BEHAVIOR OF GETSELECTROW CALL  -- KYO 0080613
          // THIS NEEDS TO BE SIMPLIFIED.
          var selectedRows = [];
          var source = sourceName;
          // GET THE SOURCE OF THE LANDING PAGE
          console.log("Landing Page Source: " + source);
          // GET THE DATATABLE COLUMN MAP BASED ON THE SOURCE
          var columnMaps = this.scope.databox.columnMap;
          var thisColumnMap = [];
          $.each(columnMaps, function(index, map){
            if( map.name == source ){
              thisColumnMap = map.column;
            }
          });
          console.log("Column Map: " + JSON.stringify(thisColumnMap));
          // SET THE DEFAULT COLUMN ITEM TO "ID"
          var thisValue = "id";
          // SCAN ALL THE MODELS ON THIS LANDING PAGE
          this.scope.items.each(function(model){
            // CHECK IF THE MODEL IS CLICKED
            if( model.get('clicked') === true ){
             console.log("Clicked Row's ID: " + model.get('id'));
             // IF THIS getSelectedRows() FUNCTION IS INVOKED WITH A SPECIFIC COLUMN INDEX, 
	     if(columnIdx){
	       console.log("columnIdx: " + columnIdx);
               // SCAN THE MAP AND FIND THE MATCHING VALUE PER INDEX
               $.each(thisColumnMap, function(index, col){
                 if( col.id == columnIdx ){
                   thisValue = col.value;
                 };
               });
               selectedRows.push(model.toJSON()[thisValue]);
               console.log("Selected Row's Column Value: " + thisValue + "=" + model.toJSON()[thisValue]);
             }else{
               // NO SPECIFIC COLUMN INDEX CASE: SEND THE WHOLE MODEL ARRAY
	       selectedRows.push(model.toJSON());
	     }
            }	
          });  
          return selectedRows;
        },
   });
});

