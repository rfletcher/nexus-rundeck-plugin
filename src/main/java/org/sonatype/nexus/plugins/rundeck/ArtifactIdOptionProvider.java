/*
 * Copyright 2011 Vincent Behar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sonatype.nexus.plugins.rundeck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.SearchType;
import org.codehaus.plexus.component.annotations.Component;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.index.Searcher;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.plexus.rest.resource.PlexusResource;

/**
 * Option provider for RunDeck - see http://rundeck.org/docs/RunDeck-Guide.html#option-model-provider<br>
 * Provider for artifactId of artifacts presents in the Nexus repository, and matching the request.
 * 
 * @author Vincent Behar
 */
@Component(role = PlexusResource.class, hint = "ArtifactIdOptionProvider")
public class ArtifactIdOptionProvider extends AbstractOptionProvider {

    @Inject
    @Named("mavenCoordinates")
    private Searcher searcher;

    @Override
    public String getResourceUri() {
        return "/rundeck/options/artifactId";
    }

    @Override
    public Object get(Context context, Request request, Response response, Variant variant) throws ResourceException {
        // retrieve main parameters (r, g, a, v, p, c)
        Form form = request.getResourceRef().getQueryAsForm();
        String repositoryId = form.getFirstValue("r", null);
        Map<String, String> terms = new HashMap<String, String>(form.getValuesMap());

        // search
        IteratorSearchResponse searchResponse;
        try {
            searchResponse = searcher.flatIteratorSearch(terms,
                                                         repositoryId,
                                                         null,
                                                         null,
                                                         null,
                                                         false,
                                                         SearchType.EXACT,
                                                         null);
        } catch (NoSuchRepositoryException e) {
            throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "No repository at " + repositoryId, e);
        }

        // retrieve unique artifactIds and sort them
        List<String> artifactIds = new ArrayList<String>();
        for (ArtifactInfo aInfo : searchResponse.getResults()) {
            String artifactId = aInfo.artifactId;
            if (!artifactIds.contains(artifactId)) {
                artifactIds.add(artifactId);
            }
        }
        Collections.sort(artifactIds);

        // optionally include a blank value
        if (Boolean.parseBoolean(form.getFirstValue("optional", null))) {
            artifactIds.add(0, "");
        }

        return artifactIds;
    }

}
