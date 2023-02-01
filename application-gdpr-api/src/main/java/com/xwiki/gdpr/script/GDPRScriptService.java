/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.xwiki.gdpr.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service for the GDPR Application.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(GDPRScriptService.ROLE_HINT)
@Singleton
public class GDPRScriptService implements ScriptService
{
    /**
     * The name of the script service.
     */
    public static final String ROLE_HINT = "gdpr";

    /**
     * The ID of the GDPR Job.
     */
    private static final List<String> JOB_ID = Arrays.asList(GDPRScriptService.ROLE_HINT, "singleton");

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private JobStatusStore jobStatusStore;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Apply the defined GDPR security rules on user profiles.
     */
    public void protectUserProfiles()
    {
        JobStatus jobStatus = getJobStatus();

        if (jobStatus != null && jobStatus.getState().equals(State.RUNNING)) {
            logger.warn("GDPR job already running.");
            return;
        }
        if (!authorization.hasAccess(Right.ADMIN, new WikiReference(xwikiContextProvider.get().getMainXWiki()))) {
            // Only a global admin should be able to enable GDPR on the current wiki
            logger.warn("The user [{}] does not have the right to enforce GDPR compliance.",
                serializer.serialize(xwikiContextProvider.get().getUserReference()));
            return;
        }

        GDPRRequest gdprRequest = createGDPRRequest();
        try {
            this.jobExecutor.execute(GDPRScriptService.ROLE_HINT, gdprRequest);
        } catch (Exception e) {
            logger.error("Could not execute job to enable GDPR.", e);
        }
    }

    /**
     * @return the status of the GDPR job
     */
    public JobStatus getJobStatus()
    {
        return this.jobStatusStore.getJobStatus(JOB_ID);
    }

    private GDPRRequest createGDPRRequest()
    {
        GDPRRequest gdprRequest = new GDPRRequest();
        gdprRequest.setId(JOB_ID);
        gdprRequest.setInteractive(true);
        gdprRequest.setUserReference(this.documentAccessBridge.getCurrentUserReference());
        gdprRequest.setWikiReference(new WikiReference(this.documentAccessBridge.getCurrentWiki()));
        return gdprRequest;
    }

}
