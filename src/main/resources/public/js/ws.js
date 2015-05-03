$(document).ready(function () {

    var rxSocket = Rx.DOM.fromWebSocket('ws://' + window.location.hostname + ':8000', null,
        Rx.Observer.create(function (e) {
            console.log('opened');
        }),
        Rx.Observer.create(function (e) {
            console.log('closed');
        })
    );

    rxSocket.subscribe(function (e) {
            console.log('received: ' + e.data);
        },
        function (e) {
            console.error('error', e);
        },
        function (e) {
            console.log('closed');
        }
    );
});
