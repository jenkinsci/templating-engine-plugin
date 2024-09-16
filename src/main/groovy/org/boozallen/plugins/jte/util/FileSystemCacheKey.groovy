/*
    Copyright 2018 Booz Allen Hamilton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.boozallen.plugins.jte.util

import hudson.scm.SCM
import jenkins.scm.api.SCMHead
import jenkins.scm.api.SCMRevision
import jenkins.scm.api.SCMSource
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner

/**
 * The FileSystemCacheKey class is a utility for creating cache keys based on various
 * SCM (Source Code Management) related objects within the Jenkins Pipeline ecosystem.
 * This key is intended to uniquely identify a cache entry based on SCM configurations
 * and the owner of the pipeline execution.
 *
 * <p>It overrides the {@link #equals(Object)} and {@link #hashCode()} methods to ensure
 * that cache keys are properly compared and can be used effectively in hash-based collections.</p>
 *
 * <p>This class is used in caching scenarios to avoid re-fetching or recalculating information
 * that has already been retrieved from the SCM system during a Jenkins pipeline execution.</p>
 *
 * <h2>Fields:</h2>
 * <ul>
 *   <li>{@code owner} - The {@link FlowExecutionOwner} that represents the owner of the flow execution.</li>
 *   <li>{@code scm} - The {@link SCM} instance that represents the source code management system in use.</li>
 *   <li>{@code scmSource} - The {@link SCMSource} which defines the SCM source for the project.</li>
 *   <li>{@code scmHead} - The {@link SCMHead} that identifies the branch, tag, or other head in the SCM repository.</li>
 *   <li>{@code scmRevision} - The {@link SCMRevision} that represents the revision of the SCM head.</li>
 * </ul>
 *
 * <h2>Methods:</h2>
 * <ul>
 *   <li>{@link #equals(Object)} - Compares this cache key with another object for equality, based on the owner, SCM, and related fields.</li>
 *   <li>{@link #hashCode()} - Generates a hash code based on the owner, SCM, and related fields for use in hash-based collections.</li>
 * </ul>
 *
 * <p><b>Copyright:</b> 2018 Booz Allen Hamilton</p>
 *
 * <p><b>License:</b> This class is licensed under the Apache License, Version 2.0. See the LICENSE file or
 * visit <a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a> for details.</p>
 *
 * @see FlowExecutionOwner
 * @see SCM
 * @see SCMSource
 * @see SCMHead
 * @see SCMRevision
 */
class FileSystemCacheKey {

    private static final int MULTIPLIER = 31

    FlowExecutionOwner owner
    SCM scm
    SCMSource scmSource
    SCMHead scmHead
    SCMRevision scmRevision

    /**
     * Compares this FileSystemCacheKey instance with another object to determine if they are equal.
     * The comparison is based on the equality of the following fields: {@code owner}, {@code scm},
     * {@code scmSource}, {@code scmHead}, and {@code scmRevision}.
     *
     * <p>If the provided object is not an instance of {@code FileSystemCacheKey}, this method returns {@code false}.</p>
     *
     * @param o the object to be compared with this instance.
     * @return {@code true} if the provided object is equal to this instance, {@code false} otherwise.
     */
    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (!(o instanceof FileSystemCacheKey)) {
            return false
        }

        FileSystemCacheKey that = (FileSystemCacheKey) o

        boolean result = true

        if (owner != that.owner) {
            result = false
        }
        if (scm != that.scm) {
            result = false
        }
        if (scmHead != that.scmHead) {
            result = false
        }
        if (scmRevision != that.scmRevision) {
            result = false
        }
        if (scmSource != that.scmSource)  {
            result = false
        }

        return result
    }
/**
     * Generates a hash code for this FileSystemCacheKey instance.
     * The hash code is computed based on the {@code owner}, {@code scm}, {@code scmSource},
     * {@code scmHead}, and {@code scmRevision} fields.
     *
     * <p>This method is used to efficiently store and retrieve instances of {@code FileSystemCacheKey}
     * in hash-based collections such as {@link java.util.HashMap} or {@link java.util.HashSet}.</p>
     *
     * @return the hash code value for this instance.
     */
    @Override
    int hashCode() {
        int result
        result = (owner != null ? owner.hashCode() : 0)
        result = MULTIPLIER * result + (scm != null ? scm.hashCode() : 0)
        result = MULTIPLIER * result + (scmSource != null ? scmSource.hashCode() : 0)
        result = MULTIPLIER * result + (scmHead != null ? scmHead.hashCode() : 0)
        result = MULTIPLIER * result + (scmRevision != null ? scmRevision.hashCode() : 0)
        return result
    }

}
