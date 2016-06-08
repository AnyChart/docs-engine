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

var expandMenu = function(target) {
    $menu.find(".active").removeClass("active");
    target = target.split("/");
    var path = [];
    for (var i = target.length - 1; i >= 0; i--) {
        if (target[i] == version)
            break;
        path.push(target[i]);
    }
    path = path.reverse();
    var $el;
    var str = "/" + version;
    for (var i = 0; i < path.length; i++) {
        str += "/" + path[i];
        $el = $menu.find("a[href='"+str+"']");
        var $ul = $el.parent().find(">ul");
        if ($ul.length && !$ul.is(":visible")) {
            $ul.toggle();
            $el.find("i").removeClass('fa-folder').addClass('fa-folder-open');
        }
    }
    $el.addClass("active");
};

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
$menu.find("a").each(function() {
    if ($(this).find(">i.fa-file").length) {
        /*$(this).click(function() {
            return loadPage($(this).attr("href"));
        });*/
    }
});

expandMenu(location.pathname);
