/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.stats.sensors;

import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.component.Component;
import com.eucalyptus.component.Components;
import com.eucalyptus.stats.StatsOutputValues;
import com.eucalyptus.stats.SystemMetric;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Single sensor that gets metrics for all components running in the JVM without having
 * a-priori configuration of which to check.
 * <p/>
 * Generates a SystemMetric result for each found metric. Uses child-nodes in the namespace for
 * each found component. e.g. "euca.components.db", "euca.components.storage"
 */
public class ComponentsSensor implements EucalyptusStatsSensor {
    private static final Logger LOG = Logger.getLogger(ComponentsSensor.class);
    private static final String STATE_NAME = "state";
    private String sensorName = "euca.components";
    private long ttl;
    private String description;
    private List<String> tags;

    public ComponentsSensor() {
    }

    private static final Predicate<Bootstrapper> RUN_CHECK = new Predicate<Bootstrapper>() {
        @Override
        public boolean apply(@Nullable Bootstrapper bootstrapper) {
            try {
                return bootstrapper != null && bootstrapper.check();
            } catch (Throwable f) {
                LOG.trace("Bootstrapper check for " + bootstrapper.getProvides().getName() + " threw exception", f);
                return false;
            }
        }
    };

    /**
     * Returns metrics with names 'euca.components.<compname>.state
     *
     * @return
     * @throws Exception
     */
    @Override
    public List<SystemMetric> poll() throws Exception {
        List<SystemMetric> results = Lists.newArrayList();
        SystemMetric stateOutput;
        boolean result;
        String componentName;
        List<String> componentTags;
        for (Component comp : Components.listLocal()) {
            componentName = comp.getComponentId().getName();
            componentTags = new ArrayList<String>(tags.size() + 1);
            componentTags.addAll(tags);
            componentTags.add(comp.getLocalServiceConfiguration().getFullName().toString()); //Add the arn to the tags
            stateOutput = new SystemMetric(this.sensorName + "." + componentName + "." + STATE_NAME, componentTags, "Component " + componentName + " state and health checks", new HashMap<String, Object>(), ttl);

            //Check()
            try {
                LOG.trace("Running check() on component: " + componentName + " for monitoring results");
                result = Iterables.all(comp.getBootstrappers(), RUN_CHECK);
            } catch (Throwable e) {
                LOG.fatal("Component " + componentName + " Check() call threw exception. Component may not be available for use", e);
                result = false;
            }
            stateOutput.getValues().put("Check", result ? StatsOutputValues.CHECK_OK : StatsOutputValues.CHECK_FAILED);

            //Internal state
            try {
                LOG.trace("Getting state for component: " + componentName + " for monitoring results");
                stateOutput.getValues().put("State", comp.getState().toString());
            } catch (Throwable e) {
                LOG.fatal("Component " + componentName + " getState() call threw exception. Component may not be available for use", e);
            }
            results.add(stateOutput);
        }
        return results;
    }

    @Override
    public void init(String name, String description, List<String> defaultTags, long defaultTtl) throws Exception {
        //TODO: what to do with the callable/function passed in? Clean up this code
        this.sensorName = name;
        this.description = description;
        this.tags = defaultTags;
        this.ttl = defaultTtl;
    }

    @Override
    public String getName() {
        return this.sensorName;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
