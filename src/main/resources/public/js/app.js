$(document).ready(function () {

    var GRAPHS = [
        {
            id: "USDRUB",
            graph: new D3Graph('#USDRUB')
        },
        {
            id: "EURRUB",
            graph: new D3Graph('#EURRUB')
        }
    ];

    var getObsRates = function () {

        var websocketPort;
        if ((typeof window.location.port !== "undefined") && (window.location.port !== "")) {
            // test environment
            websocketPort = 8080;
        } else {
            // production environment
            websocketPort = 8000;
        }

        var rxSocket = Rx.DOM.fromWebSocket('ws://' + window.location.hostname + ':' + websocketPort + '/api/ws', null,
            Rx.Observer.create(function (e) {
                // websocket opened
            }),
            Rx.Observer.create(function (e) {
                // websocket closed
            })
        );

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

        return rxSocket
            .map(function (messageEvent) {
                return messageEvent.data;
            })
            .map(decodeRatesString);
    };

    var applyUIChange = function (rates) {
        _.each(rates, function (rate) {
            var strRateValue = rate.value.toFixed(4);
            $('#' + rate.key).find('.value').text(strRateValue);
        });
    };

    var pushGraphData = function (rates) {
        _.each(GRAPHS, function (graph) {
            _.each(rates, function (rate) {
                if (graph.id == rate.key) {
                    graph.graph.push(rate.value);
                }
            });
        });
    };

    getObsRates().subscribe(function (rates) {
        applyUIChange(rates);
        pushGraphData(rates);
    });
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
        .attr("style", "margin: 0 auto;")
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
        var dataMin = _.min(dataset, function (d) {
            return d.v;
        }).v;
        var dataMax = _.max(dataset, function (d) {
            return d.v;
        }).v;
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
        .x(function (d) {
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
            .x(function (d) {
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
