var rxSocket;

var requestWebsocket = function () {

    rxSocket = Rx.DOM.fromWebSocket('ws://' + window.location.hostname + ':8080/api/ws', null,
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
};

$(document).ready(function () {

    requestWebsocket();
});
