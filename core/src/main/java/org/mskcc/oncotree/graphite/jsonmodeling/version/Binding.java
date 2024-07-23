/*
 * Copyright (c) 2024 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 * documentation provided hereunder is on an "as is" basis, and
 * Memorial Sloan-Kettering Cancer Center
 * has no obligations to provide maintenance, support,
 * updates, enhancements or modifications.  In no event shall
 * Memorial Sloan-Kettering Cancer Center
 * be liable to any party for direct, indirect, special,
 * incidental or consequential damages, including lost profits, arising
 * out of the use of this software and its documentation, even if
 * Memorial Sloan-Kettering Cancer Center
 * has been advised of the possibility of such damage.
*/

package org.mskcc.oncotree.graphite.jsonmodeling.version;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Binding {

    @JsonProperty("api_identifier")
    private Field apiIdentifier;

    @JsonProperty("graph_uri")
    private Field graphUri;

    @JsonProperty("description")
    private Field description;

    @JsonProperty("release_date")
    private Field releaseDate;

    @JsonProperty("visible")
    private Field visible;

    public Field getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(Field apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public Field getGraphUri() {
        return graphUri;
    }

    public void setGraphUri(Field graphUri) {
        this.graphUri = graphUri;
    }

    public Field getDescription() {
        return description;
    }

    public void setDescription(Field description) {
        this.description = description;
    }

    public Field getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Field releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Field getVisible() {
        return visible;
    }

    public void setVisible(Field visible) {
        this.visible = visible;
    }

}
