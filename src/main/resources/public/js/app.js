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
                    value: parseFloat(pair[1])
                };
            })
            .value();
    };

    var applyUIChange = function (rates) {
        _.each(rates, function (rate) {
            var strRateValue = rate.value.toFixed(4);
            $('#' + rate.key).find('.value').text(strRateValue);
        });
    };

    var buildPlots = function () {
        var plots = _.map($('.rate_chart'), function (rate_chart) {
            var $rate_chart = $(rate_chart);
            var id = $rate_chart.attr('id');
            var chart = $rate_chart.find('.rickshaw_graph')[0];
            var legend = $rate_chart.find('.rickshaw_legend')[0];
            return {
                id: id,
                graph: new Graph(chart, legend)
            };
        });
        return Rx.Observable.just(plots);
    };

    var updatePlotRates = function (plots, rates) {
        _.each(plots, function (plot) {
            _.each(rates, function (rate) {
                if (plot.id == rate.key) {
                    plot.graph.continueData(rate.value);
                }
            });
        });
    };

    var obsRates = firePeriodically()
        .flatMapLatest(pollRatesString)
        .map(decodeRatesString);

    obsRates
        .subscribe(applyUIChange);

    /*
     buildPlots()
     .subscribe(function (plots) {
     obsRates.subscribe(function (rates) {
     updatePlotRates(plots, rates);
     });
     });
     */

    var graphs = [
        {
            id: "USDRUB",
            graph: new D3Graph('#USDRUB')
        },
        {
            id: "EURRUB",
            graph: new D3Graph('#EURRUB')
        }
    ];

    obsRates
        .subscribe(function (rates) {
            _.each(graphs, function (graph) {
                _.each(rates, function (rate) {
                    if (graph.id == rate.key) {
                        graph.graph.push(rate.value);
                    }
                });
            });
        });



    // WebSocket
    (function () {

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
    })();
});

var D3Graph = function (selector) {

    var baseWidth = 300;
    var baseHeight = 140;

    var margin = {top: 0, right: 15, bottom: 20, left: 50};

    var width = baseWidth - margin.left - margin.right;
    var height = baseHeight - margin.top - margin.bottom;

    var svg = d3.select(selector).select(".rickshaw_graph")
        .append("svg")
        .attr("width", baseWidth)
        .attr("height", baseHeight)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    //

    var now = new Date();

    var dataset = [];

    var maxDatasetSize = 15;

    var getYDomain = function (dataset) {
        if (dataset.length == 0) {
            return {
                min: 0,
                max: 100
            };
        }
        var goldenRatio = 1.6180;
        var dataMin = _.min(dataset, function (d) { return d.v; }).v;
        var dataMax = _.max(dataset, function (d) { return d.v; }).v;
        if (dataMin == dataMax) {
            dataMin -= 1;
            dataMax += 1;
        }
        var dataHeight = dataMax - dataMin;
        var fullHeight = dataHeight * goldenRatio;
        var padding = (fullHeight - dataHeight) / 2;
        return {
            min: dataMin - padding,
            max: dataMax + padding
        };
    };

    //

    var xScale = d3.time.scale()
        .domain([d3.time.minute.offset(now, -1), now])
        .range([0, width]);

    var yDomain = getYDomain(dataset);

    var yScale = d3.scale.linear()
        .domain([yDomain.min, yDomain.max])
        .range([height, 0]);

    var xAxis = d3.svg.axis()
        .scale(xScale)
        .orient("bottom")
        .ticks(5);

    var yAxis = d3.svg.axis()
        .scale(yScale)
        .orient("left")
        .ticks(5);

    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(xAxis);

    svg.append("g")
        .attr("class", "y axis")
        .attr("transform", "translate(0,0)")
        .call(yAxis);

    //

    svg.append("clipPath")
        .attr("id", "graph-area")
        .append("rect")
        .attr("x", 0)
        .attr("y", 0)
        .attr("width", width)
        .attr("height", height);

    var lineFunction = d3.svg.line()
        .x(function (d, i) {
            return xScale(d.t);
        })
        .y(function (d) {
            return yScale(d.v);
        })
        .interpolate("linear");

    svg.append("path")
        .attr("clip-path", "url(#graph-area)")
        .attr("class", "line")
        .attr("stroke", "blue")
        .attr("stroke-width", 2)
        .attr("fill", "none")
        .attr("d", lineFunction(dataset));

    var redraw = function () {

        var now = new Date();

        xScale.domain([d3.time.minute.offset(now, -1), now]);

        var yDomain = getYDomain(dataset);
        yScale.domain([yDomain.min, yDomain.max]);

        var lineFunction = d3.svg.line()
            .x(function (d, i) {
                return xScale(d.t);
            })
            .y(function (d) {
                return yScale(d.v);
            })
            .interpolate("linear");

        svg.select(".line")
            .attr("d", lineFunction(dataset));

        svg.select(".x.axis")
            .transition()
            .call(xAxis);

        svg.select(".y.axis")
            .transition()
            .call(yAxis);
    };

    this.push = function (newValue) {
        var now = new Date();
        dataset.push({
            t: now,
            v: newValue
        });
        if (dataset.length > maxDatasetSize) {
            dataset.shift();
        }
        redraw();
    };
};
