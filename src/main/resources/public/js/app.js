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

    rickshaw();
});

var rickshaw = function () {

    var seriesData = [[]];

    var random = new Rickshaw.Fixtures.RandomData(150);
    for (var i = 0; i < 150; i++) {
        random.addData(seriesData);
    }

    var graph = new Rickshaw.Graph({
        element: document.getElementById("chart"),
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

    var legend = new Rickshaw.Graph.Legend({
        graph: graph,
        element: document.getElementById('legend')
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