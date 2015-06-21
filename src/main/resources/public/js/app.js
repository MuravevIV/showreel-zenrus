$(document).ready(function () {

    new (function () {

        var MAX_MIN_BACK = 1440;
        var SAMPLE_PERIOD = 200;

        var $slider = $("#slider");

        var fSliderValMin = 0;
        var fSliderValMax = 100;

        $slider.noUiSlider({
            start: fSliderValMax,
            connect: false,
            range: {
                'min': fSliderValMin,
                'max': fSliderValMax
            }
        });

        var scale = d3.scale.log();

        var logValMax = scale(MAX_MIN_BACK);

        var sliderValToLog = function (sliderVal) {
            return logValMax - (sliderVal / fSliderValMax) * logValMax;
        };

        var obsMinBack = Rx.Observable.fromEvent($(document), "click mousemove")
            .sample(SAMPLE_PERIOD)
            .map(function () {
                return $slider.val();
            })
            .distinctUntilChanged()
            .map(parseFloat)
            .map(sliderValToLog)
            .map(scale.invert);

        obsMinBack.subscribe(function (n) {
            ui.setGraphsMinBack(n);
        });
    })();

    var ui = new (function () {

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

        this.setGraphsTimeshift = function (tsTimeshift) {
            _.each(GRAPHS, function (g) {
                g.graph.setTimeshift(tsTimeshift);
            });
        };

        this.setGraphsMinBack = function (minBack) {
            _.each(GRAPHS, function (g) {
                g.graph.setMinBack(minBack);
            });
        };

        this.changeValues = function (rates) {
            _.each(rates, function (rate) {
                var strRateValue = rate.value.toFixed(4);
                $('#' + rate.key).find('.value').text(strRateValue);
            });
        };

        this.pushGraphData = function (coupledRates) {
            _.each(GRAPHS, function (graph) {
                _.each(coupledRates, function (rate) {
                    if (graph.id == rate.key) {
                        graph.graph.push(rate);
                    }
                });
            });
        };

        this.pushGraphDataList = function (ratesLists) {
            _.each(GRAPHS, function (graph) {
                _.each(ratesLists, function (ratesList, key) {
                    if (graph.id == key) {
                        graph.graph.pushList(ratesList);
                    }
                });
            });
        };
    })();

    var eventPipes = new (function () {

        var Message = function () {
            //
        };
        Message.Type = {
            RATES: 0,
            RATES_COLLECTION: 1,
            SERVER_TIMESTAMP: 2,
            RATES_LATEST: 3
        };
        Message.getType = function (message) {
            return message.charCodeAt(0);
        };
        Message.getBody = function (message) {
            return message.substring(1);
        };

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
                console.trace('websocket opened');
            }),
            Rx.Observer.create(function (e) {
                console.trace('websocket closed');
            })
        );

        var rxMessageStream = rxSocket
            .map(function (messageEvent) {
                return messageEvent.data;
            });

        var rxMessageStreamHot = rxMessageStream.publish();

        var decodeRatesMessage = function (rates) {
            var timstamedPair = rates.split('!');
            var ratesTimestamp = timstamedPair[0];
            var ratesValue = timstamedPair[1];
            return _.chain(ratesValue.split(';'))
                .map(function (item) {
                    return item.split(':');
                })
                .map(function (pair) {
                    return {
                        ts: parseInt(ratesTimestamp),
                        key: pair[0],
                        value: parseFloat(pair[1])
                    };
                })
                .value();
        };

        this.obsRates = rxMessageStreamHot
            .filter(function (message) {
                return (Message.getType(message) === Message.Type.RATES);
            })
            .map(function (message) {
                return Message.getBody(message);
            })
            .map(decodeRatesMessage);

        this.obsRatesCollection = rxMessageStreamHot
            .filter(function (message) {
                return (Message.getType(message) === Message.Type.RATES_COLLECTION);
            })
            .map(function (message) {
                return Message.getBody(message);
            })
            .filter(function (ratesCollectionString) {
                return (ratesCollectionString.length > 0);
            })
            .map(function (ratesCollectionString) {
                return ratesCollectionString.split("|");
            })
            .map(function (ratesString) {
                return _.map(ratesString, decodeRatesMessage);
            })
            .map(function (coupledRatesList) {
                return _.chain(coupledRatesList)
                    .flatten()
                    .groupBy(function (item) {
                        return item.key;
                    })
                    .value();
            });

        this.obsTimeshift = rxMessageStreamHot
            .filter(function (message) {
                return (Message.getType(message) === Message.Type.SERVER_TIMESTAMP);
            })
            .map(function (message) {
                return Message.getBody(message);
            })
            .map(function (strServerTimestamp) {
                return parseInt(strServerTimestamp);
            })
            .map(function (serverTimestamp) {
                var clientTimestamp = (new Date()).getTime();
                return clientTimestamp - serverTimestamp;
            });

        this.obsRatesLatest = rxMessageStreamHot
            .filter(function (message) {
                return (Message.getType(message) === Message.Type.RATES_LATEST);
            })
            .map(function (message) {
                return Message.getBody(message);
            })
            .map(decodeRatesMessage);

        this.start = function () {
            rxMessageStreamHot.connect();
        }
    })();

    eventPipes.obsRates.subscribe(ui.changeValues);
    eventPipes.obsRates.subscribe(ui.pushGraphData);
    eventPipes.obsRatesCollection.subscribe(ui.pushGraphDataList);
    eventPipes.obsTimeshift.subscribe(ui.setGraphsTimeshift);
    eventPipes.obsRatesLatest.subscribe(ui.changeValues);

    eventPipes.start();
});

