const selectedVersion = () => $("#versionSelect :selected").text();


const errorFn = (jqXHR, textStatus, errorThrown) => {
    alert('An error occurred... Look at the console for more information!')
};


const rebuildSuccessFn = (data, textStatus, jqXHR) => {
    alert("Rebuild has started!");
    window.location.href = "/_admin_";
};


init = (e) => {
    $('#deleteButton').click((e) => {
        $('button').prop('disabled', true);
        $.ajax({
            type: "POST",
            url: `/_delete_`,
            data: {version: selectedVersion()},
            success: (data, textStatus, jqXHR) => {
                window.location.href = "/_admin_";
            },
            error: errorFn
        });
    });

    $('#rebuildCommit').click((e) => {
        $('button').prop('disabled', true);
        e.preventDefault();
        $.ajax({
            type: "POST", url: `/_rebuild_`,
            data: {version: selectedVersion()},
            success: rebuildSuccessFn, error: errorFn
        });
    });

    $('#rebuildFast').click((e) => {
        $('button').prop('disabled', true);
        e.preventDefault();
        $.ajax({
            type: "POST", url: `/_rebuild_`,
            data: {
                version: selectedVersion(),
                fast: true
            },
            success: rebuildSuccessFn, error: errorFn
        });
    });

    $('#rebuildLinkChecker').click((e) => {
        $('button').prop('disabled', true);
        e.preventDefault();
        $.ajax({
            type: "POST", url: `/_rebuild_`,
            data: {
                version: selectedVersion(),
                linkChecker: true
            },
            success: rebuildSuccessFn, error: errorFn
        });
    });

    $('#showReportLink').click((e) => {
        window.location.href = `/${selectedVersion()}/report`
    });

    $('#githubLink').click((e) => {
        e.preventDefault();
        window.location.href = `https://github.com/AnyChart/docs.anychart.com/tree/${selectedVersion()}`
    });
};

init();