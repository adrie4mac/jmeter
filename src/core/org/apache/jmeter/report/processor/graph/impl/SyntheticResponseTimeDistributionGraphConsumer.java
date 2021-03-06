/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.jmeter.report.processor.graph.impl;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.jmeter.report.core.Sample;
import org.apache.jmeter.report.processor.ListResultData;
import org.apache.jmeter.report.processor.MapResultData;
import org.apache.jmeter.report.processor.SumAggregatorFactory;
import org.apache.jmeter.report.processor.ValueResultData;
import org.apache.jmeter.report.processor.graph.AbstractGraphConsumer;
import org.apache.jmeter.report.processor.graph.AbstractSeriesSelector;
import org.apache.jmeter.report.processor.graph.CountValueSelector;
import org.apache.jmeter.report.processor.graph.GraphKeysSelector;
import org.apache.jmeter.report.processor.graph.GroupInfo;
import org.apache.jmeter.util.JMeterUtils;

/**
 * The class SyntheticResponseTimeDistributionGraphConsumer provides a graph to visualize
 * the distribution of the response times on APDEX threshold
 *
 * @since 3.1
 */
public class SyntheticResponseTimeDistributionGraphConsumer extends
        AbstractGraphConsumer {
    private static final String FAILED_LABEL = JMeterUtils.getResString("response_time_distribution_failed_label");
    private static final MessageFormat SATISFIED_LABEL = new MessageFormat(JMeterUtils.getResString("response_time_distribution_satified_label"));
    private static final MessageFormat TOLERATED_LABEL = new MessageFormat(JMeterUtils.getResString("response_time_distribution_tolerated_label"));
    private static final MessageFormat UNTOLERATED_LABEL = new MessageFormat(JMeterUtils.getResString("response_time_distribution_untolerated_label"));

    private long satifiedThreshold;
    private long toleratedThreshold;

    private class SyntheticSeriesSelector extends AbstractSeriesSelector {
        @Override
        public Iterable<String> select(Sample sample) {
            if(!sample.getSuccess()) {
                return Arrays.asList(FAILED_LABEL);
            } else {
                long elapsedTime = sample.getElapsedTime();
                if(elapsedTime<=getSatifiedThreshold()) {
                    return Arrays.asList(SATISFIED_LABEL.format(new Object[] {Long.valueOf(getSatifiedThreshold())}));
                } else if(elapsedTime <= getToleratedThreshold()) {
                    return Arrays.asList(TOLERATED_LABEL.format(new Object[] {Long.valueOf(getSatifiedThreshold()), Long.valueOf(getToleratedThreshold())}));
                } else {
                    return Arrays.asList(UNTOLERATED_LABEL.format(new Object[] {Long.valueOf(getToleratedThreshold())}));
                }
            }
        }
    }
    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jmeter.report.csv.processor.impl.AbstractGraphConsumer#
     * createKeysSelector()
     */
    @Override
    protected final GraphKeysSelector createKeysSelector() {
        return new GraphKeysSelector() {

            @Override
            public Double select(Sample sample) {
                if(sample.getSuccess()) {
                    long elapsedTime = sample.getElapsedTime();
                    if(elapsedTime<=satifiedThreshold) {
                        return Double.valueOf(0);
                    } else if(elapsedTime <= toleratedThreshold) {
                        return Double.valueOf(1);
                    } else {
                        return Double.valueOf(2);
                    }
                } else {
                    return Double.valueOf(3);
                }
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.jmeter.report.csv.processor.impl.AbstractGraphConsumer#
     * createGroupInfos()
     */
    @Override
    protected Map<String, GroupInfo> createGroupInfos() {
        Map<String, GroupInfo> groupInfos = new HashMap<>(1);
        SyntheticSeriesSelector syntheticSeriesSelector = new SyntheticSeriesSelector();
        groupInfos.put(AbstractGraphConsumer.DEFAULT_GROUP, new GroupInfo(
                new SumAggregatorFactory(), syntheticSeriesSelector,
                // We ignore Transaction Controller results
                new CountValueSelector(true), false, false));

        return groupInfos;
    }

    @Override
    protected void initializeExtraResults(MapResultData parentResult) {
        ListResultData listResultData = new ListResultData();
        String[] messages = new String[]{
                SATISFIED_LABEL.format(new Object[] {Long.valueOf(getSatifiedThreshold())}),
                TOLERATED_LABEL.format(new Object[] {Long.valueOf(getSatifiedThreshold()), Long.valueOf(getToleratedThreshold())}),
                UNTOLERATED_LABEL.format(new Object[] {Long.valueOf(getToleratedThreshold())}),
                FAILED_LABEL
        };
        for (int i = 0; i < messages.length; i++) {
            ListResultData array = new ListResultData();
            array.addResult(new ValueResultData(Integer.valueOf(i)));
            array.addResult(new ValueResultData(messages[i]));   
            listResultData.addResult(array);
        }        
        parentResult.setResult("ticks", listResultData);
    }

    /**
     * @return the satifiedThreshold
     */
    public long getSatifiedThreshold() {
        return satifiedThreshold;
    }

    /**
     * @param satifiedThreshold the satifiedThreshold to set
     */
    public void setSatifiedThreshold(long satifiedThreshold) {
        this.satifiedThreshold = satifiedThreshold;
    }

    /**
     * @return the toleratedThreshold
     */
    public long getToleratedThreshold() {
        return toleratedThreshold;
    }

    /**
     * @param toleratedThreshold the toleratedThreshold to set
     */
    public void setToleratedThreshold(long toleratedThreshold) {
        this.toleratedThreshold = toleratedThreshold;
    }
}
