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

package org.boozallen.plugins.jte.config

import hudson.Extension
import jenkins.model.GlobalConfiguration

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class TemplateGlobalConfig extends GlobalConfiguration {

    private GovernanceTier tier
    private Boolean allowLibraryOverride = true;

    public static TemplateGlobalConfig get() {
        return GlobalConfiguration.all().get(TemplateGlobalConfig)
    }

    public TemplateGlobalConfig() {
        load()
    }

    public void setTier(GovernanceTier tier){
        this.tier = tier
        save()
    }

    public GovernanceTier getTier(){
        return tier
    }

    Boolean getAllowLibraryOverride() {
        return allowLibraryOverride
    }

    void setAllowLibraryOverride(Boolean allowLibraryOverride) {
        this.allowLibraryOverride = allowLibraryOverride
    }
}