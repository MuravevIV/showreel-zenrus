
$(document).ready(function () {

    var POLL_PERIOD = 5000;

    var firePeriodically = function () {
        return Rx.Observable.timer(0, POLL_PERIOD);
    };

    var pollRates = function () {
        return $.ajax({
            url: '/api/rates',
            dataType: 'text'
        }).promise();
    };

    var applyUIChange = function (ratesString) {

        var setUIValue = function (key, value) {
            $('#' + key).text(value);
        };

        ratesString.split(';').forEach(function (item) {
            var pair = item.split(':');
            setUIValue(pair[0], pair[1]);
        });
    };

    // main event chain:
    firePeriodically()
        .flatMapLatest(pollRates)
        .subscribe(applyUIChange);
});