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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * GDPR rights enforcer listener, to handle rights restriction on new user profile pages.
 * 
 * @version $Id$
 */
@Component
@Named("RightsEnforcerListener")
@Singleton
public class RightsEnforcerListener implements EventListener
{

    @Inject
    private Logger logger;

    @Inject
    protected EntityReferenceSerializer<String> stringSerializer;

    private static final LocalDocumentReference ADMIN_GROUP_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiAdminGroup");

    /**
     * The reference to the XWiki Users class, relative to the current wiki.
     */
    public static final LocalDocumentReference USERS_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

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

    @Override
    public String getName()
    {
        return "RightsEnforcerListener";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new DocumentCreatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;

        XWikiDocument userXDoc = (XWikiDocument) source;
        DocumentReference userDocRef = userXDoc.getDocumentReference();

        // Go ahead and work if we have a User Class object in this document
        BaseObject userObject = userXDoc.getXObject(USERS_CLASS);
        if (userObject == null) {
            // there is no user object, return
            return;
        }
        
        // Hide the document
        userXDoc.setHidden(true);

        // Delete existing rights objects
        userXDoc.removeXObjects(RIGHTS_CLASS);

        addRightsObject(userXDoc, true, userDocRef, context);

        addRightsObject(userXDoc, false, ADMIN_GROUP_REFERENCE, context);
    }

    private void addRightsObject(XWikiDocument userXDoc, boolean isUser, EntityReference docRef, XWikiContext context)
    {
        // Create a new rights object
        BaseObject rightsObject = null;
        try {
            rightsObject = userXDoc.newXObject(RIGHTS_CLASS, context);
        } catch (XWikiException e) {
            logger.warn(String.format(
                "Could not get rights object to start enforcing rights for GDPR compliance on user %s", userXDoc), e);
        }

        String rightsPropertyName = RIGHTS_USERS;
        if (!isUser) {
            rightsPropertyName = RIGHTS_GROUPS;
        }

        // Set the user
        rightsObject.set(rightsPropertyName, stringSerializer.serialize(docRef).toString(), context);

        // Set the allow flag
        rightsObject.set(RIGHTS_ALLOWDENY, 1, context);

        // Set view and edit rights to the current user
        rightsObject.set(RIGHTS_LEVELS, "view,edit", context);
    }

}
