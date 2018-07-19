const selectedVersion = () => $("#versionSelect :selected").text();


const errorFn = (jqXHR, textStatus, errorThrown) => {
    alert('An error occurred... Look at the console for more information!')
};


const rebuildSuccessFn = (data, textStatus, jqXHR) => {
    alert("Rebuild has started!");
    window.location.href = "/_admin_";
};


window.onload = (e) => {
    $('#deleteButton').click((e) => {
        $('button').prop('disabled', true);
        $.ajax({
            type: "POST",
            url: `/_delete_/${selectedVersion()}`,
            data: {},
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
};