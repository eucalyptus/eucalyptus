/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

(function($, eucalyptus) {
  $.widget('eucalyptus.launcher', $.eucalyptus.eucawidget, {
    options : {
      image : null,
      type : null,
      security : null,
      advanced : null
    },
    launchParam : {
      emi : null,
      type : null,
      number : null,
      zone : null,
      keypair : null,
      sgroup: null, // this and above are the required fields
      kernel: null,
      ramdisk: null,
      data: null,
      data_file: null,
      private_addressing: false,
      device_map : [],
    },
    _keypairCallback : null,
    _sgCallback : null,
    _create : function() { },
    _init : function() {
      // read template and attach to the widget element
      var thisObj = this;
      $.each(thisObj.launchParam, function(k,v){
        thisObj.launchParam[k] = null;
      });

      var $tmpl = $('html body').find('.templates #launchWizardTmpl').clone();
      var $wrapper = $($tmpl.render($.extend($.i18n.map, help_launcher)));
      var $launcher = $wrapper.children().first();
      var $launcherHelp = $wrapper.children().last();
      // make section headers
      thisObj._makeSectionHeader($launcher);
 
      // make section buttons 
      thisObj._makeSectionButton($launcher);
      // make sections (image, type, security, advanced options)

      thisObj._makeImageSection($launcher.find('#launch-wizard-image'));
      thisObj._makeTypeSection($launcher.find('#launch-wizard-type'));
      thisObj._makeSecuritySection($launcher.find('#launch-wizard-security'));
      thisObj.makeAdvancedSection($launcher.find('#launch-wizard-advanced'));

      $launcher.find('#launch-wizard-cancel').find('a').click( function(e){
        var $container = $('html body').find(DOM_BINDING['main']);
        $container.maincontainer("changeSelected", e, {selected:'instance'});
      });
      $launcher.appendTo(this.element); //$wrapper);

      thisObj._addHelp($launcherHelp);

      thisObj._initImageSection();
      this.element.qtip();
     },

    _addHelp : function(help){
      var thisObj = this;
      var $header = this.element.find('.box-header');
      var $target = this.element.find('.wizard-wrapper');
      $header.find('span').append(
          $('<div>').addClass('help-link').append(
            $('<a>').attr('href','#').text('?').click( function(evt){
              thisObj._flipToHelp(evt,{content: help, url: help_launcher.landing_content_url}, $target);
            })));
      return $header;
    },

     close : function(){ 
       var thisObj = this;
       if(thisObj._keypairCallback){
         cancelRepeat(thisObj._keypairCallback);
       }
       if(thisObj._sgCallback){
         cancelRepeat(thisObj._sgCallback);
       }
       thisObj.options['image'] = null;
       thisObj.options['type'] = null;
       thisObj.options['security'] = null;
       thisObj.options['advanced'] = null;
       this._super('close');
     },

    _destroy : function() { },

    _makeSectionHeader : function($launcher) {
      var $header = $launcher.find('#launch-wizard-image-header');
      $header.append($('<span>').addClass('required-label').html(launch_instance_section_header_image));

      $header = $launcher.find('#launch-wizard-type-header');
      $header.append($('<span>').addClass('required-label').html(launch_instance_section_header_type));

      $header = $launcher.find('#launch-wizard-security-header');
      $header.append($('<span>').addClass('required-label').html(launch_instance_section_header_security));
      
      $header = $launcher.find('#launch-wizard-advanced-header');
      $header.append($('<span>').html(launch_instance_section_header_advanced));
    },
    _enableImageLink : function() {
      var thisObj = this;
      var $header = this.element.find('#launch-wizard-image-header');
      $header.children().detach();
      $header.append($('<a>').attr('href', '#').addClass('required-label').html(launch_instance_section_header_image).click( function(e) {
        var imgSection = thisObj.element.find('#launch-wizard-image-contents');
        if(! $header.hasClass('expanded')) { 
          var selectedTableSize = thisObj.element.find('#wizard-img-table-size a.selected').text();
          var displayLength = thisObj._imageTable.fnSettings()._iDisplayLength;
          if(! ((selectedTableSize===showing_all && displayLength < 0) ||
                 (parseInt(selectedTableSize) === displayLength))){
            var allLinks = thisObj.element.find('#wizard-img-table-size a.show');
            for(i in allLinks){
              if( ($(allLinks[i]).text() === showing_all && displayLength <0 ) || 
                   (parseInt($(allLinks[i]).text()) === displayLength))
              {
                $(allLinks[i]).trigger('click');
                break;
              }
            }
          }
          thisObj._selectedSection.slideToggle('fast');
          imgSection.slideToggle('fast');
          thisObj._selectedSection = imgSection;
          $header.addClass('expanded');
          thisObj.element.find('#launch-wizard-type-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-security-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-advanced-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-image-contents').find('select').first().focus();
        }
      }));
    },
    _enableTypeLink : function() {
      var thisObj = this;
      var $header = this.element.find('#launch-wizard-type-header');
      $header.children().detach();
      $header.append($('<a>').attr('href', '#').addClass('required-label').html(launch_instance_section_header_type).click(function(e) {
        var typeSection = thisObj.element.find('#launch-wizard-type-contents');
        if(! $header.hasClass('expanded')) { 
          thisObj._selectedSection.slideToggle('fast');
          typeSection.slideToggle('fast');
          thisObj._selectedSection = typeSection;
          $header.addClass('expanded');
          thisObj.element.find('#launch-wizard-image-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-security-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-advanced-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-type-contents').find('input').first().focus();
        }
      }));
    },
    _enableSecurityLink : function() {
      var thisObj = this;
      var $header = this.element.find('#launch-wizard-security-header');
      $header.children().detach();
      $header.append($('<a>').attr('href', '#').addClass('required-label').html(launch_instance_section_header_security).click(function(e){
        var secSection = thisObj.element.find('#launch-wizard-security-contents');
        if(! $header.hasClass('expanded')){
          thisObj._selectedSection.slideToggle('fast');
          secSection.slideToggle('fast');
          thisObj._selectedSection = secSection;
          $header.addClass('expanded');
          thisObj.element.find('#launch-wizard-image-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-type-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-advanced-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-security-contents').find('select').first().focus();
        }
      }));
    },
    _enableAdvancedLink : function (){
      var thisObj = this;
      var $header = this.element.find('#launch-wizard-advanced-header');
      $header.children().detach();
      $header.append($('<a>').attr('href', '#').html(launch_instance_section_header_advanced).click(function(e){
        var advSection = thisObj.element.find('#launch-wizard-advanced-contents');
        if(! $header.hasClass('expanded')){
          thisObj._selectedSection.slideToggle('fast');
          advSection.slideToggle('fast');
          thisObj._selectedSection = advSection;
          $header.addClass('expanded');
          thisObj.element.find('#launch-wizard-image-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-type-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-security-header').removeClass('expanded');
          thisObj.element.find('#launch-wizard-advanced-contents').find('textarea').first().focus();
        }
      }));
    },
    _makeSectionButton : function($launcher) {
      var thisObj = this;
      var $imageBtn = $launcher.find('#launch-wizard-buttons-image');
      $imageBtn.append($('<button>').attr('id', 'launch-wizard-buttons-image-next').addClass('button').html(launch_instance_btn_next_type).click( function(e){
        var typeSection = $launcher.find('#launch-wizard-type-contents');
        thisObj._selectedSection.slideToggle('fast');
        typeSection.slideToggle('fast');
        thisObj._selectedSection = typeSection;
        thisObj._initTypeSection();
      }));
      var $typeBtn = $launcher.find('#launch-wizard-buttons-type');
      $typeBtn.append($('<button>').attr('id', 'launch-wizard-buttons-type-next').addClass('button').html(launch_instance_btn_next_security).click( function(e){
        var secSection = $launcher.find('#launch-wizard-security-contents');
        thisObj._selectedSection.slideToggle('fast');
        secSection.slideToggle('fast');
        thisObj._selectedSection = secSection;
        thisObj._initSecuritySection();
      }));
      var $securityBtn = $launcher.find('#launch-wizard-buttons-security');
      $securityBtn.append(
        $('<button>').attr('id','launch-wizard-buttons-security-launch').addClass('button').html(launch_instance_btn_launch).click( function(e){
        }),
        $('<div>').addClass('form-row advanced-link-wrapper').append(  
          $('<label>').attr('for','launch-wizard-buttons-security-advanced-link').text(or_label), 
          $('<a>').attr('id','launch-wizard-buttons-security-advanced-link').attr('href','#').html(launch_instance_btn_next_advanced).click( function(e){
            var advSection = $launcher.find('#launch-wizard-advanced-contents');
            thisObj._selectedSection.slideToggle('fast');
            advSection.slideToggle('fast');
            thisObj._selectedSection = advSection;
            thisObj._initAdvancedSection();
          }))); 
      var $advancedBtn = $launcher.find('#launch-wizard-buttons-advanced');
      $advancedBtn.append(
        $('<button>').attr('id', 'launch-wizard-buttons-advanced-launch').addClass('button').html(launch_instance_btn_launch).click( function(e){
        })); 
    },
    _selectedSection : null,
    _imageTable : null,

    _makeImageSection : function($section){ 
      var thisObj = this;
      var $content = $section.find('#launch-wizard-image-main-contents');

      var $table = $content.find('table');
      var $summary = '';
      var drawCallback = function(oSettings){
        $section.find('table tbody').find('tr').each(function(index, tr) {
        // add custom td handlers
          var $currentRow = $(tr);
          if(!$currentRow.data('events') || !('click' in $currentRow.data('events'))){
            $currentRow.unbind('click').bind('click', function (e) {
              var $selectedRow = $currentRow; 
              $section.find('table tbody').find('tr.selected-row').each(function(){
                $(this).toggleClass('selected-row');
              });
              $(this).toggleClass('selected-row');
              e.stopPropagation();

              var imgClass = $selectedRow.find('td div').attr('class');
              var imgName = $selectedRow.find('.image-name').first().html();
              var emi = $selectedRow.find('.image-id-arch').children().first().text();
              thisObj.launchParam['emi'] = emi;
              if(! thisObj.element.hasClass('euca-hidden-container')) // not called from launch-more-like-this
                thisObj.element.find('#launch-wizard-image-emi').val(emi).trigger('change');
              $summary =  $('<div>').addClass(imgClass).addClass('summary').append(
                            $('<div>').text(launch_instance_summary_image), $('<span>').text(emi),
                            $('<div>').text(launch_instance_summary_platform), $('<span>').text(imgName)
                          );
              thisObj._setSummary('image', $summary);
            });
          }
          // when image is pre-populated (launch-wizard called from image landing)
          var emi = $currentRow.find('.image-id-arch').children().first().text();
          if(thisObj.options.image && thisObj.options.image === emi){
            $currentRow.trigger('click');
            $section.find('#launch-wizard-buttons-image-next').trigger('click');
            thisObj._imageTable.fnSettings()._iDisplayLength = 5;
            thisObj.options.image = null;
          }
        });
        $section.find('div#launch-images_filter').find('input').watermark(launch_instance_image_search_watermark);
        $section.find('div#launch-images_filter').find('input').attr('title', launch_instance_image_search_text_tip);
      };

      var image_self = false; 
      var dtArg = { 
          "sAjaxSource": "../ec2?Action=DescribeImages",
          "fnServerData": function (sSource, aoData, fnCallback) {
                $.ajax( {
                    "dataType": 'json',
                    "type": "POST",
                    "url": sSource,
                    "data": "_xsrf="+$.cookie('_xsrf'),
                    "success": fnCallback
                });

          },
          "bSortClasses" : false,
          "bSortable" : false,
          "oLanguage" : { "sProcessing": "<img src=\"images/dots32.gif\"/> &nbsp; <span>Loading...</span>", 
                          "sLoadingRecords": "",
                          "sSearch": "",
                          "sZeroRecords": $.i18n.prop('resource_no_records', image_plural),
                        },
          "aoColumns": [
             { // platform
               "fnRender" : function(oObj) { 
                 var emi = oObj.aData.id;
                 var desc = oObj.aData.description ? oObj.aData.description : oObj.aData.location;
                 var arch = oObj.aData.architecture;
                 arch=arch.replace('i386', '32 bit')
                 arch=arch.replace('x86_64', '64 bit');

                 var name = '';
                 var imgKey = inferImage(oObj.aData.location, desc, oObj.aData.platform);
                 if(imgKey)
                   name = getImageName(imgKey);
                 var $cell = $('<div>').addClass(imgKey).addClass('image-type').append(
                               $('<div>').addClass('image-name').text(name), // should be linux, windows, or distros
                               $('<div>').addClass('image-id-arch').append($('<span>').text(emi), $('<span>').text(arch)),
                               $('<div>').addClass('image-description').html(desc).text()); 
                 
                 return $cell.wrap($('<div>')).parent().html();
               }
             },
             {
               "bVisible": false,
               "fnRender" : function(oObj){
                 if(!oObj.aData.platform)
                   return 'linux';
                 else
                   return oObj.aData.platform;
               }
             },
             {
               "bVisible": false,
               "fnRender" : function(oObj){
                 var results = describe('sgroup');
                 var group = null;
                 for(i in results){
                   if(results[i].name === 'default'){
                     group = results[i];
                     break;
                   }
                 } 
                 if(group && group.owner_id === oObj.aData.ownerId)
                   return 'self'; // equivalent of 'describe-images -self'
                 else
                   return 'all'; 
               }
             },
             {
               "bVisible": false,
               "mDataProp": "type",             
             },
             {  
               "bVisible": false,
               "mDataProp": "architecture"
             },
             { 
               "bVisible": false,
               "mDataProp": "root_device_type"
             },
             {
               "bVisible": false,
               "mDataProp": "state",
             }
           ],
           "sDom" : "<\"#filter-wrapper\"<\"#owner-filter\"><\"#platform-filter\"><\"clear\"><\"#arch-filter\"><\"clear\"><\"#root-filter\"><\"clear\">f><\"#table-wrapper\" <\"#wizard-img-table-size\"> tr<\"clear\">p>", 
           "sPaginationType" : "full_numbers",
           "iDisplayLength" : thisObj.options.image ? -1: 5,
           "bProcessing" : true,
           "sAjaxDataProp" : "results",
           "bAutoWidth" : false,
           "fnDrawCallback" : function( oSettings ) {
              drawCallback(oSettings);
              if($section.find('table tbody').find('tr.selected-row').length === 0)
                $section.find('table tbody').find('tr').first().trigger('click'); 
            }
          };
      this._imageTable = $table.dataTable(dtArg);
    
      $section.find('#filter-wrapper').prepend($('<div>').addClass('wizard-section-label').html(launch_instance_image_table_refine));
      $section.find('#table-wrapper').prepend($('<div>').addClass('wizard-section-label required-label').html(launch_instance_image_table_header));

      $.fn.dataTableExt.afnFiltering = [];
      $.fn.dataTableExt.afnFiltering.push(
        function( oSettings, aData, iDataIndex ) {
          if (oSettings.sInstance !== $table.attr('id'))
            return true;
          return (aData[3] === 'machine'  && aData[6] ==='available');
        }); // display only machine images

      var filters = [{name:"platform", options: ['all', 'linux','windows'], text: [launch_instance_image_table_platform_all, launch_instance_image_table_platform_linux, launch_instance_image_table_platform_windows], filter_col:1}, 
                     {name:"arch", options: ['all','i386','x86_64'], text: [launch_instance_image_table_arch_all, launch_instance_image_table_arch_32, launch_instance_image_table_arch_64], filter_col:4 },
                     {name:"root", options: ['all', 'instance-store', 'ebs'], text: [launch_instance_image_table_root_all, launch_instance_image_table_root_inst_store, launch_instance_image_table_root_ebs], filter_col:5 },
                     {name:"owner", options: ['all', 'self'], text: [launch_instance_image_table_owner_all, launch_instance_image_table_owner_me], filter_col:2}];
      $.each(filters, function (idx, filter){
        var $filter = $section.find('#'+filter['name']+'-filter');
        $filter.addClass('euca-table-filter');
          $filter.append(
            $('<select>').attr('title', $.i18n.prop('launch_instance_'+filter['name']+'_select_tip')).attr('id',filter['name']+'-selector'));
          var $selector = $filter.find('#'+filter['name']+'-selector');
           
          for (i in filter.options){
            var option = filter.options[i];
            var text = (filter.text&&filter.text.length>i) ? filter.text[i] : option; 
            $selector.append($('<option>').val(option).text(text));
          }
         
          if(filter['filter_col']){
            $.fn.dataTableExt.afnFiltering.push(
	      function( oSettings, aData, iDataIndex ) {
                if (oSettings.sInstance !== $table.attr('id'))
                  return true;
                var selectorVal = thisObj.element.find('select#'+filter['name']+'-selector').val();
                if(filter['alias'] && filter['alias'][selectorVal]){
                  var aliasTbl = filter['alias'];
                  return aliasTbl[selectorVal] === aData[filter['filter_col']];
                }else if ( selectorVal !== filter['options'][0] ){ // not 'all'
                    return selectorVal === aData[filter['filter_col']];
                }else
                  return true;
            });
          }
          $selector.change( function() { thisObj._imageTable.fnDraw(); } );
      });

      $section.find('#launch-wizard-buttons-image-next').click( function(e){
        thisObj._setSummary('image', $summary);
      });

      $content = $section.find('#launch-wizard-image-contents').slideToggle('fast');
      thisObj._selectedSection = $content;
    },
    _initImageSection : function(){ 
      var thisObj = this;
      this._enableImageLink();
      thisObj.element.find('#wizard-img-table-size').children().detach();

      thisObj.element.find('#wizard-img-table-size').append(
        $('<span>').attr('id','img-table-count'),
          '&nbsp;', showing_label,
          $('<a>').attr('href','#').addClass('show').text('5'),
          '&nbsp;|&nbsp;', // TODO: don't use nbsp; in place for padding!
          $('<a>').attr('href','#').addClass('show').text('10'),
          '&nbsp;|&nbsp;',
          $('<a>').attr('href','#').addClass('show').text('20'),
          '&nbsp;|&nbsp;',
          $('<a>').attr('href','#').addClass('show').text(showing_all));

      var tableLength = thisObj._imageTable.fnSettings()._iDisplayLength; 
      $.each(thisObj.element.find('#wizard-img-table-size a.show'), function(){
        if(tableLength < 0 && $(this).text() === showing_all)
          $(this).addClass('selected'); 
        else if (tableLength === parseInt($(this).text()))
          $(this).addClass('selected');
      });

      thisObj.element.find('#wizard-img-table-size a.show').unbind('click').bind('click',function () {
        if($(this).hasClass('selected'))
          return;
        var numEntry = $(this).text().replace('|','');
        if(numEntry===showing_all)
          numEntry = "-1";
        $(this).parent().children('a').each( function() {
          $(this).removeClass('selected');
        });
        thisObj._imageTable.fnSettings()._iDisplayLength = parseInt(numEntry);
        thisObj._imageTable.fnDraw();
        $(this).addClass('selected');
        return false;
      });

      thisObj.element.find('#launch-wizard-image-header').addClass('expanded');
      thisObj.element.find('#launch-wizard-type-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-security-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-advanced-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-image-contents').find('select').first().focus();
    },
   
    _makeTypeSection : function($section){
      var thisObj = this;
      var $content = $section.find('#launch-wizard-type-main-contents');
      var $size = $content.find('#launch-wizard-type-size');
      var $option = $content.find('#launch-wizard-type-options');
    
      var $list = $('<ul>').addClass('launch-wizard-type-size clearfix'); 
      var $legend = $('<div>').attr('id','launch-wizard-type-size-legend').addClass('wizard-type-size-legend');
      var selectedType = 'm1.small';
      var typeSelected = false;
      var numInstances = 1;
      var instanceChanged = true;
      var selectedZone = 'Any';
      var zoneChanged = false;

      var summarize = function(){
        var zone = selectedZone;
        if(selectedZone === 'Any')
          zone = launch_instance_type_option_az_any;
        else
          zone = selectedZone;
        var $summary = $('<div>').addClass(selectedType).addClass('summary').append(
          $('<div>').attr('id','summary-type-insttype').append($('<div>').text(launch_instance_summary_type), $('<span>').text(selectedType)),
          $('<div>').attr('id','summary-type-numinst').append($('<div>').text(launch_instance_summary_instances), $('<span>').text(numInstances)),
          $('<div>').attr('id','summary-type-zone').append($('<div>').text(launch_instance_summary_zone), $('<span>').text(zone)));
        if(!typeSelected)
          $summary.find('#summary-type-insttype').hide();
        if(!instanceChanged)
          $summary.find('#summary-type-numinst').hide();
        if(!zoneChanged)
          $summary.find('#summary-type-zone').hide();

        thisObj.launchParam['type'] = selectedType;
        thisObj.launchParam['number'] = numInstances;
        thisObj.launchParam['zone'] = selectedZone;
        return $summary;
      }

      var instType ={};
      instType['m1.small'] = $.eucaData.g_session['instance_type']['m1.small'];
      instType['c1.medium'] = $.eucaData.g_session['instance_type']['c1.medium'];
      instType['m1.large'] = $.eucaData.g_session['instance_type']['m1.large'];
      instType['m1.xlarge'] = $.eucaData.g_session['instance_type']['m1.xlarge'];
      instType['c1.xlarge'] = $.eucaData.g_session['instance_type']['c1.xlarge'];

      $.each(instType, function(type, size){
        $list.append(
          $('<li>').addClass('instance-type-'+type.replace('.','_')).append(
            $('<a>').attr('href','#').text(type).click( function(){
              selectedType = type;
              typeSelected = true;
              var legend = type + '&nbsp;' + launch_wizard_type_description_default + '&nbsp;' + size[0] + '&nbsp;' + launch_wizard_type_description_cpus + ',&nbsp;' + size[1] + '&nbsp;' + launch_wizard_type_description_memory + ',&nbsp;' +size[2] + '&nbsp;' + launch_wizard_type_description_disk;  
              $size.find('#launch-wizard-type-size-legend').html(legend); 
              $(this).parent().addClass('selected-type');
              $(this).parent().siblings().removeClass('selected-type');
              thisObj._setSummary('type', summarize());
            })));
      });

      $size.append($('<div>').addClass('wizard-section-label').html(launch_instance_type_size_header),
                   $('<div>').addClass('wizard-section-content').append($list),
                   $legend); 

      var $list = $('<div>').addClass('launch-wizard-type-option'); 
      $list.append(
        $('<div>').addClass('form-row').addClass('clearfix').attr('id','launch-wizard-type-option-num-instance').append(
          $('<label>').attr('for','launch-instance-type-num-instance').text(launch_instance_type_option_numinstance),
          $('<input>').attr('title', launch_instance_type_num_instance_tip).attr('id','launch-instance-type-num-instance').attr('type','text').attr('class', 'short-textinput').change( function(e) {
            numInstances = asText($(this).val()); 
            if(numInstances<=0 || isNaN(numInstances)){
              $(this).val(1);
              return;
            }
            instanceChanged = true;
            thisObj._setSummary('type', summarize());
         })));
      $list.append(
        $('<div>').addClass('form-row').addClass('clearfix').attr('id','launch-wizard-type-option-az').append(
          $('<label>').attr('for','launch-instance-type-az').text(launch_instance_type_option_az),
          $('<select>').attr('title', launch_instance_az_select_tip).attr('id','launch-instance-type-az')));

      $list.find('#launch-instance-type-num-instance').val('1');
      var $az = $list.find('#launch-instance-type-az');
      $az.append($('<option>').attr('value', 'Any').text(launch_instance_type_option_az_any));
      $az.change(function(e){
        selectedZone = $(this).val();
        zoneChanged = true;
        thisObj._setSummary('type', summarize());
      });
      var results = describe('zone');
      for( res in results) {
        var azName = results[res].name;
        $az.append($('<option>').attr('value', azName).text(azName));
      }

      $option.append($('<div>').addClass('wizard-section-label').html(launch_instance_type_option_header),
                     $list);

      $section.find('#launch-wizard-buttons-type-next').click(function(e) {
        selectedZone = $az.val();
      });

      if(thisObj.options.type){
        var instType = thisObj.options.type['instance_type'];
        var numInst = 1; // default to 1
        var zone = thisObj.options.type['zone'];
        $section.find('#launch-wizard-type-size').find('ul li a').each(function(){
          if($(this).text() === instType){
            $(this).trigger('click');
          }
        });
        $list.find('#launch-instance-type-num-instance').val(numInst);
        $az.val(zone);
        setTimeout(function(){ $section.find('#launch-wizard-buttons-type-next').trigger('click')}, 100); // click event should be delayed
      }

      $section.find('#launch-wizard-buttons-type-next').click( function(e){
        thisObj._setSummary('type', summarize());
      });
    },
    _initTypeSection : function(){
      var thisObj = this;
      if(! thisObj.options.type){
        this.element.find('#launch-wizard-type').find('ul.launch-wizard-type-size li a').first().trigger('click');
        this.element.find('#launch-wizard-type').find('input#launch-instance-type-num-instance').val('1').trigger('change');
        this.element.find('#launch-wizard-type').find('select#launch-instance-type-az').trigger('change');
      }
      thisObj.element.find('#launch-wizard-image-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-type-header').addClass('expanded');
      thisObj.element.find('#launch-wizard-security-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-advanced-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-type-contents').find('input').first().focus();
      thisObj._enableTypeLink();
    },

    _makeSecuritySection : function($section) {
      var thisObj = this;
      var $content = $section.find('#launch-wizard-security-main-contents');
      $content.prepend($('<div>').addClass('wizard-section-label').html(launch_instance_security_header));
      var $keypair = $content.find('#launch-wizard-security-keypair');
      var $sgroup = $content.find('#launch-wizard-security-sgroup');
      
      var summarize = function(kp, sg){
        var selectedKp = kp? kp : $section.find('select#launch-wizard-security-keypair-selector').val();
        var selectedSg = sg? sg : $section.find('select#launch-wizard-security-sg-selector').val();
        thisObj.launchParam['keypair'] = selectedKp;
        thisObj.launchParam['sgroup'] = selectedSg;
        return $('<div>').addClass('summary').append(
          $('<div>').attr('id','summary-security-keypair').append(
            $('<div>').text(launch_instance_summary_keypair),
            $('<span>').attr('title', selectedKp).text(addEllipsis(selectedKp, 15))),
          $('<div>').attr('id','summary-security-sg').append(
            $('<div>').text(launch_instance_summary_sg),
            $('<span>').attr('title', selectedSg).text(addEllipsis(selectedSg, 15))));
      }
      var populateKeypair = function(oldKeypairs){ // select the new keypair if not found in the list of old ones
        var $kp_selector = $keypair.find('select');
        var results = describe('keypair');
        if(!results)
          return;
        var numOptions = $kp_selector.find('option').length - 1; // none is always appended
        if (numOptions === results.length)
          return;
        $kp_selector.children().detach();
        var keyNameArr = [];
        for( res in results ) {
          var kpName = results[res].name;
          keyNameArr.push(kpName);
        }
        var sortedArr = sortArray(keyNameArr);
        var selectedKeyPair = '';
        $.each(sortedArr, function(idx, kpName){
          var $option = $('<option>').attr('value', kpName).attr('title', kpName).text(addEllipsis(kpName, 70));
          if(oldKeypairs && $.inArray(kpName, oldKeypairs) < 0){
            $option.attr('selected', 'selected');
            selectedKeyPair = kpName;
          }
          $kp_selector.append($option);
        });

        thisObj._setSummary('security', summarize(selectedKeyPair, undefined));
        $kp_selector.append($('<option>').attr('value', 'none').text(launch_instance_security_keypair_none));
        $kp_selector.change(function(e){
          var $summary = summarize(); 
          thisObj._setSummary('security', $summary.clone());
        });
      }

      var populateSGroup = function(oldGroups){
        var $sg_selector = $sgroup.find('select');
        var results = describe('sgroup');
        var numOptions = $sg_selector.find('option').length;
        if (numOptions === results.length)
          return;
        var onSelectorChange = function(groupName){
          var $rule = $section.find('div#launch-wizard-security-sg-detail');
          $rule.children().detach();
          $rule.append(
            $('<div>').addClass('launcher-sgroup-details-label').html($.i18n.prop('launch_instance_security_group_rule',groupName)));
          var results = describe('sgroup');
          var group = null;
          for(i in results){
            if(results[i].name === groupName){
              group = results[i];
              break; 
            }
          }
          if(!group.rules || group.rules.length <=0){
            $rule.children().detach();
            $rule.append(
              $('<div>').html(launch_instance_security_group_norule));
          }else{
            $.each(group.rules, function (idx, rule){
              var $wrapper = $('<div>').addClass('launcher-sgroup-rule clearfix');
              var protocol = rule['ip_protocol'];
              var port = rule['from_port'];
              if(rule['to_port'] !== rule['from_port'])
                port += '-'+rule['to_port']; 
              var type = '';
              if(protocol === 'icmp'){
              // TODO : define icmp type
                ;
              }
              var portOrType = type ? type: port;
              var src = [];
              var grants = rule['grants'];
              $.each(grants, function(idx, grant){
                if(grant.cidr_ip && grant.cidr_ip.length>0){
                  src.push(grant.cidr_ip);
                }else if(grant.owner_id && grant.owner_id.length>0){
                  if(group.owner_id === grant.owner_id)
                    src.push(grant.groupName);
                  else
                    src.push(grant.owner_id+'/'+grant.groupName);
                }
              });
              src = src.join(', '); 
 
              src = $("<div/>").html(src).text();
              $wrapper.append( $('<div>').addClass('launcher-sgroup-rule-label').html(launch_instance_security_rule + ':'),
                                 $('<ul>').append(
                                   $('<li>').text(protocol),
                                   $('<li>').text(portOrType),
                                   $('<li>').text(src)));
              $rule.append($wrapper);
            });
          } 
          var $summary = summarize(undefined, groupName);
          thisObj._setSummary('security', $summary.clone());
        } //end of onSelectorChange

        $sg_selector.children().detach();
        var sgNameArr = [];
        for(res in results){
          var sgName = results[res].name;
          sgNameArr.push(sgName);
        }
        var sortedArr = sortArray(sgNameArr);
        $.each(sortedArr, function(idx, sgName){
          var $option = $('<option>').attr('value',sgName).attr('title',sgName).text(addEllipsis(sgName, 70));
          if(oldGroups && $.inArray(sgName, oldGroups) < 0){
            $option.attr('selected','selected');
            onSelectorChange(sgName);
          }
          $sg_selector.append($option);
        });
        if(! oldGroups){
          $sg_selector.find('option').each(function(){
            if($(this).val() ==='default')
              $(this).attr('selected','selected');
          });
        }
        $sg_selector.change(function(e){
          var groupName = $sg_selector.val();
          onSelectorChange(groupName);
        });
      }

      $keypair.append(
        $('<div>').addClass('form-row clearfix upper-sibling').append(
          $('<label>').attr('for','launch-wizard-security-keypair-selector').text(launch_instance_security_keypair),
          $('<select>').attr('title', launch_instance_key_select_tip).attr('id','launch-wizard-security-keypair-selector')),
        $('<div>').addClass('form-row clearfix lower-sibling').append(
          $('<label>').attr('for','launch-instance-create-keypair-link').text(or_label),
          $('<a>').attr('id','launch-instance-create-keypair-link').attr('href','#').text(launch_instance_security_create_kp).click(function(e){
            if(thisObj._keypairCallback)
              cancelRepeat(thisObj._keypairCallback);
            addKeypair( function(){ 
              var oldKeypairs = [];
              $.each($section.find('#launch-wizard-security-keypair-selector').find('option'), function(){
                oldKeypairs.push($(this).val());
              });

              var numKeypairs = oldKeypairs.length;
              refresh('keypair'); 
              thisObj._keypairCallback = runRepeat(function(){
                populateKeypair(oldKeypairs); 
                if($section.find('#launch-wizard-security-keypair-selector').find('option').length  > numKeypairs){
                  cancelRepeat(thisObj._keypairCallback);
                }
              }, 2000); 
            });
          // TODO: add callback to re-populate the select
        })));

      $sgroup.append(
        $('<div>').addClass('form-row clearfix upper-sibling').append(
          $('<label>').attr('for','launch-wizard-security-sg-selector').text(launch_instance_security_sgroup),
          $('<select>').attr('title', launch_instance_sgroup_select_tip).attr('id','launch-wizard-security-sg-selector')),
        $('<div>').addClass('form-row clearfix lower-sibling').append(
          $('<label>').attr('for','launch-instance-create-sg-link').text(or_label),
          $('<a>').attr('id', 'launch-instance-create-sg-link').attr('href','#').text(launch_instance_security_create_sg).click(function(e){
            if(thisObj._sgCallback)
              cancelRepeat(thisObj._sgCallback);
            addGroup( function() {
              var oldGroups = [];
              $.each($section.find('#launch-wizard-security-sg-selector').find('option'), function(){
                oldGroups.push($(this).val());
              });
              var numGroups = oldGroups.length;
              refresh('sgroup');
              thisObj._sgCallback = runRepeat(function(){ 
                populateSGroup(oldGroups);
                if($section.find('#launch-wizard-security-sg-selector').find('option').length > numGroups){
                  cancelRepeat(thisObj._sgCallback);
                }  
             }, 2000);
            });
          })),
        $('<div>').attr('id','launch-wizard-security-sg-detail'));
     
      populateKeypair();
      populateSGroup();

      $section.find('#launch-wizard-buttons-security').find('a#launch-wizard-buttons-security-advanced-link').click(function(e) { // proceed to advanced
        var $summary = summarize(); 
        thisObj._setSummary('security', $summary.clone());
      });
      $section.find('#launch-wizard-buttons-security').find('button#launch-wizard-buttons-security-launch').click(function(e){
        var $summary = summarize(); 
        thisObj._setSummary('security', $summary.clone());
        thisObj.launch();  /// launch button
      });

      if(thisObj.options['security']){
        setTimeout(function(){
          var keypair = thisObj.options['security']['keypair'];
          var sgroup = thisObj.options['security']['sgroup'];
          $section.find('#launch-wizard-security-keypair-selector').val(keypair);
          $section.find('#launch-wizard-security-sg-selector').val(sgroup);
          $section.find('#launch-wizard-buttons-security').find('a#launch-wizard-buttons-security-advanced-link').trigger('click');
        }, 200);
      }
    },
    _initSecuritySection : function(){
      var thisObj = this;
      thisObj._enableSecurityLink();
      if(!thisObj.options.security){
        thisObj.element.find('#launch-wizard-security').find('select#launch-wizard-security-keypair-selector').trigger('change');
        thisObj.element.find('#launch-wizard-security').find('select#launch-wizard-security-sg-selector').trigger('change');
      }
      thisObj.element.find('#launch-wizard-image-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-type-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-security-header').addClass('expanded');
      thisObj.element.find('#launch-wizard-advanced-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-security-contents').find('select').first().focus();
    },

    makeAdvancedSection : function($section) { 
      var thisObj = this;
      var $content = $section.find('#launch-wizard-advanced-main-contents');
      if(! $content.children().first().is('span'))
        $content.prepend($('<div>').addClass('wizard-section-label').html(launch_instance_advanced_header));
      var $userdata = $content.find('#launch-wizard-advanced-userdata');
      var $kernel = $content.find('#launch-wizard-advanced-kernelramdisk');
      var $network = $content.find('#launch-wizard-advanced-network');
      var $storage = $content.find('#launch-wizard-advanced-storage');

      $userdata.append(
        $('<div>').addClass('form-row clearfix upper-sibling').append(
          $('<label>').attr('for','launch-wizard-advanced-input-userdata').text(launch_instance_advanced_userdata),
          $('<textarea>').attr('id','launch-wizard-advanced-input-userdata').attr('title',launch_wizard_advanced_input_userdata_tip).addClass('description').change(function(e){
            var $summary = summarize(); 
            thisObj._setSummary('advanced', $summary.clone());
          })),
        $('<div>').addClass('form-row clearfix lower-sibling').append(
          $('<label>').attr('for','launch-wizard-advanced-input-userfile').text(or_label),
          $('<input>').attr('id','launch-wizard-advanced-input-userfile').attr('type','file')));
      var $input = $userdata.find('#launch-wizard-advanced-input-userfile');
      $input.change(function(e){
          thisObj.launchParam['data_file'] = this.files;
      });
 
      $kernel.append(
        $('<div>').addClass('form-row').addClass('clearfix').attr('id', 'launch-wizard-advanced-kernel').append(
          $('<label>').attr('for','launch-wizard-advanced-kernel-selector').text(launch_instance_advanced_kernel),
          $('<select>').attr('title', launch_instance_kernel_select_tip).attr('id', 'launch-wizard-advanced-kernel-selector').change(function(e){
            var $summary = summarize(); 
            thisObj._setSummary('advanced', $summary.clone());
          }).append($('<option>').val('default').text(launch_instance_advanced_usedefault))),
        $('<div>').addClass('form-row').addClass('clearfix').attr('id', 'launch-wizard-advanced-ramdisk').append(
          $('<label>').attr('for','launch-wizard-advanced-ramdisk-selector').text(launch_instance_advanced_ramdisk),
          $('<select>').attr('title', launch_instance_ramdisk_select_tip).attr('id', 'launch-wizard-advanced-ramdisk-selector').change(function(e){
            var $summary = summarize(); 
            thisObj._setSummary('advanced', $summary.clone());
          }).append($('<option>').val('default').text(launch_instance_advanced_usedefault))));

      var result = describe('image');
      var $kernelSelector = $kernel.find('#launch-wizard-advanced-kernel-selector');
      var $ramdiskSelector = $kernel.find('#launch-wizard-advanced-ramdisk-selector');
      $.each(result, function(idx, image){
        if(image.type === 'kernel'){
          $kernelSelector.append(
            $('<option>').val(image.id).text(image.id)); 
        }else if(image.type === 'ramdisk'){
          $ramdiskSelector.append(
            $('<option>').val(image.id).text(image.id));
        } 
      });

      $network.addClass('form-row clearfix orphan-row').addClass('clearfix').append(
        $('<label>').attr('for','launch-wizard-advanced-input-network').text(launch_instance_advanced_network),
        $('<input>').attr('title', launch_wizard_advanced_input_network_tip).attr('id','launch-wizard-advanced-input-network').attr('type','checkbox').change(function(e){
          var $summary = summarize(); 
          thisObj._setSummary('advanced', $summary.clone());
        }),
        $('<label>').addClass('plaintext-label').attr('for', 'launch-wizard-advanced-input-network').text(launch_instance_advanced_network_desc));
      
      var usedSnapshots = []; 
      var usedVolumes = [];
      var usedMappings = [];

      var validate = function($selectedRow, emi){
        if(! $selectedRow || $selectedRow.length <= 0)
          return true;

        var $cells = $selectedRow.find('td');
        var volume = $($cells[0]).find('select').val();
        var mapping = '/dev/'+asText($($cells[1]).find('input').val());
        var snapshot = $($cells[2]).find('select').val();
        var size = asText($($cells[3]).find('input').val()); 
        var delOnTerm = $($cells[4]).find('input').is(':checked') ? true : false;

        //validate the input 
        // TODO: create and use generic error dialog
          
        if($.inArray(mapping, usedMappings) >= 0){
          thisObj.element.find('.field-error').remove();
          $($cells[1]).append($('<div>').addClass('field-error').html(launch_instance_advanced_error_dev_dup));
          return false;
        }
        if(mapping.replace('/dev/','').length <= 0){
          thisObj.element.find('.field-error').remove();
          $($cells[1]).append($('<div>').addClass('field-error').html(launch_instance_advanced_error_dev_none));
          return false;
        }
        if(size.length <= 0 || (volume === 'ebs' && (parseInt(size)<=0 || isNaN(size)))){
          thisObj.element.find('.field-error').remove();
          $($cells[3]).append($('<div>').addClass('field-error').html(launch_instance_advanced_error_dev_size_none));
          return false;
        }
        if(volume === 'ebs' || volume === 'root'){
          var snapshotSize = -1;
          if(volume==='ebs'){
          //find the size of the chosen snapshot;
            var result = describe('snapshot');
            for (i in result){
              var s = result[i]; 
              if(s.id === snapshot){
                snapshotSize = s.volume_size;
                break;
              }
            }
          }else if (emi){ //root volume
            var image = describe('image', emi);
            if(image['block_device_mapping'] && image['block_device_mapping']['/dev/sda1']) 
             snapshotSize = parseInt(image['block_device_mapping']['/dev/sda1']['size']);
          }
          if(snapshotSize > size){
            thisObj.element.find('.field-error').remove();
            $($cells[3]).append($('<div>').addClass('field-error').html(launch_instance_advanced_error_dev_size));
            return false;
          }
        } 
        thisObj.element.find('.field-error').remove();
        return true;
      }

      var addNewRow = function(param) {
        var results = describe('snapshot');
        var $snapshots = $('<div>');
        $.each(results, function(idx, snapshot){
          if(snapshot['status'] === 'completed' && $.inArray(snapshot['id'], usedSnapshots) < 0)
            $snapshots.append($('<option>').attr('value',snapshot['id']).text(snapshot['id']));
        }); 
        var $volume = $('<select>').attr('title', launch_instance_volume_select_tip).attr('class','launch-wizard-advanced-storage-volume-selector');
        if(param && param['volume']){
          $volume.append($('<option>').attr('value', param['volume'][0]).text(param['volume'][1]));
          $volume.attr('disabled','disabled');
        }
        else{
          // exclude used volumes
          if($.inArray('root',usedVolumes) < 0)
            $volume.append($('<option>').attr('value', 'root').text(launch_instance_root));
          $volume.append($('<option>').attr('value', 'ebs').text(launch_instance_ebs)); // multiple ebs allowed
          if($.inArray('ephemeral0',usedVolumes) < 0)
            $volume.append($('<option>').attr('value', 'ephemeral0').text(launch_instance_ephemeral0));
          if($.inArray('ephemeral1',usedVolumes) < 0)
            $volume.append($('<option>').attr('value', 'ephemeral1').text(launch_instance_ephemeral1));
          if($.inArray('ephemeral2',usedVolumes) < 0)
            $volume.append($('<option>').attr('value', 'ephemeral2').text(launch_instance_ephemeral2));
          if($.inArray('ephemeral3',usedVolumes) < 0)
            $volume.append($('<option>').attr('value', 'ephemeral3').text(launch_instance_ephemeral3));

          // change event should enable/disable following cells
        }
        $volume.change(function(e){
          vol = $volume.val();
          $row = $(e.target).parents('tr');
          if (vol === 'ebs'){
             $row.find('.launch-wizard-advanced-storage-delOnTerm').attr('checked','true');
             $row.find('.launch-wizard-advanced-storage-snapshot-input').removeAttr('disabled');
             $row.find('.launch-wizard-advanced-storage-size-input').removeAttr('disabled');
             $row.find('.launch-wizard-advanced-storage-delOnTerm').removeAttr('disabled');
          }else if (vol.indexOf('ephemeral')>=0){
             $row.find('.launch-wizard-advanced-storage-snapshot-input').attr('disabled','disabled').val('none');
             $row.find('.launch-wizard-advanced-storage-size-input').attr('disabled','disabled').val('-1');
             $row.find('.launch-wizard-advanced-storage-delOnTerm').attr('disabled','disabled').removeAttr('checked');
          }
        });
        
        var $mapping = $('<input>').attr('title', launch_wizard_advanced_storage_mapping_input_tip).attr('class','launch-wizard-advanced-storage-mapping-input').attr('type','text'); // mapping
        if(param && param['mapping']){
          $mapping.val(param['mapping']);
          $mapping.attr('disabled','disabled');
        } 
        var $snapshot = $('<select>').attr('title',launch_instance_snapshot_select_tip).attr('class', 'launch-wizard-advanced-storage-snapshot-input');
        if(param && param['snapshot']){
          $snapshot.append($('<option>').attr('value',param['snapshot'][0]).text(param['snapshot'][1]));
          $snapshot.attr('disabled','disabled');
        }
        else
          $snapshot.append(
            $('<option>').attr('value','none').text(launch_instance_advanced_snapshot_none), $snapshots.html());
        $snapshot.change(function(e){
           var result = describe('snapshot');
           var size = 0;
           for (i in result){
             var s = result[i];
             if(s.id === $snapshot.val()){
               size = s.volume_size;
               break;
             }
           }
           var $row = $(e.target).parents('tr');
           $row.find('.launch-wizard-advanced-storage-size-input').val(size);
        });
        var $size = $('<input>').attr('title', launch_wizard_advanced_storage_size_input_tip).attr('class','launch-wizard-advanced-storage-size-input').attr('type','text');
        $size.change(function(e){  
          $(e.target).parent().find('.field-error').detach();
          validate($(e.target).parents('tr'), param['emi'] );
        });
        if(param && param['size']){
          $size.val(param['size']);
          if(param['size'] <= 0){
            $size.attr('disabled','disabled');
          }
        }
    
        var $delOnTerm= $('<input>').attr('class','launch-wizard-advanced-storage-delOnTerm').attr('type','checkbox');
        if(param && param['delOnTerm'] !== undefined){
          if(param['delOnTerm'])
            $delOnTerm.attr('checked','true'); 
        }else{
          if ($volume.val() === 'ebs')
            $delOnTerm.attr('checked','true'); 
        }

        var $addBtn = $('<input>').attr('title',launch_wizard_advanced_storage_add_input_tip).attr('class','launch-wizard-advanced-storage-add-input').attr('type','button').val(launch_instance_advanced_btn_add);
        if(param && param['add'] !== undefined){
          if(!param['add']) 
            $addBtn.attr('disabled','disabled');
        }

        $addBtn.click(function(e) {
          var $selectedRow = $(e.target).parents('tr');
          var $cells = $selectedRow.find('td');
          var volume = $($cells[0]).find('select').val();
          var mapping = '/dev/'+asText($($cells[1]).find('input').val());
          var snapshot = $($cells[2]).find('select').val();
          var size = asText($($cells[3]).find('input').val()); 
          var delOnTerm = $($cells[4]).find('input').is(':checked') ? true : false;

          if(!validate($selectedRow, param['emi']))
            return false;
          usedMappings.push(mapping);
          usedVolumes.push(volume);
          usedSnapshots.push(snapshot);
          $selectedRow.find('.launch-wizard-advanced-storage-add-input').hide();
          $selectedRow.find('.launch-wizard-advanced-storage-remove-input').hide();
          $selectedRow.find('input').each(function(){
            if($(this).attr('type') !== 'button')
              $(this).attr('disabled','disabled')
          });
          $selectedRow.find('select').attr('disabled','disabled');
          var $tbody = $storage.find('table tbody');
          var $tr = addNewRow();
          $tbody.append($tr);
          var $summary = summarize(); 
          thisObj._setSummary('advanced', $summary.clone());
        });

        var $removeBtn = $('<input>').attr('title', launch_wizard_advanced_storage_remove_input_tip).attr('class', 'launch-wizard-advanced-storage-remove-input').attr('type','button').val(launch_instance_advanced_btn_remove);
        if(param && param['remove'] !== undefined){
          if(!param['remove'])
            $removeBtn.attr('disabled','disabled');
        }
        $removeBtn.click(function(e) {
          var $selectedRow = $(e.target).parents('tr');
          var mapping = $selectedRow.find('.launch-wizard-advanced-storage-mapping-input').val();
          var volume = $selectedRow.find('.launch-wizard-advanced-storage-volume-selector').val();
          var snapshot = $selectedRow.find('.launch-wizard-advanced-storage-snapshot-input').val();
          usedMappings.splice(usedMappings.indexOf(mapping), 1);
          usedVolumes.splice(usedVolumes.indexOf(volume), 1);
          usedSnapshots.splice(usedSnapshots.indexOf(snapshot),1);
          $selectedRow.remove();
          var $rows =  $storage.find('table tbody tr');
          $rows.last().find('.launch-wizard-advanced-storage-add-input').show(); 
          if($rows.length > 1) 
            $rows.last().find('.launch-wizard-advanced-storage-remove-input').show();  
          var $summary = summarize(); 
          thisObj._setSummary('advanced', $summary.clone()); 
        });
        var $tr = $('<tr>').append(
          $('<td>').append($volume),
          $('<td>').text('/dev/').append($mapping),
          $('<td>').append($snapshot),
          $('<td>').append($size), 
          $('<td>').addClass('centered-cell').append($delOnTerm)
          /*,
          $('<td>').append($addBtn),
          $('<td>').append($removeBtn) */
         );

        return $tr;
      } /// end of addNewRow()

      $section.find('#launch-wizard-image-emi').change(function(e){
        var isEbsBacked = false;
        var snapshotId= null;
        var emiId = $(e.target).val();
        var snapshotSize = '-1';
        var emi = null;
        if(emiId){
          emi = describe('image', emiId);
          if (emi && emi['root_device_type'] === 'ebs'){
            isEbsBacked = true;
            if(emi['block_device_mapping']&&emi['block_device_mapping']['/dev/sda1']){
              snapshotId = emi['block_device_mapping']['/dev/sda1']['snapshot_id'];
              snapshotSize = emi['block_device_mapping']['/dev/sda1']['size'];
            }
          }
        }
        if(isEbsBacked && snapshotId){
          $storage.children().detach();
          $storage.append(
            $('<div>').addClass('wizard-section-label').text(launch_instance_advanced_storage),
            $('<div>').attr('id', 'launch-wizard-advanced-storage-table-wrapper').append(
              $('<table>').addClass('device-map-table').append(
                $('<thead>').append(
                  $('<tr>').append(
                    $('<th>').text(launch_instance_advanced_th_volume),
                    $('<th>').text(launch_instance_advanced_th_mapping),
                    $('<th>').text(launch_instance_advanced_th_create),
                    $('<th>').text(launch_instance_advanced_th_size),
                    $('<th>').text(launch_instance_advanced_th_delete)/*,
                    $('<th>').text(''),
                    $('<th>').text('') */
            )
            ), $('<tbody>'))));
          var $tbody = $storage.find('table tbody');
          var $tr = null;
          $tr = addNewRow({
            'emi' : emiId,
            'volume' : ['root', 'Root'],
            'mapping' : 'sda1',
            'snapshot' : [snapshotId, snapshotId],
            'size' : snapshotSize, 
            'delOnTerm' : true,
            'add' : true,
            'remove' : false
          });
          $tbody.find('tr').detach();
          $tbody.append($tr);
        }else{
          $storage.children().detach();// don't display storage option  
          // this must be changed when we fully support the device mapping
        }
      });

      var summarize = function(){
        var dataAdded = false;
        var data = $section.find('#launch-wizard-advanced-input-userdata');
        if(data.val().length > 0){
          dataAdded = true;
          thisObj.launchParam['data'] = toBase64(asText(data.val()));
        }
        if($userdata.find('#launch-wizard-advanced-input-userfile').val().length > 0){
          dataAdded = true;
        }
        var kernelChanged = false; 
        var kernel =  $section.find('#launch-wizard-advanced-kernel-selector');
        if(kernel.val() !== 'default'){
          kernelChanged = true;
          thisObj.launchParam['kernel'] = kernel.val();
        }
        var ramdiskChanged = false;
        var ramdisk = $section.find('#launch-wizard-advanced-ramdisk-selector');
        if(ramdisk.val() !== 'default'){
          ramdiskChanged = true;       
          thisObj.launchParam['ramdisk'] = ramdisk.val();
        }
        var privateAddressing = false;
        var network = $section.find('#launch-wizard-advanced-input-network');
        if(network.attr('checked')){
          privateAddressing = true;
          thisObj.launchParam['private_addressing'] = true;
        }
        var storageConfigured = false; // always configured
        if($section.find('#launch-wizard-advanced-storage').find('table tbody tr').length > 1)
          storageConfigured = true; 

        thisObj.launchParam['device_map'] = [];       
        $section.find('#launch-wizard-advanced-storage').find('table tbody tr').each(function(){
          var $selectedRow = $(this);
          var $cells = $selectedRow.find('td');
          var volume = $($cells[0]).find('select').val();
          var mapping = '/dev/'+asText($($cells[1]).find('input').val());
          var snapshot = $($cells[2]).find('select').val();
          var size = asText($($cells[3]).find('input').val()); 
          var delOnTerm = $($cells[4]).find('input').is(':checked') ? true : false;
        
          snapshot = (snapshot ==='none') ? null : snapshot; 
          if(volume ==='ebs'){
            var mapping = {
              'volume':'ebs',
              'dev':mapping,
              'snapshot':snapshot, 
              'size':size,
              'delOnTerm':delOnTerm
            };
            thisObj.launchParam['device_map'].push(mapping);
          }else if(volume.indexOf('ephemeral')>=0){
            var mapping = {
              'volume':volume,
              'dev':mapping
            }
            thisObj.launchParam['device_map'].push(mapping);
          }else if(snapshot !== 'none') { // root volume
            var mapping = {
              'volume':'ebs',
              'dev': mapping,
              'snapshot':snapshot,
              'size':size,
              'delOnTerm':delOnTerm
            }
            thisObj.launchParam['device_map'].push(mapping);
          }
        });
 
        var $wrapper = $('<div>').addClass('summary').append(
          $('<div>').text(launch_instance_summary_advanced),
          $('<ul>'));
        var $ul = $wrapper.find('ul');
        if(dataAdded)
          $ul.append(
            $('<li>').append($('<span>').attr('id','summary-advanced-userdata').text(launch_instance_summary_advanced_userdata)));
        if(kernelChanged)
          $ul.append(
            $('<li>').append($('<span>').attr('id','summary-advanced-kernel').text(launch_instance_summary_advanced_kernel)));
        if(ramdiskChanged)
          $ul.append(
            $('<li>').append($('<span>').attr('id','summary-advanced-ramdisk').text(launch_instance_summary_advanced_ramdisk)));
        if(privateAddressing)
          $ul.append(
            $('<li>').append($('<span>').attr('id','summary-advanced-addressing').text(launch_instance_summary_advanced_addressing)));
        if(storageConfigured)
          $ul.append(
            $('<li>').append($('<span>').attr('id','summary-advanced-storage').text(launch_instance_summary_advanced_storage)));
        return $wrapper;
      }

      $section.find('#launch-wizard-buttons-advanced').find('button').click(function(e){// launch button
        var $selectedRow = $section.find('#launch-wizard-advanced-storage').find('table tbody tr').last(); 
        if(!validate($selectedRow, thisObj.launchParam['emi']))
          return false;

        var $summary = summarize(); 
        thisObj._setSummary('advanced', $summary.clone()); 
        thisObj.launch();
        // and launch
      });

      if(thisObj.options['advanced']){
        var kernel = thisObj.options['advanced']['kernel'];
        var ramdisk = thisObj.options['advanced']['ramdisk'];
        $section.find('#launch-wizard-advanced-kernel-selector').val(kernel);
        $section.find('#launch-wizard-advanced-ramdisk-selector').val(ramdisk);
        // setTimeout ...
      }
    },

    _initAdvancedSection : function(){
      var thisObj = this;
      thisObj._enableAdvancedLink();
      thisObj.element.find('#launch-wizard-advanced').find('#launch-wizard-advanced-input-userdata').trigger('change');
      thisObj.element.find('#launch-wizard-image-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-type-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-security-header').removeClass('expanded');
      thisObj.element.find('#launch-wizard-advanced-header').addClass('expanded');
      thisObj.element.find('#launch-wizard-advanced-contents').find('textarea').first().focus();

      // make the first row of the storage table depending on the chosen emi
    },
    _setSummary : function(section, content){
      var thisObj = this;
      var $summary = thisObj.element.find('#launch-wizard-summary');
      var $section = $summary.find('#summary-'+section);
      $section.removeClass('required-missing');
      $section.children().detach(); 
      $section.append(content);
    },
    _showError : function(step){
      var thisObj = this;
      var $summary = thisObj.element.find('#launch-wizard-summary');
      var $step = $summary.find('#summary-'+step);
      $step.addClass('required-missing');
      var header = '';
      if(step==='image')
        header = launch_instance_section_header_image+':';
      else if(step==='type')
        header = launch_instance_section_header_type+':';
      else if(step==='security')
        header = launch_instance_section_header_security+':';
      else if(step==='advanced')
        header = launch_instance_section_header_advanced+':';
      $step.find('.required-missing-message').remove();
      $step.append($('<div>').addClass('required-missing-message').append($('<span>').html(header), $('<span>').html(launch_instance_required_missing)));
      return false;
    },
  ///////////// PUBLIC METHODS  /////////////////// 

    updateLaunchParam : function(key, val) {
      var thisObj = this;
      thisObj.launchParam[key] = val;
    },

    launch : function(){
      var thisObj = this;
      var param = thisObj.launchParam;

      if(!param['emi'])
        return thisObj._showError('image');
      if(!param['type'])
        return thisObj._showError('type');
      if(!param['number'])
        return thisObj._showError('type');
      if(!param['zone'])
        return thisObj._showError('type');

      //prepare for the actual request parameters
      var reqParam = new Array();
      reqParam.push({name: 'Action', value: 'RunInstances'});
      reqParam.push({name: 'ImageId', value: param['emi']});
      reqParam.push({name: 'InstanceType', value: param['type']});
      reqParam.push({name: 'MinCount', value: param['number']});
      reqParam.push({name: 'MaxCount', value: param['number']});

      if(param['zone'].toLowerCase() !== 'any')
        reqParam.push({name: 'Placement.AvailabilityZone', value: param['zone']});
      if (param['sgroup'])
        reqParam.push({name: 'SecurityGroup.1', value: param['sgroup']});
      if (param['keypair'] && param['keypair'] !== 'none')
        reqParam.push({name: 'KeyName', value: param['keypair']});

      if(param['kernel'] && param['kernel'].length > 0)
        reqParam.push({name: 'KernelId', value: param['kernel']});
      if(param['ramdisk'] && param['ramdisk'].length > 0)
        reqParam.push({name: 'RamdiskId', value: param['ramdisk']}); 
      if(param['data'] && param['data'].length > 0)
        reqParam.push({name: 'UserData', value: param['data']});
      if(param['private_addressing'])
        reqParam.push({name: 'AddressingType', value: 'private'});
      if(param['device_map'] && param['device_map'].length>0){
        $.each(param['device_map'], function(idx, mapping){
          if(mapping['volume'] === 'ebs'){
           reqParam.push({name: 'BlockDeviceMapping.'+(idx+1)+'.DeviceName', value: mapping['dev']});
           if(mapping['size'] && mapping['size'] > 0)
             reqParam.push({name: 'BlockDeviceMapping.'+(idx+1)+'.Ebs.VolumeSize', value: mapping['size']});
           if(mapping['snapshot'])
             reqParam.push({name: 'BlockDeviceMapping.'+(idx+1)+'.Ebs.SnapshotId', value: mapping['snapshot']});
           reqParam.push({name: 'BlockDeviceMapping.'+(idx+1)+'.Ebs.DeleteOnTermination', value: mapping['delOnTerm'] ? 'true' : 'false'});
          }else if(mapping['volume'].indexOf('ephemeral')>=0){
           reqParam.push({name: 'BlockDeviceMapping.'+(idx+1)+'.DeviceName', value: mapping['dev']});
           reqParam.push({name: 'BlockDeviceMapping.'+(idx+1)+'.VirtualName', value: mapping['volume']});
          }
        });
      }
      reqParam.push({name: '_xsrf', value: $.cookie('_xsrf')});

      this.element.find('#launch-wizard-advanced-input-userfile').fileupload({
        dataType: 'json',
        url: "../ec2",
        fileInput: null,
        formData: reqParam,  
        paramName: 'user_data_file',
      });

      // it turns out that simply passing null for the file causes fileupload to complain. passing a bogus
      // string instead is the ticket.
      var file_param = "none";
      if (param['data_file'] != null) {
        file_param = param['data_file'];
      }
      this.element.find('#launch-wizard-advanced-input-userfile').fileupload('send', {
        files: file_param,
        success: function (result, textStatus, jqXHR) {
          if ( result && result['results'] ){
            var results = result.results;
            var inst_ids = [];
            for (i in results){
              var instance = results[i];
              inst_ids.push(instance.id);
            }
            var instances = inst_ids.join(' ');
            notifySuccess(null, $.i18n.prop('instance_run_success', instances));
            //TODO: move to instance page?
            var $container = $('html body').find(DOM_BINDING['main']);
            $container.maincontainer("clearSelected");
            $container.maincontainer("changeSelected",null, {selected:'instance'});
            $container.instance('glowNewInstance', inst_ids);
          } else {
            notifyError($.i18n.prop('instance_run_error'), undefined_error);
            //TODO: clear launch-instance wizard?
            var $container = $('html body').find(DOM_BINDING['main']);
            $container.maincontainer("clearSelected");
            $container.maincontainer("changeSelected",null, {selected:'launcher'});
          }
        },
        error: function (jqXHR, textStatus, errorthrown) {
          notifyError($.i18n.prop('instance_run_error'), getErrorMessage(jqXHR));
          var $container = $('html body').find(DOM_BINDING['main']);
          $container.maincontainer("clearSelected");
          $container.maincontainer("changeSelected",null, {selected:'launcher'});
        }
      });
    },
 
  });
})(jQuery,
   window.eucalyptus ? window.eucalyptus : window.eucalyptus = {});
