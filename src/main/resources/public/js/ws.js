$(document).ready(function () {

    var socket = new WebSocket("ws://localhost:8888");

    socket.onopen = function () {
        console.log("client: opened");
        // socket.send("init");
    };

    socket.onmessage = function (message) {
        if (message.data == "ping") {
            console.log("client: received ping, sending pong");
            socket.send("pong");
        }
    };

    socket.onclose = function () {
        console.log("client: closed");
    };

    socket.onerror = function (e) {
        console.log("client: error", e);
    };
});
