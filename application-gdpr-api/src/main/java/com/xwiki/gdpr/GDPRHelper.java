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
package com.xwiki.gdpr;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Helper for securing profiles using GDPR configuration.
 *
 * @version $Id$
 * @since 2.0
 */
@Role
public interface GDPRHelper
{
    /**
     * Apply the configured GDPR policy on the given user profile.
     *
     * @param userDocumentReference the user profile
     * @param context the XWiki context
     */
    void secureUserProfile(DocumentReference userDocumentReference, XWikiContext context);

    /**
     * Apply the configured GDPR policy on the given user profile.
     *
     * @param userXDoc the user document
     * @param context the XWiki context
     * @param saveDocument whether or not the document should be saved
     */
    void secureUserProfile(XWikiDocument userXDoc, XWikiContext context, boolean saveDocument);
}
