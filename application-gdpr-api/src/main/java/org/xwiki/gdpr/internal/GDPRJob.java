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

package org.xwiki.gdpr.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 */
@Component
@Named(GDPRScriptService.ROLE_HINT)
public class GDPRJob extends AbstractJob<GDPRRequest, DefaultJobStatus<GDPRRequest>>
{
    
    private static final LocalDocumentReference ADMIN_GROUP_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiAdminGroup");

    /**
     * The reference to the XWiki Rights class, relative to the current wiki.
     */
    public static final LocalDocumentReference RIGHTS_CLASS = new LocalDocumentReference("XWiki", "XWikiRights");

    /**
     * The groups property of the rights class.
     */
    public static final String RIGHTS_GROUPS = "groups";

    /**
     * The levels property of the rights class.
     */
    public static final String RIGHTS_LEVELS = "levels";

    /**
     * The users property of the rights class.
     */
    public static final String RIGHTS_USERS = "users";

    /**
     * The 'allow / deny' property of the rights class.
     */
    public static final String RIGHTS_ALLOWDENY = "allow";

    @Inject
    private WikiDescriptorManager wikiDescriptor;

    @Inject
    protected DocumentReferenceResolver<String> defaultDocRefResolver;

    @Inject @Named("local")
    protected EntityReferenceSerializer<String> localSerializer;

    @Inject
    private QueryManager queryManager;
    
    /**
     * The query filter used to count the documents from the database.
     */
    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    protected Execution execution;

    @Inject
    protected Provider<XWikiContext> xwikiContextProvider;

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
        
        logger.info("Enforcing GDPR complience on wiki {}", wikiId);
        
        // Set wiki
        context.setWikiId(wikiId);

        try {
            // Search for local user profiles in batches of 1000 users
            int limit = 1000;
            int offset = 0;
            Query countQuery = this.queryManager.createQuery("from doc.object(XWiki.XWikiUsers) as user", Query.XWQL);
            long size = (long) countQuery.addFilter(countFilter).setWiki(wikiId).execute().get(0);
            
            while(offset < size) {
              Query query = this.queryManager.createQuery("from doc.object(XWiki.XWikiUsers) as user", Query.XWQL);
              List<String> userList = query.setWiki(wikiId).setOffset(offset).setLimit(limit).execute();

              // Secure the user profiles
              for (String user : userList) {
                  //logger.debug("Updating the profile page of {}:{}", wikiId, user);
                  secureUserProfile(user, context);
              }
              
              offset+= limit;
            }
        } catch (Exception e) {
            logger.error("Could enable GDPR on wiki {}", wikiId, e);
        }
        
        // End step
        this.progressManager.endStep(this);
    }

    private void secureUserProfile(String user, XWikiContext context) throws XWikiException
    {
        try {
            DocumentReference userDocRef = this.defaultDocRefResolver.resolve(user, context.getWikiReference());
            XWikiDocument userXDoc = context.getWiki().getDocument(userDocRef, context);

            // Hide the document
            userXDoc.setHidden(true);

            // Delete existing rights objects
            userXDoc.removeXObjects(RIGHTS_CLASS);

            // Add rights for the owner
            addRightsObject(userDocRef, true, userXDoc, context);

            // Add rights for the (local) admin group
            addRightsObject(ADMIN_GROUP_REFERENCE, false, userXDoc, context);

            // Save the document
            String defaultMessage = "Enforced rights for GDPR compliance.";
            String message = getMessage("gdpr.save.setRights", defaultMessage,
                Arrays.asList(localSerializer.serialize(userDocRef).toString()));
            context.getWiki().saveDocument(userXDoc, message, false, context);
        } catch (Exception e) {
            logger.warn("Could not enable GDPR for user {}", user, e);
        }
    }
    
    private void addRightsObject(EntityReference docRef, boolean isUser, XWikiDocument userXDoc, XWikiContext context)
        throws XWikiException
    {
        // Create a new rights object
        BaseObject rightsObject = userXDoc.newXObject(RIGHTS_CLASS, context);
        
        // Set user / group
        String rightsPropertyName = RIGHTS_USERS;
        if (!isUser) {
            rightsPropertyName = RIGHTS_GROUPS;
        }
        rightsObject.set(rightsPropertyName, localSerializer.serialize(docRef).toString(), context);

        // Set the allow flag
        rightsObject.set(RIGHTS_ALLOWDENY, 1, context);

        // Set view and edit rights to the current user
        rightsObject.set(RIGHTS_LEVELS, "view,edit", context);
    }

    protected String getMessage(String key, String defaultMessage, List<String> params)
    {
        String message = (params == null) ? localizationManager.getTranslationPlain(key)
            : localizationManager.getTranslationPlain(key, params.toArray());
        if (message == null || message.equals(key)) {
            message = defaultMessage;
        }
        // Trim the message, whichever that is, to 255 characters
        if (message.length() > 255) {
            // Add some dots to show that it was trimmed
            message = message.substring(0, 252) + "...";
        }
        return message;
    }

}
