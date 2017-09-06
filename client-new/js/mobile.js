var showPage = true;

function toggleMobileMenu(){
    showPage = !showPage;
    if (showPage){
        hideMobileMenu();
    }else{
        showMobileMenu();
    }
}

function showMobileMenu(){
    $(".left-sidebar-container").show();
    $("#page-content").hide();
    $(".ac.ac-bars").hide();
    $(".ac.ac-remove").show();
}

function hideMobileMenu(){
    $(".left-sidebar-container").hide();
    $("#page-content").show();
    $(".ac.ac-bars").show();
    $(".ac.ac-remove").hide();
}

$(".sidebar-switcher").click(function() {
    toggleMobileMenu();
});

$(".white-shadow").click(function() {
    toggleMobileMenu();
});

$(window).keyup(function(e) {
    if (e.keyCode == 27) {
        toggleMobileMenu();
    }
});