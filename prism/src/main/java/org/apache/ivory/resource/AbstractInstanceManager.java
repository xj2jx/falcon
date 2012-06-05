/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ivory.resource;

import org.apache.commons.lang.StringUtils;
import org.apache.ivory.IvoryException;
import org.apache.ivory.IvoryWebException;
import org.apache.ivory.Tag;
import org.apache.ivory.entity.EntityUtil;
import org.apache.ivory.entity.parser.ValidationException;
import org.apache.ivory.entity.v0.Entity;
import org.apache.ivory.entity.v0.EntityType;
import org.apache.ivory.logging.LogProvider;
import org.apache.ivory.monitors.Dimension;
import org.apache.ivory.monitors.Monitored;
import org.apache.ivory.rerun.event.RerunEvent.RerunType;
import org.apache.ivory.rerun.event.RetryEvent;
import org.apache.ivory.rerun.handler.AbstractRerunHandler;
import org.apache.ivory.rerun.handler.RerunHandlerFactory;
import org.apache.ivory.rerun.queue.DelayedQueue;
import org.apache.ivory.workflow.engine.WorkflowEngine;
import org.apache.log4j.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public abstract class AbstractInstanceManager extends AbstractEntityManager {
    private static final Logger LOG = Logger.getLogger(AbstractInstanceManager.class);
	private AbstractRerunHandler<RetryEvent, DelayedQueue<RetryEvent>> retryHandler =  RerunHandlerFactory
			.getRerunHandler(RerunType.RETRY);

    protected void checkType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw IvoryWebException.newException("entity type is empty",
                    Response.Status.BAD_REQUEST);
        } else {
            EntityType entityType = EntityType.valueOf(type.toUpperCase());
            if (entityType == EntityType.CLUSTER) {
                throw IvoryWebException.newException("Instance management functions don't apply to Cluster entities",
                        Response.Status.BAD_REQUEST);
            }
        }
    }

    public InstancesResult getRunningInstances(String type, String entity, String colo) {
        checkColo(colo);
        checkType(type);
        try {
            validateNotEmpty("entityName", entity);
            WorkflowEngine wfEngine = getWorkflowEngine();
            Entity entityObject = EntityUtil.getEntity(type, entity);
            return wfEngine.getRunningInstances(entityObject);
        } catch (Throwable e) {
            LOG.error("Failed to get running instances", e);
            throw IvoryWebException.newException(e, Response.Status.BAD_REQUEST);
        }
    }


    public InstancesResult getStatus(String type, String entity, String startStr, String endStr,
                                            String runId, String colo) {
        checkColo(colo);
        checkType(type);
        try {
			validateParams(type, entity, startStr, endStr);

			Date start = EntityUtil.parseDateUTC(startStr);
			Date end = getEndDate(start, endStr);
			Entity entityObject = EntityUtil.getEntity(type, entity);

			WorkflowEngine wfEngine = getWorkflowEngine();
			InstancesResult result = wfEngine.getStatus(
					entityObject, start, end);
			return getInstanceWithLog(entityObject, Tag.DEFAULT,
                    runId == null ? "0" : runId, result);
		} catch (Throwable e) {
			LOG.error("Failed to get instances status", e);
			throw IvoryWebException
					.newException(e, Response.Status.BAD_REQUEST);
		}
	}

	private InstancesResult getInstanceWithLog(Entity entity,
                                               Tag type, String runId, InstancesResult result)
			throws IvoryException {
		InstancesResult.Instance[] instances = new InstancesResult.Instance[result
				.getInstances().length];
		for (int i = 0; i < result.getInstances().length; i++) {
			InstancesResult.Instance pInstance = LogProvider
					.getLogUrl(entity, result.getInstances()[i], type, runId);
			instances[i] = pInstance;
		}

		return new InstancesResult(result.getMessage(), instances);
	}

    public InstancesResult killInstance(HttpServletRequest request,
                                        String type, String entity, String startStr, String endStr, String colo) {

        checkColo(colo);
        checkType(type);
        try {
            audit(request, entity, type, "INSTANCE_KILL");
            validateParams(type, entity, startStr, endStr);
            
            Date start = EntityUtil.parseDateUTC(startStr);
            Date end = getEndDate(start, endStr);            
            Entity entityObject = EntityUtil.getEntity(type, entity);
            
            Properties props = getProperties(request);
            WorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.killInstances(entityObject, start, end, props);
        } catch (Throwable e) {
            LOG.error("Failed to kill instances", e);
            throw IvoryWebException.newException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult suspendInstance(HttpServletRequest request,
                                           String type, String entity, String startStr, String endStr, String colo) {

        checkColo(colo);
        checkType(type);
        try {
            audit(request, entity, type, "INSTANCE_SUSPEND");
            validateParams(type, entity, startStr, endStr);
            
            Date start = EntityUtil.parseDateUTC(startStr);
            Date end = getEndDate(start, endStr);            
            Entity entityObject = EntityUtil.getEntity(type, entity);
            
            Properties props = getProperties(request);
            WorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.suspendInstances(entityObject, start, end, props);
        } catch (Throwable e) {
            LOG.error("Failed to suspend instances", e);
            throw IvoryWebException.newException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult resumeInstance(HttpServletRequest request,
                                          String type, String entity, String startStr, String endStr, String colo) {

        checkColo(colo);
        checkType(type);
        try {
            audit(request, entity, type, "INSTANCE_RESUME");
            validateParams(type, entity, startStr, endStr);
            
            Date start = EntityUtil.parseDateUTC(startStr);
            Date end = getEndDate(start, endStr);            
            Entity entityObject = EntityUtil.getEntity(type, entity);
            
            Properties props = getProperties(request);
            WorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.resumeInstances(entityObject, start, end, props);
        } catch (Throwable e) {
            LOG.error("Failed to resume instances", e);
            throw IvoryWebException.newException(e, Response.Status.BAD_REQUEST);
        }
    }

    public InstancesResult reRunInstance(String type, String entity, String startStr, String endStr,
                                                HttpServletRequest request, String colo) {

        checkColo(colo);
        checkType(type);
        try {
            audit(request, entity, type, "INSTANCE_RERUN");
            validateParams(type, entity, startStr, endStr);
            
            Date start = EntityUtil.parseDateUTC(startStr);
            Date end = getEndDate(start, endStr);            
            Entity entityObject = EntityUtil.getEntity(type, entity);

            Properties props = getProperties(request);
            WorkflowEngine wfEngine = getWorkflowEngine();
            return wfEngine.reRunInstances(entityObject, start, end, props);
        } catch (Exception e) {
            LOG.error("Failed to rerun instances", e);
            throw IvoryWebException.newException(e, Response.Status.BAD_REQUEST);
        }
    }

    private Properties getProperties(HttpServletRequest request) throws IOException {
        Properties props = new Properties();
        ServletInputStream xmlStream = request==null?null:request.getInputStream();
        if (xmlStream != null) {
            if (xmlStream.markSupported()) {
                xmlStream.mark(XML_DEBUG_LEN); // mark up to debug len
            }
            props.load(xmlStream);
        }
        return props;
    }

    private Date getEndDate(Date start, String endStr) throws IvoryException {
        Date end;
        if (StringUtils.isEmpty(endStr)) {
            end = new Date(start.getTime() + 1000); // next sec
        } else
            end = EntityUtil.parseDateUTC(endStr);
        return end;
    }
    
    private void validateParams(String type, String entity, String startStr, String endStr) throws IvoryException {
        validateNotEmpty("entityType", type);
        validateNotEmpty("entityName", entity);
        validateNotEmpty("start", startStr);

        Entity entityObject = EntityUtil.getEntity(type, entity);
        validateDateRange(entityObject, startStr, endStr);
    }

    private void validateDateRange(Entity entity, String start, String end) throws IvoryException {
        IvoryException firstException = null;
        boolean valid = false;
        for (String cluster : entity.getClustersDefined()) {
            try {
                validateDateRangeFor(entity, cluster, start, end);
                valid = true;
                break;
            } catch (IvoryException e) {
                if (firstException == null) firstException = e;
            }
        }
        if (!valid && firstException != null) throw firstException;

    }

    private void validateDateRangeFor(Entity entity, String cluster, String start, String end) throws IvoryException {
        Date clusterStart = EntityUtil.getStartTime(entity, cluster);
        Date clusterEnd = EntityUtil.getEndTime(entity, cluster);

        Date instStart = EntityUtil.parseDateUTC(start);
        if(instStart.before(clusterStart))
            throw new ValidationException("Start date " + start +
                    " is before" + entity.getEntityType() + "  start " + EntityUtil.formatDateUTC(clusterStart));

        if(StringUtils.isNotEmpty(end)) {
            Date instEnd = EntityUtil.parseDateUTC(end);
            if(instStart.after(instEnd))
                throw new ValidationException("Start date " + start + " is after end date " + end);

            if(instEnd.after(clusterEnd))
                throw new ValidationException("End date " + end + " is after " + entity.getEntityType() + " end " +
                        EntityUtil.formatDateUTC(clusterEnd));
        } else if(instStart.after(clusterEnd))
            throw new ValidationException("Start date " + start + " is after " + entity.getEntityType() + " end " +
                    EntityUtil.formatDateUTC(clusterEnd));
    }

    private void validateNotEmpty(String field, String param) throws ValidationException {
        if (StringUtils.isEmpty(param))
            throw new ValidationException("Parameter " + field + " is empty");
    }
    
	/*
	 * Below method is a mock and gets automatically invoked by Aspect
	 */
	// TODO capture execution time
	@Monitored(event = "process-instance")
	public String instrumentWithAspect(
			@Dimension(value = "process") String process,
			@Dimension(value = "feed") String feedName,
			@Dimension(value = "feedPath") String feedpath,
			@Dimension(value = "nominalTime") String nominalTime,
			@Dimension(value = "timeStamp") String timeStamp,
			@Dimension(value = "status") String status,
			@Dimension(value = "workflowId") String workflowId,
			@Dimension(value = "runId") String runId, long msgReceivedTime)
			throws Exception {
		if (status.equalsIgnoreCase("FAILED")) {
			retryHandler.handleRerun(process, nominalTime, runId, workflowId,
                    getWorkflowEngine(), msgReceivedTime);
			throw new Exception(process + ":" + nominalTime + " Failed");
		}
		return "DONE";

	}
}
