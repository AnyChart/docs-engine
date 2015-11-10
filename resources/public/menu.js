var $menu = $("ul.menu");
$(window).scroll(function(e) {
    return;
    if ($(window).scrollTop() > 50) {
        $menu.css({"position": "absolute",
                   "top": $(window).scrollTop(),
                   "height": $(window).height()});
    }else {
        $menu.removeAttr("style");
    }
});

$menu.find('ul').hide();
$menu.find('a>i.fa-folder-open').removeClass('fa-folder-open').addClass('fa-folder');
$menu.find('a>i.fa-folder').each(function() {
    var $this = $(this);
    var $link = $this.parent();
    var $ul = $link.parent().find(">ul");
    $link.click(function() {
        $ul.toggle();
        if ($ul.is(":visible")) {
            $this.removeClass('fa-folder').addClass('fa-folder-open');
        }else {
            $this.removeClass('fa-folder-open').addClass('fa-folder');
        }
        return false;
    });
});
