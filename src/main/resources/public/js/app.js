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
            $('#' + rate.key).text(rate.value);
        });
    };

    var buildCharts = function () {
        _.each($('.rate_chart'), function (rate_chart) {
            var $rate_chart = $(rate_chart);
            var chart = $rate_chart.find('.rickshaw_graph')[0];
            var legend = $rate_chart.find('.rickshaw_legend')[0];
            rickshaw(chart, legend)
        });
    };

    buildCharts();

    // main event chain:
    firePeriodically()
        .flatMapLatest(pollRatesString)
        .map(decodeRatesString)
        .subscribe(applyUIChange);
});

var rickshaw = function (chart, legend) {

    var seriesData = [[]];

    var random = new Rickshaw.Fixtures.RandomData(150);
    for (var i = 0; i < 150; i++) {
        random.addData(seriesData);
    }

    var graph = new Rickshaw.Graph({
        element: chart,
        width: 260,
        height: 100,
        renderer: 'line',
        stroke: true,
        preserve: true,
        series: [
            {
                color: 'black',
                data: seriesData[0],
                name: 'USDRUB'
            }
        ]
    });

    graph.render();

    new Rickshaw.Graph.HoverDetail({
        graph: graph,
        xFormatter: function (x) {
            return new Date(x * 1000).toString();
        }
    });

    new Rickshaw.Graph.Legend({
        graph: graph,
        element: legend
    });

    var ticksTreatment = 'glow';

    var xAxis = new Rickshaw.Graph.Axis.Time({
        graph: graph,
        ticksTreatment: ticksTreatment,
        timeFixture: new Rickshaw.Fixtures.Time.Local()
    });
    xAxis.render();

    var yAxis = new Rickshaw.Graph.Axis.Y({
        graph: graph,
        tickFormat: Rickshaw.Fixtures.Number.formatKMBT,
        ticksTreatment: ticksTreatment
    });
    yAxis.render();

    setInterval(function () {
        random.removeData(seriesData);
        random.addData(seriesData);
        graph.update();
    }, 5000);
};