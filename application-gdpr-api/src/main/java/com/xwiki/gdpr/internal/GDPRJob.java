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

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xwiki.gdpr.GDPRHelper;
import com.xwiki.gdpr.script.GDPRScriptService;

/**
 * Job that secures user profiles applying the configured GDPR security rules.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named(GDPRScriptService.ROLE_HINT)
public class GDPRJob extends AbstractJob<GDPRRequest, DefaultJobStatus<GDPRRequest>>
{
    private static final String XWQL_USER_QUERY = "from doc.object(XWiki.XWikiUsers) as user";

    @Inject
    private WikiDescriptorManager wikiDescriptor;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    private GDPRHelper gdprHelper;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Override
    public String getType()
    {
        return GDPRScriptService.ROLE_HINT;
    }

    @Override
    protected void runInternal() throws Exception
    {
        // Get wiki IDs
        Collection<String> wikiIDs = wikiDescriptor.getAllIds();
        
        // Start progress
        this.progressManager.pushLevelProgress(wikiIDs.size(), this);

        XWikiContext context = xwikiContextProvider.get();
        WikiReference currentWiki = this.request.getWikiReference();
        
        // Get current wiki ID
        String currentWikiId = currentWiki.getName();

        // Enable GDPR complience on all wikis, one by one
        for (String wikiId : wikiIDs) {
            enableGDPROnWiki(wikiId, context);
        }

        // At the end, get back to the current wiki
        context.setWikiId(currentWikiId);

        // Stop progress
        this.progressManager.popLevelProgress(this);
    }

    private void enableGDPROnWiki(String wikiId, XWikiContext context)
    {
        // Start step
        this.progressManager.startStep(this);
        
        logger.info("Enforcing GDPR complience on wiki [{}]", wikiId);
        
        // Set wiki
        context.setWikiId(wikiId);
        WikiReference wikiReference = new WikiReference(wikiId);

        try {
            // Search for local user profiles in batches of 1000 users
            int limit = 1000;
            int offset = 0;
            Query countQuery = this.queryManager.createQuery(XWQL_USER_QUERY, Query.XWQL);
            long size = (long) countQuery.addFilter(countFilter).setWiki(wikiId).execute().get(0);
            
            while (offset < size) {
                Query query = this.queryManager.createQuery(XWQL_USER_QUERY, Query.XWQL);
                List<String> userList = query.setWiki(wikiId).setOffset(offset).setLimit(limit).execute();

                // Secure the user profiles
                for (String user : userList) {
                    DocumentReference userReference =
                        documentReferenceResolver.resolve(user).setWikiReference(wikiReference);
                    logger.debug("Updating the profile page of [{}]", userReference);
                    gdprHelper.secureUserProfile(userReference, context);
                }

                offset += limit;
            }
        } catch (Exception e) {
            logger.error("Could enable GDPR on wiki {}", wikiId, e);
        }
        
        // End step
        this.progressManager.endStep(this);
    }
}
