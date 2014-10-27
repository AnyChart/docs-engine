$(function() {

    var $versions = $("#versions");

    $("#version").click(function(e) {
	e.stopPropagation();
	if (!$versions.is(":visible")) {
	    $versions.show();
	    $(this).find("i").removeClass("fa-caret-right");
	    $(this).find("i").addClass("fa-caret-down");
	}else {
	    $versions.hide();
	    $(this).find("i").addClass("fa-caret-right");
	    $(this).find("i").removeClass("fa-caret-down");
	}
	return false;
    });

    $(window).click(function() {
	$versions.hide();
    });
    
    $("li i.group").parent().click(function(e) {
	if (e.altClick || e.ctrlClick || e.metaKey) return true;
	var $li = $(this).parent();
	var $ul = $li.find("ul");
	$ul.toggle();
	if ($ul.is(':visible')) {
	    $li.find("i").removeClass('close').addClass('open');
	}else {
	    $li.find("i").removeClass('open').addClass('close');
	}
	return false;
    });

    var resize = function(e) {
	var w = e.pageX;
	$("#sidebar").css("width", w - 5);
	$("#content").css("left", w);
	$(".resizer").css("left", w);
    };

    var stopResizing = function() {
	$("#locker").hide();
	$(window).off("mousemove", resize);
	$(window).off("mouseup", stopResizing);
    };

    $(".resizer").mousedown(function() {
	$("#locker").show();
	$(window).on("mousemove", resize);
	$(window).on("mouseup", stopResizing);
    });

    var updatePage = function(url, data) {
	if (!data["title"]) return;
	$(window).scrollTop(0);
	
	$("title").text(data["title"] + " - AnyChart documentation");
	$("#content>.main").html(data['content']);
	$("#content").scrollTop(0);

	$(".main pre").wrap("<div class='code'></div>");
	$("<p>Code</p>").insertBefore(".main .code pre");
	$(".main pre").addClass("brush: js");
	SyntaxHighlighter.highlight();
    };

    window.onpopstate = function(event) {
	var href = document.location;
	$.get(href+"-json", function(data) {
	    updatePage(href, data);
	});
    };

    $("#sidebar .page").click(function(e) {
	if (e.altClick || e.ctrlClick || e.metaKey) return true;
	if (window.history && window.history.pushState) {
	    var $this = $(this);
	    var href = $this.attr('href');
	    window.history.pushState(null, null, href);
	    $.get(href+"-json", function(data) {
		updatePage(href, data);
	    });
	    return false;
	}else {
	    return true;
	}
    });
});
