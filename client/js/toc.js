function fixToc() {

    var idx = 0;
    var $items = $("#content .col-lg-17 > *");
    while ($items.get(idx).tagName.toLowerCase() == "br")
        idx++;

    if ($items.get(idx).tagName.toLowerCase() == "h1")
        idx++;

    while ($items.get(idx).tagName.toLowerCase() == "br")
        idx++;

    if ($items.get(idx).tagName.toLowerCase() == "div")
        idx++;

    while ($items.get(idx).tagName.toLowerCase() == "br")
        idx++;

    var $next = $($items.get(idx));
    
    $("div.wrapper.container-fluid>div.row>div.visible-lg").html('');
    if ($next.prop("tagName").toLowerCase() == "ul") {
        $next.addClass("table_of_content");
        $next.find("li").addClass("main");
        $("#table-of-content-small").append($next.clone());
        $("#table-of-content-large").append($next.clone());
        $next.remove();
        $('#article-content ul:first-child').css('display', 'block');
    }
};

fixToc();
