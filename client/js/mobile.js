$(".sidebar-switcher").click(function() {
    $("#bar").toggle();
    $("#shadow").toggle();
});

$("#shadow").click(function() {
    $("#bar").toggle();
    $("#shadow").toggle();
});

$(window).keyup(function(e) {
    if (e.keyCode == 27) {
        $("#bar").hide();
        $("#shadow").hide();
    }
});
