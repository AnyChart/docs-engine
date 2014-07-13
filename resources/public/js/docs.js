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
});
