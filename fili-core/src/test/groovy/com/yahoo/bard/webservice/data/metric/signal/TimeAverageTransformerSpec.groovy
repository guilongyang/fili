// Copyright 2020 Oath Inc.
// Licensed under the terms of the Apache license. Please see LICENSE.md file distributed with this work for terms.
package com.yahoo.bard.webservice.data.metric.signal

import static com.yahoo.bard.webservice.data.metric.signal.TimeAverageTransformer.BASE_SIGNAL

import com.yahoo.bard.webservice.data.config.metric.makers.LongSumMaker
import com.yahoo.bard.webservice.data.config.metric.makers.MetricMaker
import com.yahoo.bard.webservice.data.config.metric.makers.ThetaSketchMaker
import com.yahoo.bard.webservice.data.metric.LogicalMetric
import com.yahoo.bard.webservice.data.metric.LogicalMetricImpl
import com.yahoo.bard.webservice.data.metric.LogicalMetricInfo
import com.yahoo.bard.webservice.data.metric.MetricDictionary
import com.yahoo.bard.webservice.data.metric.TemplateDruidQuery
import com.yahoo.bard.webservice.data.time.DefaultTimeGrain
import com.yahoo.bard.webservice.druid.model.aggregation.DoubleSumAggregation
import com.yahoo.bard.webservice.druid.model.aggregation.LongSumAggregation
import com.yahoo.bard.webservice.druid.model.postaggregation.ConstantPostAggregation
import com.yahoo.bard.webservice.druid.model.postaggregation.PostAggregation
import com.yahoo.bard.webservice.druid.util.FieldConverterSupplier
import com.yahoo.bard.webservice.druid.util.ThetaSketchFieldConverter

import spock.lang.Specification
import spock.lang.Unroll

class TimeAverageTransformerSpec extends Specification {

    @Unroll
    def "Create a time average for #grain"() {
        setup:
        FieldConverterSupplier.sketchConverter = new ThetaSketchFieldConverter()
        TimeAverageTransformer timeAverageTransformer = new TimeAverageTransformer()

        MetricDictionary metricDictionary = new MetricDictionary();
        MetricMaker maker = new LongSumMaker(metricDictionary)
        MetricMaker sketchMaker = new ThetaSketchMaker(metricDictionary, 128)

        LogicalMetric longSum = maker.make("foo", "bar")
        LogicalMetric sketchUnion = sketchMaker.make("foo", "bar")
        Map params = [(BASE_SIGNAL): value]

        LogicalMetric averageMetric = timeAverageTransformer.apply(longSum, BASE_SIGNAL, params)
        LogicalMetric sketchAverage = timeAverageTransformer.apply(sketchUnion, BASE_SIGNAL, params)

        expect:
        TemplateDruidQuery innerQuery = averageMetric.templateDruidQuery.getInnermostQuery()
        innerQuery.getMetricField("foo") instanceof LongSumAggregation
        ((LongSumAggregation) innerQuery.getMetricField("foo")).fieldName == "bar"
        innerQuery.getMetricField("one") instanceof ConstantPostAggregation
        ((ConstantPostAggregation) innerQuery.getMetricField("one")).value == 1
        TemplateDruidQuery outerQuery = averageMetric.templateDruidQuery
        outerQuery.getMetricField("foo") instanceof LongSumAggregation

        sketchAverage.templateDruidQuery.getMetricField("foo_estimate_sum") instanceof DoubleSumAggregation
        ((DoubleSumAggregation) sketchAverage.
                templateDruidQuery.
                getMetricField("foo_estimate_sum")).fieldName == "foo_estimate"

        where:
        value      | grain                  | longNameBase
        "dayAvg"   | DefaultTimeGrain.DAY   | "Daily Average"
        "weekAvg"  | DefaultTimeGrain.WEEK  | "Weekly Average"
        "monthAvg" | DefaultTimeGrain.MONTH | "Monthly Average"
    }

    @Unroll
    def "LogicalName transformer"() {
        setup:
        TimeAverageTransformer timeAverageTransformer = new TimeAverageTransformer()
        MetricDictionary metricDictionary = new MetricDictionary();

        FieldConverterSupplier.sketchConverter = new ThetaSketchFieldConverter()
        String longName = "foo (" + longNameBase + ")"

        MetricMaker maker = new LongSumMaker(metricDictionary)
        LogicalMetric longSum = maker.make("foo", "bar")
        Map params = [(BASE_SIGNAL): value]

        LogicalMetricInfo info = timeAverageTransformer.makeNewLogicalMetricInfo(longSum.logicalMetricInfo, value)

        expect:
        info.getName().startsWith(value)
        info.getLongName() == longName
        info.getDescription().contains(longNameBase)

        where:
        value      | grain                  | longNameBase
        "dayAvg"   | DefaultTimeGrain.DAY   | "Daily Average"
        "weekAvg"  | DefaultTimeGrain.WEEK  | "Weekly Average"
        "monthAvg" | DefaultTimeGrain.MONTH | "Monthly Average"
    }
}
