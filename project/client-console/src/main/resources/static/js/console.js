document.addEventListener('click', function (event) {
    var opener = event.target.closest('[data-dialog]');
    if (opener) {
        var dialog = document.getElementById(opener.dataset.dialog);
        if (dialog) {
            event.preventDefault();
            Object.keys(opener.dataset).forEach(function (key) {
                if (key.indexOf('set') !== 0 || key === 'set') {
                    return;
                }
                var name = key.slice(3);
                var field = dialog.querySelector('[name="' + name.charAt(0).toLowerCase() + name.slice(1) + '"]');
                if (field) {
                    field.value = opener.dataset[key];
                }
            });
            var action = opener.dataset.action;
            if (action) {
                var form = dialog.querySelector('form');
                if (form) {
                    form.action = action;
                }
            }
            dialog.showModal();
        }
        return;
    }

    var closer = event.target.closest('[data-dialog-close]');
    if (closer) {
        event.preventDefault();
        var open = closer.closest('dialog');
        if (open) {
            open.close();
        }
        return;
    }

    var row = event.target.closest('tr[data-href]');
    if (row && !event.target.closest('button, a, form, dialog')) {
        window.location.assign(row.dataset.href);
    }
});

document.addEventListener('submit', function (event) {
    var form = event.target;
    var question = form.dataset.confirm;
    if (question && !window.confirm(question)) {
        event.preventDefault();
    }
});
