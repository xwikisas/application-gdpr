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

import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.gdpr.GDPRHelper;

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
    /**
     * The reference to the XWiki Users class, relative to the current wiki.
     */
    public static final LocalDocumentReference USERS_CLASS = new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private GDPRHelper gdprHelper;

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

        gdprHelper.secureUserProfile(userXDoc, context, false);
    }

}
