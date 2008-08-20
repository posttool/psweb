
var UPLOAD_GATEWAY = "${module_url}/ResourceModule/handleHttpRequest/";
var UPLOAD_OBSERVER = "${module_url}/ResourceModule/getUploads/.json";
var IMAGE_PREVIEW = "${module_url}/ResourceModule/getPreview/";

var upload_name;
var upload_gateway_url;
var upload_observers_url = UPLOAD_OBSERVER;
var upload_poll_frequency = 500; //ms

function upload_init()
{
	upload_get_observers(upload_init_ok, upload_error);
}
function upload_init_ok(uploads)
{
	var b = 0;
	for (var p in uploads)
	{
		var bb = Number(uploads[p].name.substring(9));
		if (bb>b) b = bb;
	}
	upload_name = "uploader_"+ (b+1);
	upload_gateway_url =  UPLOAD_GATEWAY + upload_name + "/.html";
	//
	reset_ui();
	upload_on_observer_response(uploads,true);
}
function upload_get_observers(ok_func, err_func)
{
	new Ajax.Request(upload_observers_url,
	{
    method:'get',
    onSuccess: function(transport){
      var json = transport.responseText.evalJSON();
      ok_func(json);
    },
    onFailure: err_func
    });
}
function upload_on_observer_response(uploads,ignore_complete)
{
	if (ignore_complete == null)
		ignore_complete = false;
		
	upload_update_ui(uploads);
	//
	var all_complete = true;
	for (var p in uploads)
	{
		if (!uploads[p].isComplete)
		{
			all_complete = false;
			break;
		}
	}
	if (all_complete && !ignore_complete)
		upload_complete(uploads);
	else if (!all_complete)
		setTimeout("upload_get_observers(upload_on_observer_response, upload_error)", upload_poll_frequency);

}
// ui
function reset_ui()
{
	$("upload_form_and_buttons").style.display = "block";
	$("bbb").value = "";
	$("ddd").value = "";
	$("submit").style.display = "block";
	$("cancel").style.display = "none";
}
function submit_upload()
{
	
	$("submit").style.display = "none";
	$("cancel").style.display = "block";
	//
	$("upload_form").action = upload_gateway_url;
	$("upload_form").submit();
	upload_get_observers(upload_on_observer_response, upload_error);
}
function cancel_upload()
{
	$("hidden_frame").src = "blank.jsp";
	upload_init();
}

function upload_update_ui(uploads)
{
	var s = "";
	for (var p in uploads)
	{
		var upload = uploads[p];
		//
		var title;
		if (upload.isComplete)
			title = "<i>"+upload.name+"</i>";
		else
			title = "<b>"+upload.name+"</b>";
		s += title +" (";
		var total = 0;
		for (var j=0; j<upload.observers.length; j++)
		{
			var observer = upload.observers[j];
			s += observer.fileName+"="+observer.progress+"%,  ";
			total += observer.progress;
		}
		s += ") complete="+total+"<br/>";
		
	}
	$("status").innerHTML = s;

}
function upload_complete(upload)
{
	get_preview_images(upload[upload_name]);
	upload_init();
}
function upload_error(s)
{
	alert("ERROR "+s);
}
// ajax get previews (unused)
function get_preview_images_ajax(upload)
{
	for (var j=0; j<upload.observers.length; j++)
	{
		var o = upload.observers[j];
		new Ajax.Request(IMAGE_PREVIEW + o.completionObject.id + "/100/100/.json",
		{
	    method:'get',
	    onSuccess: function(transport){
	      var json = transport.responseText.evalJSON();
	      get_preview_ok(json);
	    },
	    onFailure: function(s){ alert("get preview error "+s); } 
	    });
	}
}
function get_preview_ajax_ok(io)
{
	$("previews").innerHTML += "<img src='"+io.url+"' width='100' height='100'/>"; 
}
// img src gateway module access!
function get_preview_images(upload)
{
	alert("X"+upload)
	for (var j=0; j<upload.observers.length; j++)
	{
		var o = upload.observers[j];
		var url = IMAGE_PREVIEW + o.completionObject.id + "/100/100/.jpg";
		$("previews").innerHTML += "<img src='"+url+"' width='100' height='100'/>"; 
	}
}