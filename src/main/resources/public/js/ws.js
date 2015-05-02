$(document).ready(function () {

    var rxSocket = Rx.DOM.fromWebSocket('ws://localhost:8888', null,
        Rx.Observer.create(function (e) {
            console.log("client: opened");
        }),
        Rx.Observer.create(function (e) {
            console.log("client: closed");
        })
    );

    rxSocket.subscribe(function (e) {
            if (e.data == "ping") {
                console.log("client: received ping, sending pong");
                rxSocket.onNext("pong");
            }
        },
        function (e) {
            console.error('client: error', e);
        },
        function (e) {
            console.info('client: closed');
        }
    );
});
