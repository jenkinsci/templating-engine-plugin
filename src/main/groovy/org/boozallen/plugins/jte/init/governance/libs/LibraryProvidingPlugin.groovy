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

import hudson.PluginWrapper
import hudson.model.AbstractDescribableImpl
import hudson.model.Descriptor
import jenkins.model.Jenkins

/**
 * extended by third party plugins that package a set of libraries
 */
abstract class LibraryProvidingPlugin extends AbstractDescribableImpl<LibraryProvidingPlugin>{

    static class LibraryProvidingPluginDescriptor extends Descriptor<LibraryProvidingPlugin> {
        String getDisplayName(){
            PluginWrapper plugin = Jenkins.get().getPluginManager().whichPlugin(this.getClass())
            return plugin.getDisplayName()
        }
    }

}
