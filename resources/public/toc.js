function toc() {
    var $next = $("#content h1").next();
    $("div.wrapper.container-fluid>div.row>div.visible-lg").html('');
    if ($next.prop("tagName").toLowerCase() == "ul") {
        $next.addClass("table_of_content");
        $next.find("li").addClass("main");
        $("div.wrapper.container-fluid>div.row>div.visible-lg").append($next.clone());
        $next.remove();
    }
};

toc();
