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
package org.boozallen.plugins.jte.init.primitives.injectors

import hudson.Extension
import org.boozallen.plugins.jte.init.governance.config.dsl.PipelineConfigurationObject
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveInjector
import org.boozallen.plugins.jte.init.primitives.TemplatePrimitiveNamespace
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution

/**
 * creates Keywords
 */
@Extension class KeywordInjector extends TemplatePrimitiveInjector {

    private static final String KEY = "keywords"

    @Override
    TemplatePrimitiveNamespace injectPrimitives(CpsFlowExecution exec, PipelineConfigurationObject config){
        TemplatePrimitiveNamespace keywords = new TemplatePrimitiveNamespace(name: KEY)

        // populate namespace with keywords from pipeline config
        LinkedHashMap aggregatedConfig = config.getConfig()
        aggregatedConfig[KEY].each{ key, value ->
            Keyword keyword = new Keyword(name: key, value: (value instanceof GString ? value.toString() : value))
            keyword.setParent(keywords)
            keywords.add(keyword)
        }

        return keywords.getPrimitives() ? keywords : null
    }

}
