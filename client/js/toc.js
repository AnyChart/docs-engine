function fixToc() {
    
    var $next = $("#content h1").next();
    while ($next.prop("tagName").toLowerCase() == "br") {
        $next = $next.next();
    }
    $next = $next.first();
    
    $("div.wrapper.container-fluid>div.row>div.visible-lg").html('');
    if ($next.prop("tagName").toLowerCase() == "ul") {
        $next.addClass("table_of_content");
        $next.find("li").addClass("main");
        $("#table-of-content-small").append($next.clone());
        $("#table-of-content-large").append($next.clone());
        $next.remove();
    }
};

fixToc();
