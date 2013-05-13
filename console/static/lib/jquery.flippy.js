/*

 FLIPPY jQuery plugin (http://guilhemmarty.com/flippy)
 Released under MIT Licence (http://www.opensource.org/licenses/MIT)

 @author : Guilhem MARTY (bonjour@guilhemmarty.com)

 @version: 1.1

 @changelog:
 Apr 06 2013 - v1.2 : can now use CSS3 transform property for better visual result in modern web browsers

 Apr 03 2013 - v1.1 : code cleanup (Object Oriented) + add Revert action + add onAnimation callback

 Mar 30 2013 - v1.0.3 : bug fix on IE8/IE9 with explorerCanvas + add multiple simultaneous flippy animations

 Mar 17 2013 - v1.0.2 : bug fix with IE10+. Can use rgba in color and color target

 Feb 11 2012 - v1.0.1 : bug fix with IE9

 Feb 11 2012 - v1.0 : First release

 */
(function($){
	var _ColorsRef = {
		'aliceblue':'#f0f8ff',
		'antiquewhite':'#faebd7',
		'aqua':'#00ffff',
		'aquamarine':'#7fffd4',
		'azure':'#f0ffff',
		'beige':'#f5f5dc',
		'bisque':'#ffe4c4',
		'black':'#000000',
		'blanchedalmond':'#ffebcd',
		'blue':'#0000ff',
		'blueviolet':'#8a2be2',
		'brown':'#a52a2a',
		'burlywood':'#deb887',
		'cadetblue':'#5f9ea0',
		'chartreuse':'#7fff00',
		'chocolate':'#d2691e',
		'coral':'#ff7f50',
		'cornflowerblue':'#6495ed',
		'cornsilk':'#fff8dc',
		'crimson':'#dc143c',
		'cyan':'#00ffff',
		'darkblue':'#00008b',
		'darkcyan':'#008b8b',
		'darkgoldenrod':'#b8860b',
		'darkgray':'#a9a9a9',
		'darkgrey':'#a9a9a9',
		'darkgreen':'#006400',
		'darkkhaki':'#bdb76b',
		'darkmagenta':'#8b008b',
		'darkolivegreen':'#556b2f',
		'darkorange':'#ff8c00',
		'darkorchid':'#9932cc',
		'darkred':'#8b0000',
		'darksalmon':'#e9967a',
		'darkseagreen':'#8fbc8f',
		'darkslateblue':'#483d8b',
		'darkslategray':'#2f4f4f',
		'darkslategrey':'#2f4f4f',
		'darkturquoise':'#00ced1',
		'darkviolet':'#9400d3',
		'deeppink':'#ff1493',
		'deepskyblue':'#00bfff',
		'dimgray':'#696969',
		'dimgrey':'#696969',
		'dodgerblue':'#1e90ff',
		'firebrick':'#b22222',
		'floralwhite':'#fffaf0',
		'forestgreen':'#228b22',
		'fuchsia':'#ff00ff',
		'gainsboro':'#dcdcdc',
		'ghostwhite':'#f8f8ff',
		'gold':'#ffd700',
		'goldenrod':'#daa520',
		'gray':'#808080',
		'grey':'#808080',
		'green':'#008000',
		'greenyellow':'#adff2f',
		'honeydew':'#f0fff0',
		'hotpink':'#ff69b4',
		'indianred ':'#cd5c5c',
		'indigo  ':'#4b0082',
		'ivory':'#fffff0',
		'khaki':'#f0e68c',
		'lavender':'#e6e6fa',
		'lavenderblush':'#fff0f5',
		'lawngreen':'#7cfc00',
		'lemonchiffon':'#fffacd',
		'lightblue':'#add8e6',
		'lightcoral':'#f08080',
		'lightcyan':'#e0ffff',
		'lightgoldenrodyellow':'#fafad2',
		'lightgray':'#d3d3d3',
		'lightgrey':'#d3d3d3',
		'lightgreen':'#90ee90',
		'lightpink':'#ffb6c1',
		'lightsalmon':'#ffa07a',
		'lightseagreen':'#20b2aa',
		'lightskyblue':'#87cefa',
		'lightslategray':'#778899',
		'lightslategrey':'#778899',
		'lightsteelblue':'#b0c4de',
		'lightyellow':'#ffffe0',
		'lime':'#00ff00',
		'limegreen':'#32cd32',
		'linen':'#faf0e6',
		'magenta':'#ff00ff',
		'maroon':'#800000',
		'mediumaquamarine':'#66cdaa',
		'mediumblue':'#0000cd',
		'mediumorchid':'#ba55d3',
		'mediumpurple':'#9370d8',
		'mediumseagreen':'#3cb371',
		'mediumslateblue':'#7b68ee',
		'mediumspringgreen':'#00fa9a',
		'mediumturquoise':'#48d1cc',
		'mediumvioletred':'#c71585',
		'midnightblue':'#191970',
		'mintcream':'#f5fffa',
		'mistyrose':'#ffe4e1',
		'moccasin':'#ffe4b5',
		'navajowhite':'#ffdead',
		'navy':'#000080',
		'oldlace':'#fdf5e6',
		'olive':'#808000',
		'olivedrab':'#6b8e23',
		'orange':'#ffa500',
		'orangered':'#ff4500',
		'orchid':'#da70d6',
		'palegoldenrod':'#eee8aa',
		'palegreen':'#98fb98',
		'paleturquoise':'#afeeee',
		'palevioletred':'#d87093',
		'papayawhip':'#ffefd5',
		'peachpuff':'#ffdab9',
		'peru':'#cd853f',
		'pink':'#ffc0cb',
		'plum':'#dda0dd',
		'powderblue':'#b0e0e6',
		'purple':'#800080',
		'red':'#ff0000',
		'rosybrown':'#bc8f8f',
		'royalblue':'#4169e1',
		'saddlebrown':'#8b4513',
		'salmon':'#fa8072',
		'sandybrown':'#f4a460',
		'seagreen':'#2e8b57',
		'seashell':'#fff5ee',
		'sienna':'#a0522d',
		'silver':'#c0c0c0',
		'skyblue':'#87ceeb',
		'slateblue':'#6a5acd',
		'slategray':'#708090',
		'slategrey':'#708090',
		'snow':'#fffafa',
		'springgreen':'#00ff7f',
		'steelblue':'#4682b4',
		'tan':'#d2b48c',
		'teal':'#008080',
		'thistle':'#d8bfd8',
		'tomato':'#ff6347',
		'turquoise':'#40e0d0',
		'violet':'#ee82ee',
		'wheat':'#f5deb3',
		'white':'#ffffff',
		'whitesmoke':'#f5f5f5',
		'yellow':'#ffff00',
		'yellowgreen':'#9acd32'
	};

	function detect_CSS3Support()
	{
		$("document").ready(function()
		{
			var Fel = document.createElement('p'),
				support_css3,
				transforms = {
					'webkitTransform':'-webkit-transform',
					'OTransform':'-o-transform',
					'msTransform':'-ms-transform',
					'MozTransform':'-moz-transform',
					'transform':'transform'
				};
			document.body.appendChild(Fel);

			for(var t in transforms){
				if( Fel.style[t] !== undefined ){
					Fel.style[t] = 'rotateX(1deg)';
					support_css3 = window.getComputedStyle(Fel).getPropertyValue(transforms[t]);
				}
			}

			document.body.removeChild(Fel);

			_Support_CSS3 = (support_css3 !== undefined && support_css3.length > 0 && support_css3 !== "none");
		});
	}

	var _isIE = (navigator.appName == "Microsoft Internet Explorer");
	var _Support_Canvas = window.HTMLCanvasElement;
	var _Support_CSS3 = null;
	detect_CSS3Support();
	var PI = Math.PI;

	//! Class flipBox
	var flipBox = function($jO,opts, undefined)
	{
		//! public methods

		/**
		 * Animate the FlipBox
		 * @param reversing : boolean [false]
		 * @return void
		 */
		this.animate = function(reversing)
		{
			this._Before();

			if(typeof reversing !== undefined && reversing){
				var recto = this._Recto;
				var recto_color = this._Recto_color;

				this._Recto = this._Verso;
				this._Color = this._Recto_color = this._Verso_color;

				this._Verso = recto;
				this._Color_target = this._Verso_color = recto_color;

				switch(this._Direction){
					case "TOP": this._Direction = "BOTTOM"; break;
					case "BOTTOM": this._Direction = "TOP"; break;
					case "LEFT": this._Direction = "RIGHT"; break;
					case "RIGHT": this._Direction = "LEFT"; break;
				}
			}

			if(this._noCSS || !_Support_CSS3){
				//! run canvas animation
				this.initiateFlippy();
				this.cvO = document.getElementById("flippy"+this._UID);
				this.jO.data("_oFlippy_",this);
				this._Int = setInterval($.proxy(this.drawFlippy, this), this._Refresh_rate);
			}else{
				//! run CSS3 animation
				this.jO
					.addClass('flippy_active')
					.parent()
					.css({
						"perspective": Math.floor(this._Depth * this._nW) +"px"
					});
				this.jO.data("_oFlippy_",this);
				this._Int = setInterval($.proxy(this.drawFlippyCSS, this), this._Refresh_rate);
			}

		};

		/**
		 * Refresh CSS3 fliped element
		 * @return void
		 */
		this.drawFlippyCSS = function()
		{
			this._Ang = (this._Direction == "RIGHT" || this._Direction == "TOP") ? this._Ang + this._Step_ang : this._Ang - this._Step_ang;
			var _Axis = (this._Direction == "RIGHT" || this._Direction == "LEFT") ? "Y" : "X" ;

			if(
				( (this._Direction == "RIGHT" || this._Direction == "TOP") && this._Ang > 90 && this._Ang <= (90+this._Step_ang)) ||
					( (this._Direction == "LEFT" || this._Direction == "BOTTOM") && this._Ang < -90 && this._Ang >= (-90-this._Step_ang))
				){
				this._Midway();
                /*
				this.jO
					.css({
						"opacity":this._Color_target_alpha,
						"background":this._Color_target
					})
					.empty()
					.append(this._Verso);
                 */

				this.jO
					.css({
						"opacity":this._Color_target_alpha,
						"background":this._Color_target
					});
                this.jO.children().detach();
                this.jO.append(this._Verso);

				this._Ang = (this._Direction == "RIGHT" || this._Direction == "TOP") ? -90 : 90 ;
				this._Half = true;
			}

			if(this._Direction == "RIGHT" || this._Direction == "TOP"){
				this._Ang = (this._Ang > (this._Step_ang) && this._Half) ? this._Ang-(this._Step_ang) : this._Ang;
			}else{
				this._Ang = (this._Ang < (-this._Step_ang) && this._Half) ? this._Ang+(this._Step_ang) : this._Ang;
			}

			if(
				((this._Direction == "RIGHT" || this._Direction == "TOP") && this._Ang > 0 && this._Half) ||
					((this._Direction == "LEFT" || this._Direction == "BOTTOM") && this._Ang < 0 && this._Half)
				){
				this.jO
					.removeClass("flippy_active")
					.css({
						"-webkit-transform": "rotate"+_Axis+"(0deg)",
						"-moz-transform": "rotate"+_Axis+"(0deg)",
						"-o-transform": "rotate"+_Axis+"(0deg)",
						"-ms-transform": "rotate"+_Axis+"(0deg)",
						"transform": "rotate"+_Axis+"(0deg)"
					})
					.find(".flippy_light")
					.remove()
                    ;

				clearInterval(this._Int);
				this._Half = false;
				this._After();

				//! End animation
				return;
			}else{
				this.jO.css({
					"-webkit-transform": "rotate"+_Axis+"("+this._Ang+"deg)",
					"-moz-transform": "rotate"+_Axis+"("+this._Ang+"deg)",
					"-o-transform": "rotate"+_Axis+"("+this._Ang+"deg)",
					"-ms-transform": "rotate"+_Axis+"("+this._Ang+"deg)",
					"transform": "rotate"+_Axis+"("+this._Ang+"deg)"
				});
			}

			this.applyLight();


		};

		/**
		 * Apply light to CSS flipped element
		 * @return void
		 */
		this.applyLight = function()
		{
			if(this.jO.find(".flippy_light").size() === 0){
				this
					.jO
					.append('<div class="flippy_light"></div>')
					.find(".flippy_light")
					.css({
						"position":"absolute",
						"top":"0",
						"left":"0",
						"min-width":this._nW+"px",
						"min-height":this._nH+"px",
						"width":this._nW+"px",
						"height":this._nH+"px",
						"background-color":((this._Direction == "LEFT"  && this._Half) || (this._Direction == "RIGHT"  && !this._Half) || (this._Direction == "TOP"  && this._Half) || (this._Direction == "BOTTOM"  && !this._Half))? "#000" : "#FFF",
						"opacity":(Math.abs(this._Ang)*this._Light/90)/100
					});
			}else{
				this
					.jO
					.find(".flippy_light")
					.css({
						"background-color":((this._Direction == "LEFT"  && this._Half) || (this._Direction == "RIGHT"  && !this._Half) || (this._Direction == "TOP"  && this._Half) || (this._Direction == "BOTTOM"  && !this._Half))? "#000" : "#FFF",
						"opacity":(Math.abs(this._Ang)*this._Light/90)/100
					});
			}

		};

		/**
		 * Create the flippy canvas
		 * @return void
		 */
		this.initiateFlippy = function()
		{
			var cv_pattern;
			this.jO
				.addClass('flippy_active')
				.empty()
				.css({
					"opacity":this._Color_alpha,
					"background":"none",
					"position":"relative",
					"overflow":"visible"
				});

			switch(this._Direction){
				case "TOP":
					this._CenterX = (Math.sin(PI/2)*this._nW*this._Depth);
					this._CenterY = this._H/2;
					cv_pattern = '<canvas id="flippy'+this._UID+'" class="flippy" width="'+(this._W+(2*this._CenterX))+'" height="'+this._H+'"></canvas>';
					this.new_flippy(cv_pattern);
					this.jO.find("#flippy"+this._UID)
						.css({
							"position":"absolute",
							"top":"0",
							"left":"-"+this._CenterX+"px"
						});
					break;
				case "BOTTOM":
					this._CenterX = (Math.sin(PI/2)*this._nW*this._Depth);
					this._CenterY = this._H/2;
					cv_pattern = '<canvas id="flippy'+this._UID+'" class="flippy" width="'+(this._W+(2*this._CenterX))+'" height="'+this._H+'"></canvas>';
					this.new_flippy(cv_pattern);
					this.jO.find("#flippy"+this._UID)
						.css({
							"position":"absolute",
							"top":"0",
							"left":"-"+this._CenterX+"px"
						});
					break;
				case "LEFT":
					this._CenterY = (Math.sin(PI/2)*this._nH*this._Depth);
					this._CenterX = this._W/2;
					cv_pattern = '<canvas id="flippy'+this._UID+'" class="flippy" width="'+this._W+'" height="'+(this._H+(2*this._CenterY))+'"></canvas>';
					this.new_flippy(cv_pattern);
					this.jO.find("#flippy"+this._UID)
						.css({
							"position":"absolute",
							"top":"-"+this._CenterY+"px",
							"left":"0"
						});
					break;
				case "RIGHT":
					this._CenterY = (Math.sin(PI/2)*this._nH*this._Depth);
					this._CenterX = this._W/2;
					cv_pattern = '<canvas id="flippy'+this._UID+'" class="flippy" width="'+this._W+'" height="'+(this._H+(2*this._CenterY))+'"></canvas>';
					this.new_flippy(cv_pattern);
					this.jO.find("#flippy"+this._UID)
						.css({
							"position":"absolute",
							"top":"-"+this._CenterY+"px",
							"left":"0"
						});
					break;
			}
		};

		/**
		 * redraw Canvas
		 * @return void
		 */
		this.drawFlippy = function()
		{
			this._Ang += this._Step_ang;
			if(this._Ang > 90 && this._Ang <= (90+this._Step_ang)){
				this._Midway();
				this.jO.css({"opacity":this._Color_target_alpha});
			}
			this._Ang = (this._Ang > (180+this._Step_ang)) ? this._Ang-(180+this._Step_ang) : this._Ang;

			var rad = (this._Ang/180)*PI;

			if(this.cvO === null){ return; }
			if(_isIE && !_Support_Canvas){ G_vmlCanvasManager.initElement(this.cvO);}
			var ctx = this.cvO.getContext("2d");
			ctx.clearRect(0, 0, this._W+(2*this._CenterX), this._H+(2*this._CenterY));
			ctx.beginPath();
			var deltaH = Math.sin(rad)*this._H*this._Depth;
			var deltaW = Math.sin(rad)*this._W*this._Depth;
			var X, Y;

			switch(this._Direction){
				case "LEFT" :
					X = Math.cos(rad)*(this._W/2);
					ctx.fillStyle = (this._Ang > 90) ?this.changeColor(this._Color_target,Math.floor(Math.sin(rad)*this._Light)) : this.changeColor(this._Color,-Math.floor(Math.sin(rad)*this._Light));
					ctx.moveTo(this._CenterX-X,this._CenterY+deltaH);//TL
					ctx.lineTo(this._CenterX+X,this._CenterY-deltaH);//TR
					ctx.lineTo(this._CenterX+X,this._CenterY+this._H+deltaH);//BR
					ctx.lineTo(this._CenterX-X,this._CenterY+this._H-deltaH);//BL
					ctx.lineTo(this._CenterX-X,this._CenterY);//loop
					ctx.fill();
					break;
				case "RIGHT" :
					X = Math.cos(rad)*(this._W/2);
					ctx.fillStyle = (this._Ang > 90) ? this.changeColor(this._Color_target,-Math.floor(Math.sin(rad)*this._Light)) : this.changeColor(this._Color,Math.floor(Math.sin(rad)*this._Light));
					ctx.moveTo(this._CenterX+X,this._CenterY+deltaH);//TL
					ctx.lineTo(this._CenterX-X,this._CenterY-deltaH);//TR
					ctx.lineTo(this._CenterX-X,this._CenterY+this._H+deltaH);//BR
					ctx.lineTo(this._CenterX+X,this._CenterY+this._H-deltaH);//BL
					ctx.lineTo(this._CenterX+X,this._CenterY);//loop
					ctx.fill();
					break;
				case "TOP" :
					Y = Math.cos(rad)*(this._H/2);
					ctx.fillStyle = (this._Ang > 90) ? this.changeColor(this._Color_target,-Math.floor(Math.sin(rad)*this._Light)) : this.changeColor(this._Color,Math.floor(Math.sin(rad)*this._Light));
					ctx.moveTo(this._CenterX+deltaW,this._CenterY-Y);//TL
					ctx.lineTo(this._CenterX-deltaW,this._CenterY+Y);//TR
					ctx.lineTo(this._CenterX+this._W+deltaW,this._CenterY+Y);//BR
					ctx.lineTo(this._CenterX+this._W-deltaW,this._CenterY-Y);//BL
					ctx.lineTo(this._CenterX,this._CenterY-Y);//loop
					ctx.fill();
					break;
				case "BOTTOM" :
					Y = Math.cos(rad)*(this._H/2);
					ctx.fillStyle = (this._Ang > 90) ? this.changeColor(this._Color_target,Math.floor(Math.sin(rad)*this._Light)) : this.changeColor(this._Color,-Math.floor(Math.sin(rad)*this._Light));
					ctx.moveTo(this._CenterX+deltaW,this._CenterY+Y);//TL
					ctx.lineTo(this._CenterX-deltaW,this._CenterY-Y);//TR
					ctx.lineTo(this._CenterX+this._W+deltaW,this._CenterY-Y);//BR
					ctx.lineTo(this._CenterX+this._W-deltaW,this._CenterY+Y);//BL
					ctx.lineTo(this._CenterX,this._CenterY+Y);//loop
					ctx.fill();
					break;
			}

			if(this._Ang > 180){
				this.jO
					.removeClass("flippy_active")
					.css({
						"background":this._Color_target
					})
					.append(this._Verso)
					.removeClass("flippy_container")
					.find(".flippy")
					.remove();

				clearInterval(this._Int);
				this._After();

				//! End animation
				return;
			}

			this._During();
		};

		/**
		 * Create Canvas from HTML source cv_pattern
		 * @param cv_pattern string canvas pattern
		 * @return void
		 */
		this.new_flippy = function(cv_pattern)
		{
			if(_isIE && !_Support_Canvas){

				this.jO
					.addClass("flippy_container")
					.attr("id","flippy_container"+this._UID);
				var $that = document.getElementById("flippy_container"+this._UID);
				var cv = document.createElement(cv_pattern);

				$that.appendChild(cv);
			}else{
				this.jO.append(cv_pattern);
			}
		};

		/**
		 * Convert a rgb or rgba to an Hex color code
		 * @param color string color
		 * @return string Hex color code
		 */
		this.convertColor = function(color)
		{
			var theColor = _ColorsRef.hasOwnProperty(color) ? _ColorsRef[color] : color;

			if (/^transparent$/i.test(theColor))
				return '#ffffff';

			if(theColor.substr(0,4) == "rgb("){
				return [
					"#",
					this.toHex(theColor.substr(4,theColor.length).split(',')[0]  >>> 0),
					this.toHex(theColor.substr(3,theColor.length).split(',')[1] >>> 0),
					this.toHex(theColor.substr(3,theColor.length-4).split(',')[2] >>> 0)].join('');
			}

			if(theColor.substr(0,5) == "rgba("){
				return ["#",
					this.toHex(theColor.substr(5,theColor.length).split(',')[0] >>> 0),
					this.toHex(theColor.substr(3,theColor.length).split(',')[1] >>> 0),
					this.toHex(theColor.substr(3,theColor.length-4).split(',')[2] >>> 0)].join('');
			}

			return theColor;
		};

		/**
		 * Convert a dec to hex
		 * @param dec mixed the dec
		 * @return string hex value
		 */
		this.toHex = function(dec)
		{
			var modulos = [];
			while(Math.floor(dec)>16){
				modulos.push(dec%16);
				dec = Math.floor(dec/16);
			}

			var Hex, i;
			switch(dec){
				case 10 : Hex = "A"; break;
				case 11 : Hex = "B"; break;
				case 12 : Hex = "C"; break;
				case 13 : Hex = "D"; break;
				case 14 : Hex = "E"; break;
				case 15 : Hex = "F"; break;
				default : Hex = ""+dec; break;
			}
			for( i=modulos.length-1;i>=0;i--){
				switch(modulos[i]){
					case 10 : Hex += "A"; break;
					case 11 : Hex += "B"; break;
					case 12 : Hex += "C"; break;
					case 13 : Hex += "D"; break;
					case 14 : Hex += "E"; break;
					case 15 : Hex += "F"; break;
					default : Hex += ""+modulos[i]; break;
				}
			}
			if (Hex.length == 1 ){
				return "0"+Hex;
			} else {
				return Hex;
			}
		};

		/**
		 * Change the color to the next step
		 * @return string the Hex color code
		 */
		this.changeColor = function(colorHex,step)
		{
			var redHex = colorHex.substr(1,2);
			var greenHex = colorHex.substr(3,2);
			var blueHex = colorHex.substr(5,2);

			var redDec = (parseInt(redHex,16)+step > 255) ? 255 : parseInt(redHex,16)+step;
			var greenDec = (parseInt(greenHex,16)+step > 255) ? 255 : parseInt(greenHex,16)+step;
			var blueDec = (parseInt(blueHex,16)+step > 255) ? 255 : parseInt(blueHex,16)+step;

			redHex = (redDec <= 0) ? "00" : this.toHex(redDec);
			greenHex = (greenDec <= 0) ? "00" : this.toHex(greenDec);
			blueHex = (blueDec <= 0) ? "00" : this.toHex(blueDec);
			return "#"+redHex+greenHex+blueHex;
		};

		//! Define attributes
		opts = $.extend({
			step_ang:10,
			refresh_rate:15,
			duration:300,
			depth:0.12,
			color_target:"white",
			light:60,
			content:"",
			direction:"LEFT",
			noCSS:false,
			onStart:function(){},
			onMidway:function(){},
			onAnimation:function(){},
			onFinish:function(){}
		}, opts);

		//this._Int;
		this._Half = false;
		this._UID = Math.floor(Math.random()* 1000000);
		this.jO = $jO;
		this._noCSS = opts.noCSS;
		//this.cvO;

		this._Ang = 0;
		this._Step_ang = (opts.refresh_rate/opts.duration)*200;
		this._Refresh_rate = opts.refresh_rate;
		this._Duration = opts.duration; // UNUSED!

		this._Depth = opts.depth;
		//this._CenterX;
		//this._CenterY;

		this._Color_target_is_rgba = (opts.color_target.substr(0,5) == "rgba(");
		this._Color = $jO.css("background-color");
		this._Color_target_alpha = (this._Color_target_is_rgba)? opts.color_target.substr(3,opts.color_target.length-4).split(',')[3]  >>> 0   : 1;
		this._Color_alpha = /^transparent$/i.test('' + this._Color) ? 0 : (this._Color.substr(0,5) == "rgba(")? this._Color.substr(3,this._Color.length-4).split(',')[3]  >>> 0 : 1;
		this._Color_target = this.convertColor(opts.color_target);
		this._Color = this.convertColor(this._Color);

		this._Direction = opts.direction;
		this._Light = opts.light;

		this._Content = (typeof opts.content == "object") ? opts.content.html() : opts.content;
		this._Recto_color = this._Color;
		this._Verso_color = this._Color_target;
		this._Recto = (opts.recto !== undefined)? opts.recto : this.jO.children();
		this._Verso = (opts.verso !== undefined)? opts.verso : this._Content;

		this._Before = opts.onStart;
		this._During = opts.onAnimation;
		this._Midway = opts.onMidway;
		this._After = opts.onFinish;

		this._nW = this.jO.width();
		this._nH = this.jO.height();
		this._W = this.jO.outerWidth();
		this._H = this.jO.outerHeight();

		opts = null;

	}; //! end Class flipBox

	//! initiate jQuery Plugin
	$.fn.flippy = function(opts)
	{

		return this.each(function(){
			$t = $(this);
			if(!$t.hasClass("flippy_active")){
				var _FP = new flipBox($t,opts);
				_FP.animate();
			}

		});

	};//! end $.flippy() function

	//! flippyReverse() function
	$.fn.flippyReverse = function()
	{

		return this.each(function(){
			$t = $(this);
			if(!$t.hasClass("flippy_active")){
				var _FP = $t.data("_oFlippy_");
				_FP.animate(true);
			}

		});

	};//! end $.flippyReverse() function


})(jQuery);
