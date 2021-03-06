function fixToc() {

    var idx = 0;
    var $items = $("#article-content > *");

    if ($items.get(idx) && $items.get(idx).tagName.toLowerCase() == "a")
        idx++;

    while ($items.get(idx) && $items.get(idx).tagName.toLowerCase() == "br")
        idx++;

    if ($items.get(idx) && $items.get(idx).tagName.toLowerCase() == "h1")
        idx++;

    while ($items.get(idx) && $items.get(idx).tagName.toLowerCase() == "br")
        idx++;

    if ($items.get(idx) && $items.get(idx).tagName.toLowerCase() == "div")
        idx++;

    while ($items.get(idx) && $items.get(idx).tagName.toLowerCase() == "br")
        idx++;

    var $next = $items.get(idx);

    $("#table-of-content-large").html('');
    var $articleContent = $("#article-content");
    var $rightButtons = $($(".right-buttons")[0]);
    if ($next && $next.tagName.toLowerCase() == "ul") {
        $next = $($next);
        $next.addClass("table_of_content");
        $next.find("li").addClass("main");
        // copy menu from #article-content to right menu
        $("#table-of-content-large").append($next.clone());
        // copy menu from #article-content to #page-content
        $next.insertBefore($articleContent);
        $("#table-of-content-large").show();
    } else {
        $("#table-of-content-large").hide();
    }
    /* copying elements from article content to page content
        for better styling table of content, buttons (not styles overriding */
    // copy buttons to #page-content
    $rightButtons.clone().insertBefore($articleContent);
    // copy h1 from #article-content to #page-content
    $("#page-content").prepend($articleContent.children()[0]);
}

fixToc();

// move to top on scroll
window.addEventListener('scroll', function(e) {
    var top = Math.max(85, 134 - window.scrollY);
    $('.right-bar-side').css('top', top + 'px');
    onResize();
});

// resize table content height
var onResize = function() {
    var windowHeight = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
    $("#table-of-content-large").css('max-height', (windowHeight - 130 - Math.max(85, 134 - window.scrollY)) + 'px');
};
window.addEventListener("resize", function() {
    onResize();
});
window.onload = function() {
    onResize();
};
onResize();
