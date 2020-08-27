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
package org.boozallen.plugins.jte.init.governance

import hudson.Extension
import jenkins.model.GlobalConfiguration

/**
 * Stores a {@link GovernanceTier} for the entire Jenkins instance
 */
@Extension
class TemplateGlobalConfig extends GlobalConfiguration {

    private GovernanceTier tier

    static TemplateGlobalConfig get() {
        return GlobalConfiguration.all().get(TemplateGlobalConfig)
    }

    TemplateGlobalConfig() {
        load()
    }

    void setTier(GovernanceTier tier){
        this.tier = tier
        save()
    }

    GovernanceTier getTier(){
        return tier
    }

}
