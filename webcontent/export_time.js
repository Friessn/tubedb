/* *** settings that may need to be changed at different runtime environments *** */
var url_base = "../";
var url_result_page = "export.html";
var url_go_page = "export_settings.html";
/* ***   *** */

var url_export_settings = url_base + "export/settings";
var url_export_apply_settings = url_base + "export/apply_settings";
var url_export_timespan = url_base + "export/timespan";
var url_region_json = url_base + "tsdb/region.json";

var tasks = 0;

function getID(id) {
	return document.getElementById(id);
}

$.postJSON = function(url, data, callback) {
    return jQuery.ajax({
        'type': 'POST',
        'url': url,
        'contentType': 'application/json',
        'data': JSON.stringify(data),
        'success': callback
    });
};

function incTask() {
	tasks++;	
	getID("status").innerHTML = "busy ("+tasks+")...";
}

function decTask() {
	tasks--;
	if(tasks===0) {
		getID("status").innerHTML = "ready";
	} else if(tasks<0){
		getID("status").innerHTML = "error";
	} else {
		getID("status").innerHTML = "busy ("+tasks+")...";
	}
}

function getTimespan() {
	incTask();
	$.get(url_export_timespan).done(function(data) {
		var value = +data;
		if(value===0) {
			time_select.val(0);
		} else {
			time_select.val(1+value-2008);
		}		
		decTask();
	}).fail(function() {alert("error getting data");decTask();});
}

function on_cancel() {
	window.location = url_result_page;
}

function on_apply() {
	incTask();
	json_settings.timestep = document.getElementById("choose_aggregation").value;
	
	if(document.getElementById("radio_all").checked) {
		json_settings.timespan_type = "all";
	} else if(document.getElementById("radio_year").checked) {
		json_settings.timespan_type = "year";
		json_settings.timespan_year = time_select_year.val();
	} else if(document.getElementById("radio_years").checked) {
		json_settings.timespan_type = "years";
		json_settings.timespan_years_from = time_select_years_from.val();
		json_settings.timespan_years_to = time_select_years_to.val();
	} else if(document.getElementById("radio_dates").checked) {
		json_settings.timespan_type = "dates";
		json_settings.timespan_dates_from = time_text_dates_from.val();
		json_settings.timespan_dates_to = time_text_dates_to.val();		
	}	
	
	$.postJSON(url_export_apply_settings,json_settings)
		.done(function() {
			window.location = url_result_page;
			decTask();
		 })
		.fail(function(jqXHR, textStatus, errorThrown) {alert("error sending settings data: "+textStatus+"  "+errorThrown);decTask();});
}

function on_apply_go() {
	incTask();
	json_settings.timestep = document.getElementById("choose_aggregation").value;
	
	if(document.getElementById("radio_all").checked) {
		json_settings.timespan_type = "all";
	} else if(document.getElementById("radio_year").checked) {
		json_settings.timespan_type = "year";
		json_settings.timespan_year = time_select_year.val();
	} else if(document.getElementById("radio_years").checked) {
		json_settings.timespan_type = "years";
		json_settings.timespan_years_from = time_select_years_from.val();
		json_settings.timespan_years_to = time_select_years_to.val();
	} else if(document.getElementById("radio_dates").checked) {
		json_settings.timespan_type = "dates";
		json_settings.timespan_dates_from = time_text_dates_from.val();
		json_settings.timespan_dates_to = time_text_dates_to.val();		
	}	
	
	$.postJSON(url_export_apply_settings,json_settings)
		.done(function() {
			window.location = url_go_page;
			decTask();
		 })
		.fail(function(jqXHR, textStatus, errorThrown) {alert("error sending settings data: "+textStatus+"  "+errorThrown);decTask();});
}

function on_radio_change() {
	var type = "all";
	if(document.getElementById("radio_year").checked) type="year";
	else if(document.getElementById("radio_years").checked) type="years";
	else if(document.getElementById("radio_dates").checked) type="dates";
	radio_select(type);
}

function disable_timespan_year(disabled) {
	document.getElementById("time_select_year").disabled = disabled;
}	

function disable_timespan_years(disabled) {
	document.getElementById("time_select_years_from").disabled = disabled;
	document.getElementById("time_select_years_to").disabled = disabled;
}

function disable_timespan_dates(disabled) {
	document.getElementById("time_text_dates_from").disabled = disabled;
	document.getElementById("time_text_dates_to").disabled = disabled;
}		

function radio_select(timespan_type) {
	if(timespan_type==="all") {
		document.getElementById("radio_all").checked = true;
		disable_timespan_year(true);
		disable_timespan_years(true);
		disable_timespan_dates(true);
	} else if (timespan_type==="year") {
		document.getElementById("radio_year").checked = true;
		disable_timespan_year(false);
		disable_timespan_years(true);
		disable_timespan_dates(true);
	} else if (timespan_type==="years") {
		document.getElementById("radio_years").checked = true;
		disable_timespan_year(true);
		disable_timespan_years(false);
		disable_timespan_dates(true);
	} else if (timespan_type==="dates") {
		document.getElementById("radio_dates").checked = true;
		disable_timespan_year(true);
		disable_timespan_years(true);
		disable_timespan_dates(false);
	}
}


var time_select_year;
var time_select_years_from;
var time_select_years_to;
var time_text_dates_from;
var time_text_dates_to;


$(document).ready(function(){
	incTask();

	time_select_year = $("#time_select_year");
	time_select_years_from = $("#time_select_years_from");
	time_select_years_to = $("#time_select_years_to");
	time_text_dates_from = $("#time_text_dates_from");
	time_text_dates_to = $("#time_text_dates_to");	
	
	document.getElementById("button_cancel").onclick = on_cancel;
	document.getElementById("button_apply").onclick = on_apply;
	document.getElementById("button_apply_go").onclick = on_apply_go;
	
	document.getElementById("radio_all").onchange = on_radio_change;
	document.getElementById("radio_year").onchange = on_radio_change;
	document.getElementById("radio_years").onchange = on_radio_change;
	document.getElementById("radio_dates").onchange = on_radio_change;
	
	
	incTask();
	$.getJSON(url_export_settings).done(function( data ) {
		json_settings = data;
		console.log(json_settings);
		
		document.getElementById("choose_aggregation").value = json_settings.timestep;
		
		var timespan_type = json_settings.timespan_type;
		radio_select(timespan_type);
		time_select_year.val(json_settings.timespan_year);
		time_select_years_from.val(json_settings.timespan_years_from);
		time_select_years_to.val(json_settings.timespan_years_to);
		time_text_dates_from.val(json_settings.timespan_dates_from);
		time_text_dates_to.val(json_settings.timespan_dates_to);
		
		incTask();
		$.get(url_region_json+"?region="+json_settings.region).done(function(json) {
			for(var year = json.view_year_range.start; year <= json.view_year_range.end; year++) {
				time_select_year.append(new Option(year,year));
				time_select_years_from.append(new Option(year,year));
				time_select_years_to.append(new Option(year,year));
			}			
			decTask();
		}).fail(function() {alert("error getting data");decTask();});		
		
		decTask();
	})
	.fail(function(data) {alert("error getting settings data: "+data);decTask();});	

	decTask();
});