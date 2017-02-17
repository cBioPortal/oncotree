package org.mskcc.oncotree.model;

/**
 * Created by Hongxin on 5/23/16.
 */
public class Version {
    private String version;
    private String commitId;

    public Version(String version, String commitId) {
        this.version = version;
        this.commitId = commitId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Version version1 = (Version) o;

        if (version != null ? !version.equals(version1.version) : version1.version != null) return false;
        return commitId != null ? commitId.equals(version1.commitId) : version1.commitId == null;

    }

    @Override
    public int hashCode() {
        int result = version != null ? version.hashCode() : 0;
        result = 31 * result + (commitId != null ? commitId.hashCode() : 0);
        return result;
    }
}
