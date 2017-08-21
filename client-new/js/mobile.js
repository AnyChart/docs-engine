$(".sidebar-switcher").click(function() {
    $(".left-sidebar-container").toggle();
    $("#shadow").toggle();
});

$("#shadow").click(function() {
    $(".left-sidebar-container").toggle();
    $("#shadow").toggle();
});

$(window).keyup(function(e) {
    if (e.keyCode == 27) {
        $(".left-sidebar-container").hide();
        $("#shadow").hide();
    }
});
