$(document).ready(function () {

    var POLL_PERIOD = 5000;

    var firePeriodically = function () {
        return Rx.Observable.timer(0, POLL_PERIOD);
    };

    var pollRatesString = function () {
        return $.ajax({
            url: '/api/rates',
            dataType: 'text'
        }).promise();
    };

    var decodeRatesString = function (ratesString) {
        return _.chain(ratesString.split(';'))
            .map(function (item) {
                return item.split(':');
            })
            .map(function (pair) {
                return {
                    key: pair[0],
                    value: pair[1]
                };
            })
            .value();
    };

    var applyUIChange = function (rates) {
        _.each(rates, function (rate) {
            $('#' + rate.key).find('.value').text(rate.value);
        });
    };

    var buildCharts = function () {
        _.each($('.rate_chart'), function (rate_chart) {
            var $rate_chart = $(rate_chart);
            var chart = $rate_chart.find('.rickshaw_graph')[0];
            var legend = $rate_chart.find('.rickshaw_legend')[0];
        });
    };

    buildCharts();

    // main event chain:
    firePeriodically()
        .flatMapLatest(pollRatesString)
        .map(decodeRatesString)
        .subscribe(applyUIChange);
});
