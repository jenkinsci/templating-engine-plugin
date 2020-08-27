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
package org.boozallen.plugins.jte.init.governance.libs

import hudson.Extension
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import hudson.model.DescriptorVisibilityFilter
import jenkins.model.Jenkins
import org.kohsuke.stapler.DataBoundConstructor

/**
 * generic holder for a {@link LibraryProvider} used to fetch libraries from a source
 */
class LibrarySource extends AbstractDescribableImpl<LibrarySource> {

    private final LibraryProvider libraryProvider

    @DataBoundConstructor
    LibrarySource(LibraryProvider libraryProvider) {
        this.libraryProvider = libraryProvider
    }

    LibraryProvider getLibraryProvider(){ return libraryProvider }

    @Extension
    static class DescriptorImpl extends Descriptor<LibrarySource> {
        static List<LibraryProvider.LibraryProviderDescriptor> getLibraryProviders(){
            return DescriptorVisibilityFilter.apply(null, Jenkins.get().getExtensionList(LibraryProvider.LibraryProviderDescriptor))
        }
    }

}
