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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xwiki.gdpr.GDPRConfiguration;
import com.xwiki.gdpr.GDPRException;

/**
 * Default implementation of {@link GDPRConfiguration}.
 *
 * @version $Id$
 * @since 2.0
 */
@Component
@Singleton
public class DefaultGDPRConfiguration implements GDPRConfiguration
{
    private static final List<String> GDPR_SPACE = Arrays.asList("GDPR", "Code");

    private static final String HIDE_USER_DOCUMENTS = "hideUserDocuments";

    private static final String ALLOWED_GROUPS = "allowedGroups";

    private static final LocalDocumentReference GDPR_CONFIG_REFERENCE =
        new LocalDocumentReference(GDPR_SPACE, "GDPRConfiguration");

    private static final LocalDocumentReference GDPR_CONFIG_CLASS =
        new LocalDocumentReference(GDPR_SPACE, "GDPRConfigurationClass");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Override
    public boolean hideUserDocuments() throws GDPRException
    {
        try {
            BaseObject configuration = getConfigurationObject();

            if (configuration != null) {
                return configuration.getIntValue(HIDE_USER_DOCUMENTS) == 1;
            } else {
                return false;
            }
        } catch (XWikiException e) {
            throw new GDPRException("Failed to get the hide users configuration.", e);
        }
    }

    @Override
    public List<DocumentReference> getAllowedGroups() throws GDPRException
    {
        try {
            BaseObject configuration = getConfigurationObject();

            String[] allowedGroups = configuration.getStringValue(ALLOWED_GROUPS).split("\\|");
            List<DocumentReference> allowedGroupReferences = new ArrayList<>();
            for (String allowedGroup : allowedGroups) {
                allowedGroupReferences.add(documentReferenceResolver.resolve(allowedGroup));
            }

            return allowedGroupReferences;
        } catch (XWikiException e) {
            throw new GDPRException("Failed to get the allowed GDPR groups.", e);
        }
    }

    private BaseObject getConfigurationObject() throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        XWikiDocument document = xwiki.getDocument(GDPR_CONFIG_REFERENCE, context);
        return document.getXObject(GDPR_CONFIG_CLASS);
    }
}
