Function.prototype.bind = function()
{
	if (arguments.length == 0) return this;

	var object = arguments[0];
	var args = [];
	for (var i=1; i<arguments.length; i++)
		args[i-1] = arguments[i];
		
    var __method = this;
    return function() {
	  var args0 = [];
	  for (var i=0; i<args.length; i++)
	  	args0.push(args[i]);
	  for (var i=0; i<arguments.length; i++)
	  	args0.push(arguments[i]);
      return __method.apply(object, args0);
    }
}

Function.prototype.bindAsEventListener = function()
{
    if (arguments.length == 0) return this;

	var object = arguments[0];
	var args = [];
	for (var i=1; i<arguments.length; i++)
		args[i-1] = arguments[i];
		
    var __method = this;
    return function(event) {
	  var args0 = [event || window.event];
	  for (var i=0; i<args.length; i++)
	  	args0.push(args[i]);
      return __method.apply(object, args0);
    }
}

String.prototype.startsWith = function(s)
{
	var reg = new RegExp("^" + s);
	return reg.test(this);
}

String.prototype.endsWith = function(s)
{
	var reg = new RegExp(s + "$");
	return reg.test(this);
}
String.prototype.trim = function() {
	return this.replace(/^\s+|\s+$/g,"");
}
String.prototype.ltrim = function() {
	return this.replace(/^\s+/,"");
}
String.prototype.rtrim = function() {
	return this.replace(/\s+$/,"");
}


function $(div)
{
	return document.getElementById(div);
}

function getRadioValue(form, field_name) {
	var radioObj = form[field_name];
	if(!radioObj)
		return "";
	var radioLength = radioObj.length;
	if(radioLength == undefined)
		if(radioObj.checked)
			return radioObj.value;
		else
			return "";
	for(var i = 0; i < radioLength; i++) {
		if(radioObj[i].checked) {
			return radioObj[i].value;
		}
	}
	return "";
}


///

function com_pagesociety_DoModule(moduleMethod, args, returnFunction, errorFunction, noencode)
{
 	com_pagesociety_LockUi();
 	var onSuccess = function(o) 
	{
		com_pagesociety_UnlockUi();
		if (o.responseText == "NULL") {
			returnFunction();
		}
		else {
			var json = decodeURIComponent(o.responseText);
			var result = YAHOO.lang.JSON.parse(json);
			if (result.stacktrace)
			{
				if (errorFunction==null)
					alert(moduleMethod+"\n"+result.stacktrace);
				else
					errorFunction(result);
			}
			else
			{
				returnFunction(result);
			}
		}
	}
	var onFailure = errorFunction ? errorFunction : function(error) 
	{
		com_pagesociety_UnlockUi();
		var s = "";
		for (var p in error)
		 s+=p+"="+error[p]+"\n";
		alert(s);
	}
	var callback = 
	{
		success: onSuccess,
        failure: onFailure
	};
	var json_encoded = com_pagesociety_EncodeArgs(args);
    YAHOO.util.Connect.asyncRequest('POST', MODULE_URL + "/" + moduleMethod + "/.json"+(noencode!=null?"?noencode=1":""), 
    	callback, "json="+json_encoded);
}

function com_pagesociety_EncodeArgs(args)
{
	var json = YAHOO.lang.JSON.stringify(args);
	var json_encoded_args = encodeURIComponent(json);
	return json_encoded_args;
}

function com_pagesociety_LockUi()
{
	//YAHOO.util.Dom.setStyle(document.body,"visibility","hidden");
}

function com_pagesociety_UnlockUi()
{
	//YAHOO.util.Dom.setStyle(document.body,"visibility","visible");
}

var _overlay_item;
function position_overlay(overlay_item)
{
	if (overlay_item==null)
		_overlay_item = "overlay_base";
	else
		_overlay_item = overlay_item;
	
	
	//This will work for everything other than IE6 - and removes the weird scroll effect
	//in Firefox.

	$(_overlay_item).style.top = "50%";
	$(_overlay_item).style.left = "50%";
	
	var top = "-" + (($(_overlay_item).clientHeight)/2).toString() +"px";
	var lft = "-" + (($(_overlay_item).clientWidth)/2).toString() +"px";
	var bx = YAHOO.util.Dom.getDocumentWidth();
	var by = YAHOO.util.Dom.getDocumentHeight();

	bx = bx.toString() +"px";
	by = by.toString() +"px";
	$(_overlay_item).style.marginTop = top;
	$(_overlay_item).style.marginLeft = lft;
	$("overlay_blackout").style.height = by;
	$("overlay_blackout").style.width = bx;
	$("overlay_blackout").style.display = "block";


	YAHOO.util.Dom.setStyle("overlay_blackout", "opacity", .8);


	//Once again we have to fix IE6
	
	if (/MSIE (\d+\.\d+);/.test(navigator.userAgent)){ //test for MSIE x.x;
		var ieversion=new Number(RegExp.$1) // capture x.x portion and store as a number
		if (ieversion<7){
			
			if (overlay_item==null)
				_overlay_item = "overlay_base";
			else
				_overlay_item = overlay_item;
			
			var sx = YAHOO.util.Dom.getDocumentScrollLeft();
			var sy = YAHOO.util.Dom.getDocumentScrollTop();
			YAHOO.util.Dom.setXY("overlay_blackout", [sx, sy]);
		
		    
			var region = YAHOO.util.Dom.getRegion(_overlay_item);
			var elmHeight = region.bottom - region.top;
			var elmWidth = region.right - region.left;
		
			var x = (YAHOO.util.Dom.getViewportWidth() - elmWidth)/2 + sx;
			var y = (YAHOO.util.Dom.getViewportHeight() - elmHeight)/2 + sy;
			
			YAHOO.util.Dom.setXY( _overlay_item, [x,y]);
			window.onscroll = this.position_overlay.bind(null,_overlay_item);
		}
	}
}

function hide_overlay()
{
	$("overlay_blackout").style.display = "none";
	window.onscroll = null;	
}


function com_pagesociety_UpdateFragment(div, href){
    var callback = {
        success: function(r){
            $(div).innerHTML = r.responseText
        }
    };
    YAHOO.util.Connect.asyncRequest('GET', href, callback);
}
