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

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

/**
 *
 * @version $Id$
 */
public class GDPRRequest extends AbstractRequest
{   
    private static final String PROPERTY_WIKI_REFERENCE = "wiki.reference";
    
    private static final String PROPERTY_USER_REFERENCE = "user.reference";
    
    public WikiReference getWikiReference()
    {
        return getProperty(PROPERTY_WIKI_REFERENCE);
    }

    public void setWikiReference(WikiReference wikiReference)
    {
        setProperty(PROPERTY_WIKI_REFERENCE, wikiReference);
    }

    public DocumentReference getUserReference()
    {
        return getProperty(PROPERTY_USER_REFERENCE);
    }

    public void setUserReference(DocumentReference userReference)
    {
        setProperty(PROPERTY_USER_REFERENCE, userReference);
    }

}
