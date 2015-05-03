$(document).ready(function () {

    var rxSocket = Rx.DOM.fromWebSocket('ws://localhost:8888', null,
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
