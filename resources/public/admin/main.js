const selectedVersion = () => $("#versionSelect :selected").text();

window.onload = (e) => {
    $('#updateButton').click((e) => {
        window.location.href = '/_update_'
    });

    $('#deleteButton').click((e) => {
        console.log('Delete' + selectedVersion());
        $('button').prop('disabled', true);
        $.ajax({
            type: "POST",
            url: `/_delete_/${selectedVersion()}`,
            data: {},
            success: (data, textStatus, jqXHR) => {
                window.location.href = "/_admin_"
            },
            error: (jqXHR, textStatus, errorThrown) => {
                alert('An error occurred... Look at the console (F12 or Ctrl+Shift+I, Console tab) for more information!')
            }
        });
    });

    $('#showReportLink').click((e) => {
        window.location.href = `/${selectedVersion()}/report`
    });
};