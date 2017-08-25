function toggleMobileMenu(){
    $(".left-sidebar-container").toggle();
    $("#shadow").toggle();
    $("#page-content").toggle();
    $(".ac.ac-bars").toggle();
    $(".ac.ac-remove").toggle();
}

function hideMobileMenu(){
    $(".left-sidebar-container").hide();
    $("#shadow").hide();
    $("#page-content").show();
    $(".ac.ac-bars").show();
    $(".ac.ac-remove").hide();
}

$(".sidebar-switcher").click(function() {
    toggleMobileMenu();
});

$("#shadow").click(function() {
    toggleMobileMenu();
});

$(window).keyup(function(e) {
    if (e.keyCode == 27) {
        toggleMobileMenu();
    }
});