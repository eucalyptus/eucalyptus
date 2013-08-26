define([
   'backbone',
   'rivets',
   'views/databox/databox',
], function(Backbone, rivets, DataBox) {
    return Backbone.View.extend({

        // BASE INITIALIZE FUNCTION FOR ALL LANDING PAGES
        _do_init : function() {
          var self = this;
          $tmpl = $(this.template);

          // OUT OF THE EXISTING TEMPLATE, FIND THE 'EUCA-MAIN-CONTAINER' DIV AND REPLACE IT WITH THIS RIVETS TEMPLATE
          this.$el.append($tmpl);
          $('#euca-main-container').children().remove();
          this.$el.appendTo($('#euca-main-container'));

          // ATTRIBUTES FOR PAGE/TABLE DISPLAY. TAKEN FROM DATATABLES
          this.scope.iDisplayStart = 0;
          this.scope.iDisplayLength = 10;
          this.scope.iSortCol  = 0;
          this.scope.sSortDir  = "asc";

          // SET UP FUNCTION CALLS AND LISTENER FOR THIS VIEW
          this.setup_scope_calls();
          this.setup_listeners();

          // INITIALIZE THE DATABOX INSTANCE
          this.scope.databox = new DataBox(this.scope.collection);

          // CREATE A DEFAULT COLLECTION TO RENDER
          this.scope.items = this.scope.databox.getCollectionBySlice(this.scope.iDisplayStart, this.scope.iDisplayLength);

          // INITIALIZE THE ITEM VIEWS COLLECTION FOR THIS LANDING PAGE
          this.scope.item_views = new Backbone.Collection();
          this.scope.is_check_all = false;

          // COMPUTE THE PAGE INDEX ARRAY FOR THE PAGE BAR ON THE BOTTOM-RIGHT CORNER
          this.scope.pages = '';          
          this.setup_page_info();

          // BIND AND RENDER
          this.bind();
          this.render(); 

        },
        // SETUP VARIOUS CALLBACK CALLS FOR THE LANDING PAGE
        setup_scope_calls: function() {
          var self = this;

          this.scope.get_table_row_class = function(e){
            if( e.item_index % 2 == 1 ){
              return "odd";
            }
            return "even";
          };

          // CHECK-ALL BUTTON CALLBACK
          this.scope.clicked_check_all_callback = function(context, event) {
            console.log("is_check_all: " + self.scope.is_check_all);
            if ( self.scope.is_check_all === false ){
              self.scope.is_check_all = true; 
            }else{
              self.scope.is_check_all = false; 
            }

            self.scope.items.each(function(model){
              // MARK THE CURRENT MODELS FOR 'DATA-CHECKED' FIELD CHECK
              model.set('clicked', self.scope.is_check_all);

              var this_id = model.get('id');
              // SPECIAL CASE FOR EIP LANDING PAGE WHERE THERE IS NO ID FOR THE MODEL
              if ( self.scope.id === "eips" ){
                this_id = model.get('public_ip');
              }
              // REPLICATE THE CLICK STATE OVER TO THE 'ITEM_VIEWS' COLLECTION
              self.set_checked_item(this_id, self.scope.is_check_all);
            });
            self.activate_more_actions_button();
          };

          // CHECK-BOX CALLBACK FOR EACH ROW
          this.scope.clicked_row_callback = function(context, event) {
            var this_id = event.item.id;
            // SPECIAL CASE FOR 'EIP' AND 'KEYPAIR' LANDING PAGES WHERE THERE IS NO ID FOR THE MODEL
            if ( self.scope.id === "eips" ){
              this_id = event.item.get('public_ip');
            }else if( self.scope.id === "keys" ){
              this_id = event.item.get('name');
            }else if ( self.scope.id === "scaling" || self.scope.id === "launchconfig" ){
              this_id = event.item.get('name');
            }
            var this_model = self.scope.item_views.get(this_id);
            // REPLICATE THE CLICK STATE OVER TO THE 'ITEM_VIEWS' COLLECTION
            if( this_model === undefined || this_model.get('clicked') === false ){
              self.set_checked_item(this_id, true);
            }else{
              self.set_checked_item(this_id, false);
            }
            self.activate_more_actions_button();
          };

          this.scope.expand_row = function(context, event){
            var thisModel = '';
            var this_id = event.item.id;
            // SPECIAL CASE FOR EIP LANDING PAGE WHERE THERE IS NO ID FOR THE MODEL
            if ( self.scope.id === "eips" ){
              this_id = event.item.get('public_ip');
              thisModel = self.scope.items.where({public_ip: this_id})[0];
            }else if ( self.scope.id === "scaling" || self.scope.id === "launchconfig" ){
              this_id = event.item.get('name');
              thisModel = self.scope.items.where({name: this_id})[0];
            }else{
              thisModel = self.scope.items.get(this_id);
            }
            console.log("Clicked to expand: " + this_id);
            var is_expanded = true;
            // IF ALREADY EXPANDED, CLOSE IT
            if( thisModel.get('expanded') === true ){
              is_expanded = false;
            }
            thisModel.set('expanded', is_expanded);
            self.set_expanded_item(this_id, is_expanded);
            self.refresh_view();
          };

          // DISPLAY COUNT ADJUSTMENT BAR (TOP-RIGHT) CALLBACK
          this.scope.adjust_display_count = function(context, event){
            console.log("Clicked: " + context.srcElement.innerText);
            self.scope.iDisplayStart = 0;
            self.scope.iDisplayLength = context.srcElement.innerText; 
            self.refresh_view();
          };

          // PAGE ADJUSTMNET BAR (BOTTOM-RIGHT) CALLBACK
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

          // COLUMN SORT CALLBACK
          this.scope.sort_items = function(context, event){
            console.log(context);
            console.log(event);
            var source = self.scope.id.slice(0,-1);   // REMOVE LAST CHAR; ex. eips to eip - KYO 080713
            if( source === "key" ){   // SPECIAL CASE FOR KEYPAIR - KYO 082113
              source = "keypair";
            };
            self.scope.iSortCol = context.srcElement.cellIndex;
            if( self.scope.sSortDir === "asc" ){
              self.scope.sSortDir = "desc";
            }else{
              self.scope.sSortDir = "asc";
            }
            console.log("SORT - source: " + source + " iSortCol: " + self.scope.iSortCol + " sSortDir: " + self.scope.sSortDir);
            self.scope.databox.sortDataForDataTable(source, self.scope.iSortCol, self.scope.sSortDir);
            self.refresh_view();
          };
        },
        // SET UP VARIOUS LISTENERS FOR THE LANDINGE PAGE
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
        // COMPUTE THE PAGE INDEX ARRAY FOR THE PAGE BAR ON THE BOTTOM-RIGHT CORNER
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
          this.recover_checked_items();
          this.recover_expanded_items();
          this.activate_more_actions_button();
          this.setup_page_info();
          this.render();
          console.log("-- Landing Page View Refresh --");
        },
        adjust_page: function(){
          console.log("iDisplayStart: " + this.scope.iDisplayStart);
          console.log("iDisplayLength: " + this.scope.iDisplayLength);
          this.scope.items = this.scope.databox.getCollectionBySlice(this.scope.iDisplayStart, this.scope.iDisplayStart + this.scope.iDisplayLength);
        },
        activate_more_actions_button: function(){
          // ACTIVE "MORE ACTIONS" BUTTON
          // TEMP. SOL: THIS SHOUOLD BE DONE VIA RIVETS TEMPLATE - KYO 080613
          if( this.count_checked_items() === 0 ){
            $menu = $('#more-actions-'+this.scope.id);
            $menu.addClass("inactive-menu");
          }else{
            $menu = $('#more-actions-'+this.scope.id);
            $menu.removeClass("inactive-menu");
          }
        },
        // IN CASE OF REFRESH, RECOVER THE CHECKED ITEMS FOR THIS VIEW
        recover_checked_items: function(){
          var self = this;
          this.scope.item_views.each(function(item_view){
            var this_id = item_view.get('id');
            var is_clicked = item_view.get('clicked');
            console.log("ITEM VIEW ID: " + this_id + " IS_CLICKED: " + is_clicked);
            var this_model = self.scope.items.get(this_id)
            // SPECIAL CASE FOR 'EIP' AND 'KEYPAIR' LANDING PAGE WHERE THERE IS NO ID FOR THE MODEL
            if( self.scope.id === "eips" ){
                this_model = self.scope.items.where({public_ip: this_id})[0];
            }else if( self.scope.id === "keys" ){
                this_model = self.scope.items.where({name: this_id})[0];
            }else if ( self.scope.id === "scaling" || self.scope.id === "launchconfig" ){
                this_model = self.scope.items.where({name: this_id})[0];
            }
            if( this_model !== undefined ){
              this_model.set('clicked', is_clicked);
            }
          })
        },
        // IN CASE OF REFRESH, RECOVER THE EXPANDED ITEMS FOR THIS VIEW
        recover_expanded_items: function(){
          var self = this;
          this.scope.item_views.each(function(item_view){
            var this_id = item_view.get('id');
            var is_expanded = item_view.get('expanded');
            console.log("ITEM VIEW ID: " + this_id + " IS_EXPANDED: " + is_expanded);
            var this_model = self.scope.items.get(this_id)
            // SPECIAL CASE FOR EIP LANDING PAGE WHERE THERE IS NO ID FOR THE MODEL
            if ( self.scope.id === "eips" ){
                this_model = self.scope.items.where({public_ip: this_id})[0];
            }else if ( self.scope.id === "scaling" || self.scope.id === "launchconfig" ){
                this_model = self.scope.items.where({name: this_id})[0];
            }
            if( this_model !== undefined ){
              this_model.set('expanded', is_expanded);
            }
          })
        },
        // TRACK THE CLICK STATE OF THIS ITEM IN THE 'ITEM_VIEWS' COLLECTION
        set_checked_item: function(this_id, is_clicked){
          var this_model = this.scope.item_views.get(this_id);
          if( this_model === undefined ){
            this_model = new Backbone.Model({id: this_id, clicked: is_clicked});
            this.scope.item_views.add(this_model);
            console.log("THIS MODEL: " + this_id + " CLICKED: " + is_clicked); 
          }else{
            this_model.set('clicked', is_clicked);
            console.log("THIS MODEL: " + this_id + " CLICKED: " + this_model.get('clicked')); 
          }  
        },
        // TRACK THE EXPANDED STATE OF THIS ITEM IN THE 'ITEM_VIEWS' COLLECTION
        set_expanded_item: function(this_id, is_expanded){
          var this_model = this.scope.item_views.get(this_id);
          if( this_model === undefined ){
            this_model = new Backbone.Model({id: this_id, expanded: is_expanded});
            this.scope.item_views.add(this_model);
            console.log("THIS MODEL: " + this_id + " EXPANDED: " + is_expanded); 
          }else{
            this_model.set('expanded', is_expanded);
            console.log("THIS MODEL: " + this_id + " EXPANDED: " + this_model.get('expanded')); 
          }  
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

