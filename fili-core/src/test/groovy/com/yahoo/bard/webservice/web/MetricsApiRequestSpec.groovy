// Copyright 2016 Yahoo Inc.
// Licensed under the terms of the Apache license. Please see LICENSE file distributed with this work for terms.
package com.yahoo.bard.webservice.web

import com.yahoo.bard.webservice.application.JerseyTestBinder
import com.yahoo.bard.webservice.data.metric.MetricDictionary
import com.yahoo.bard.webservice.web.endpoints.MetricsServlet

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class MetricsApiRequestSpec extends Specification {

    JerseyTestBinder jtb

    @Shared
    MetricDictionary fullDictionary

    @Shared
    MetricDictionary emptyDictionary = new MetricDictionary()

    def setup() {
        jtb = new JerseyTestBinder(MetricsServlet.class)
        fullDictionary = jtb.configurationLoader.metricDictionary
    }

    def cleanup() {
        jtb.tearDown()
    }

    def "check api request construction for the top level endpoint (all tables)"() {
        when:
        MetricsApiRequest apiRequest = new MetricsApiRequest(
                null,
                null,
                "",
                "",
                fullDictionary,
                null
        )

        then:
        apiRequest.getMetrics() as Set == fullDictionary.values() as Set
    }

    def "check api request construction for a given table name"() {
        setup:
        String name = "height"

        when:
        MetricsApiRequest apiRequest = new MetricsApiRequest(
                name,
                null,
                "",
                "",
                fullDictionary,
                null
        )

        then:
        apiRequest.getMetric() == fullDictionary.get(name)
    }

    @Unroll
    def "api request construction throws #exception.simpleName because #reason"() {
        when:
        new MetricsApiRequest(
                name,
                null,
                "",
                "",
                dictionary,
                null
        )

        then:
        Exception e = thrown(exception)
        e.getMessage().matches(reason)

        where:
        name     | dictionary      | exception              | reason
        "height" | emptyDictionary | BadApiRequestException | ".*Metric Dictionary is empty.*"
        "weight" | fullDictionary  | BadApiRequestException | ".*Metric.*do not exist.*"
    }
}
