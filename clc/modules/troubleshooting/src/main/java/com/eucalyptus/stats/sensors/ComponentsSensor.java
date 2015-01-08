/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
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
    private static final long DEFAULT_TTL_SEC = 30;
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