var D3Graph = function (selector) {

    var margin = {top: 0, right: 15, bottom: 20, left: 50};

    var _baseWidth;
    var _baseHeight;
    var _width;
    var _height;

    var updateDimensions = function () {
        var g = $(selector).find(".rickshaw_graph");
        _baseWidth = g.width();
        _baseHeight = g.height();
        _width = _baseWidth - margin.left - margin.right;
        _height = _baseHeight - margin.top - margin.bottom;
    };

    updateDimensions();

    Rx.Observable.fromEvent($(window), "resize")
        .startWith('initial event')
        .map(function () {
            var w = $(window);
            return [w.width(), w.height()];
        })
        .sample(250)
        .subscribe(function () {
            updateDimensions();
            redraw();
        });

    var svg = d3.select(selector).select(".rickshaw_graph")
        .append("svg")
        .attr("width", _baseWidth)
        .attr("height", _baseHeight)
        .attr("style", "margin: 0 auto;")
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    //

    var now = new Date();

    var _tsTimeshift = 0;
    var _minBack = 1;

    var dataset = [];

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
        .domain([d3.time.minute.offset(now, -_minBack), now])
        .range([0, _width]);

    var yDomain = getYDomain(dataset);

    var yScale = d3.scale.linear()
        .domain([yDomain.min, yDomain.max])
        .range([_height, 0]);

    var customTimeFormat = d3.time.format.multi([
        ["%Lms", function(d) { return d.getMilliseconds(); }],
        ["%Ss", function(d) { return d.getSeconds(); }],
        ["%H:%M", function(d) { return d.getMinutes(); }],
        ["%H", function(d) { return d.getHours(); }],
        ["%a %d", function(d) { return d.getDay() && d.getDate() != 1; }],
        ["%b %d", function(d) { return d.getDate() != 1; }],
        ["%B", function(d) { return d.getMonth(); }],
        ["%Y", function() { return true; }]
    ]);

    var xAxis = d3.svg.axis()
        .scale(xScale)
        .orient("bottom")
        .tickFormat(customTimeFormat)
        .ticks(5);

    var yAxis = d3.svg.axis()
        .scale(yScale)
        .orient("left")
        .ticks(5);

    svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + _height + ")")
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
        .attr("width", _width)
        .attr("height", _height);

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

        d3.select(selector).select(".rickshaw_graph").select("svg")
            .attr("width", _baseWidth)
            .attr("height", _baseHeight);

        xScale.range([0, _width]);
        xScale.domain([d3.time.minute.offset(now, -_minBack), now]);

        yScale.range([_height, 0]);
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
            .transition()
            .attr("d", lineFunction(dataset));

        svg.select(".x.axis")
            .transition()
            .call(xAxis);

        svg.select(".y.axis")
            .transition()
            .call(yAxis);
    };

    var pushList = this.pushList = function (ratesList) {
        ratesList.forEach(function (rate) {
            dataset.push({
                ts: rate.ts,
                t: new Date(rate.ts + _tsTimeshift),
                v: rate.value
            });
        });
        redraw();
    };

    this.push = function (rate) {
        pushList([rate]);
    };

    this.setTimeshift = function (tsTimeshift) {
        _tsTimeshift = tsTimeshift;
        _.each(dataset, function (d) {
            d.t = new Date(d.ts + tsTimeshift);
        });
        redraw();
        console.log('Graph ' + selector + ' - set timeshift ' + tsTimeshift);
    };


    this.setMinBack = function (minBack) {
        _minBack = minBack;
        redraw();
    };
};
