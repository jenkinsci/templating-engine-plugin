package org.boozallen.plugins.jte.util

import hudson.scm.SCM
import jenkins.scm.api.SCMHead
import jenkins.scm.api.SCMRevision
import jenkins.scm.api.SCMSource
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

class FileSystemCacheKey {
    FlowExecutionOwner owner
    SCM scm
    SCMSource scmSource
    SCMHead scmHead
    SCMRevision scmRevision

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof FileSystemCacheKey)) return false

        FileSystemCacheKey that = (FileSystemCacheKey) o

        if (owner != that.owner) return false
        if (scm != that.scm) return false
        if (scmHead != that.scmHead) return false
        if (scmRevision != that.scmRevision) return false
        if (scmSource != that.scmSource) return false

        return true
    }

    int hashCode() {
        int result
        result = (owner != null ? owner.hashCode() : 0)
        result = 31 * result + (scm != null ? scm.hashCode() : 0)
        result = 31 * result + (scmSource != null ? scmSource.hashCode() : 0)
        result = 31 * result + (scmHead != null ? scmHead.hashCode() : 0)
        result = 31 * result + (scmRevision != null ? scmRevision.hashCode() : 0)
        return result
    }
}
