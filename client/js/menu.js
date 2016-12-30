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
            $el.find("i").removeClass('ac-folder').addClass('ac-folder-open');
        }
    }
    $el.addClass("active");
};

$menu.find('ul').hide();
$menu.find('a>i.ac-folder-open').removeClass('ac-folder-open').addClass('ac-folder');
$menu.find('a>i.ac-folder').each(function() {
    var $this = $(this);
    var $link = $this.parent();
    var $ul = $link.parent().find(">ul");
    $link.click(function() {
        $ul.toggle();
        if ($ul.is(":visible")) {
            $this.removeClass('ac-folder').addClass('ac-folder-open');
        }else {
            $this.removeClass('ac-folder-open').addClass('ac-folder');
        }
        return false;
    });
});
$menu.find("a").each(function() {
    if ($(this).find(">i.ac-file-text").length) {
        $(this).click(function() {
            return loadPage($(this).attr("href"));
        });
    }
});

// for url like: Stock_Charts/Technical_Indicators/Bollinger_Bands_%25B
var replaceURI = function(){
    var path = location.pathname;
    try {
        path = decodeURI(path);
    } catch (err){}
    finally {
        window.history.replaceState( {} , "", encodeURI(path));
    }
};

replaceURI();
expandMenu(location.pathname);
