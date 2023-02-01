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

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Request for creating a {@link GDPRJob}.
 *
 * @version $Id$
 * @since 1.0
 */
public class GDPRRequest extends AbstractRequest
{   
    private static final String PROPERTY_WIKI_REFERENCE = "wiki.reference";
    
    private static final String PROPERTY_USER_REFERENCE = "user.reference";

    /**
     * @return the reference to the wiki in which the job has been triggered
     */
    public WikiReference getWikiReference()
    {
        return getProperty(PROPERTY_WIKI_REFERENCE);
    }

    /**
     * Sets the wiki reference of the wiki on which the job has been triggered.
     *
     * @param wikiReference the wiki reference.
     */
    public void setWikiReference(WikiReference wikiReference)
    {
        setProperty(PROPERTY_WIKI_REFERENCE, wikiReference);
    }

    /**
     * @return the reference of the user that triggered the job
     */
    public DocumentReference getUserReference()
    {
        return getProperty(PROPERTY_USER_REFERENCE);
    }

    /**
     * Sets the user who triggered the job.
     *
     * @param userReference the user reference
     */
    public void setUserReference(DocumentReference userReference)
    {
        setProperty(PROPERTY_USER_REFERENCE, userReference);
    }

}
