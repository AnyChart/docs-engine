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
        if (target[i] == version) break;
        else if (target[i] != "") path.push(target[i]);
    }
    path = path.reverse();
    var $el;
    var str = "";
    for (var i = 0; i < path.length; i++) {
        str += "/" + path[i];
        // for both links: /Quick_Start/Quick_start - default page and /7.12.0/Quick_Start/Quick_Start - version page
        $el = $menu.find("a[href='"+str+"'], a[href='"+ "/" + version + str+"']");
        var $ul = $el.parent().find(">ul");
        if ($ul.length && !$ul.is(":visible")) {
            $ul.toggle();
            $el.find("i").removeClass('folder-close').addClass('folder-open');
        }
    }
    if ($el){
        $el.addClass("active");
    }
};

$menu.find('ul').hide();
$menu.find('a>i.folder-open').removeClass('folder-open').addClass('folder-close');
$menu.find('a>i.folder-close').each(function() {
    var $this = $(this);
    var $link = $this.parent();
    var $ul = $link.parent().find(">ul");
    $link.click(function() {
        $ul.toggle();
        if ($ul.is(":visible")) {
            $this.removeClass('folder-close').addClass('folder-open');
            $this.html("- ");
        }else {
            $this.removeClass('folder-open').addClass('folder-close');
            $this.html("+ ");
        }
        return false;
    });
});
$menu.find("a").each(function() {
    if ($(this).find(">i.ac-file-text").length) {
        $(this).click(function(e) {
            var url = $(this).attr("href");
            if (e.ctrlKey || e.metaKey){
                var win = window.open(url, '_blank');
                return false;
            }else{
                return loadPage(url);
            }
        });
    }
});


expandMenu(location.pathname);

// resize table content height
/*window.addEventListener("resize", function(){
    onResize();
});
var onResize = function(){
    var windowHeight = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
    var tableOfContentLarge = $("#table-of-content-large");
    var tableOfContent = $($(".table_of_content")[1]);
    if( tableOfContent.height() != null){
        if (tableOfContent.height() + 120 + 85 > windowHeight){
            tableOfContentLarge.height(windowHeight - 120 - 85);
        }else{
            console.log("Less " +  tableOfContent.height());
        }
    }
};
window.onload = function(){
    onResize();
};
onResize();
*/

// The function actually applying the offset
/*function offsetAnchor() {
    if (location.hash.length !== 0) {
        window.scrollTo(window.scrollX, window.scrollY - 100);
    }
}

// Captures click events of all <a> elements with href starting with #
$(document).on('click', 'a[href^="#"]', function(event) {
    // Click events are captured before hashchanges. Timeout
    // causes offsetAnchor to be called after the page jump.
    window.setTimeout(function() {
        offsetAnchor();
    }, 0);
});

// Set the offset when entering page with hash present in the url
window.setTimeout(offsetAnchor, 0);*/