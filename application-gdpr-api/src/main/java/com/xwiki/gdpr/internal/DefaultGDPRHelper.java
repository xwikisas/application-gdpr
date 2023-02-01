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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.gdpr.GDPRConfiguration;
import com.xwiki.gdpr.GDPRHelper;

/**
 * Helper for applying rights on documents.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Singleton
public class DefaultGDPRHelper implements GDPRHelper
{
    /**
     * The groups property of the rights class.
     */
    private static final String RIGHTS_GROUPS = "groups";

    /**
     * The levels property of the rights class.
     */
    private static final String RIGHTS_LEVELS = "levels";

    /**
     * The users property of the rights class.
     */
    private static final String RIGHTS_USERS = "users";

    /**
     * The 'allow / deny' property of the rights class.
     */
    private static final String RIGHTS_ALLOWDENY = "allow";

    /**
     * The reference to the XWiki Rights class, relative to the current wiki.
     */
    private static final LocalDocumentReference RIGHTS_CLASS = new LocalDocumentReference("XWiki", "XWikiRights");

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private GDPRConfiguration gdprConfiguration;

    @Inject
    private Logger logger;

    @Override
    public void secureUserProfile(DocumentReference userDocumentReference, XWikiContext context)
    {
        try {
            XWikiDocument userXDoc = context.getWiki().getDocument(userDocumentReference, context);
            secureUserProfile(userXDoc, context, true);
        } catch (XWikiException e) {
            logger.error("Failed to fetch user document corresponding to [{}]", userDocumentReference, e);
        }
    }

    @Override
    public void secureUserProfile(XWikiDocument userXDoc, XWikiContext context, boolean saveDocument)
    {
        try {
            // Hide the document
            userXDoc.setHidden(gdprConfiguration.hideUserDocuments());

            removeRightObjects(userXDoc);

            // Add rights for the owner
            addRightsObject(userXDoc, true, userXDoc.getDocumentReference(), context);

            // Add rights for the configured groups
            for (DocumentReference groupReference : gdprConfiguration.getAllowedGroups()) {
                addRightsObject(userXDoc, false, groupReference, context);
            }

            // Save the document
            if (saveDocument && (userXDoc.isMetaDataDirty() || userXDoc.isContentDirty())) {
                String defaultMessage = "Enforced rights for GDPR compliance.";
                String message = getSaveMessage("gdpr.save.setRights", defaultMessage,
                    Arrays.asList(entityReferenceSerializer.serialize(userXDoc.getDocumentReference())));
                context.getWiki().saveDocument(userXDoc, message, false, context);
            }
        } catch (Exception e) {
            logger.error("Could not enable GDPR for user [{}]", userXDoc.getDocumentReference(), e);
        }
    }

    private void addRightsObject(XWikiDocument userXDoc, boolean isUser, EntityReference docRef, XWikiContext context)
    {
        // Create a new rights object
        BaseObject rightsObject = null;
        try {
            rightsObject = userXDoc.newXObject(RIGHTS_CLASS, context);
        } catch (XWikiException e) {
            logger.warn("Could not get rights object to start enforcing rights for GDPR compliance on user [{}]",
                userXDoc.getDocumentReference(), e);
        }

        String rightsPropertyName = RIGHTS_USERS;
        if (!isUser) {
            rightsPropertyName = RIGHTS_GROUPS;
        }

        // Set the user or group
        rightsObject.set(rightsPropertyName, entityReferenceSerializer.serialize(docRef), context);

        // Set the allow flag
        rightsObject.set(RIGHTS_ALLOWDENY, 1, context);

        // Set view and edit rights to the current user
        rightsObject.set(RIGHTS_LEVELS, "view,edit", context);
    }

    private void removeRightObjects(XWikiDocument userXDoc)
    {
        // Delete existing rights objects
        userXDoc.removeXObjects(RIGHTS_CLASS);
    }

    private String getSaveMessage(String key, String defaultMessage, List<String> params)
    {
        String message = (params == null) ? localizationManager.getTranslationPlain(key)
            : localizationManager.getTranslationPlain(key, params.toArray());
        if (message == null || message.equals(key)) {
            message = defaultMessage;
        }

        return StringUtils.abbreviate(message, 255);
    }
}
