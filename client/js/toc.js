function fixToc() {

    var idx = 0;
    var $items = $("#page-content .row .col-lg-17 > *");

    if ($items.get(idx).tagName.toLowerCase() == "a")
        idx++;

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

    $("#table-of-content-large").html('');
    if ($next.prop("tagName").toLowerCase() == "ul") {
        $next.addClass("table_of_content");
        $next.find("li").addClass("main");
        $("#table-of-content-large").append($next.clone());
    }
}

fixToc();
