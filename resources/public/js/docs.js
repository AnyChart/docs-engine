$(function() {
    $("li i.group").parent().click(function() {
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
});
