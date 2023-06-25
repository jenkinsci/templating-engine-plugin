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
package org.boozallen.plugins.jte

import hudson.Extension
import org.jenkinsci.plugins.workflow.cps.GroovySourceFileAllowlist

/**
 * <a href="https://github.com/jenkinsci/workflow-cps-plugin/releases/tag/2692.v76b_089ccd026">
 *     workflow-cps v2692.v76b_089ccd026
 * </a> introduced allowlist functionality.
 * Some classes from this plugin are included
 * <a href="https://github.com/jenkinsci/workflow-cps-plugin/blob/master/plugin/src/main/resources/org/jenkinsci/plugins/workflow/cps/GroovySourceFileAllowlist/default-allowlist#L40-L42">
 *     by default
 * </a>.
 */
@Extension
class JteGroovySourceFileAllowList extends GroovySourceFileAllowlist {

    private static final Set<String> ALLOWED_SUFFIXES = [
            '/org/boozallen/plugins/jte/init/primitives/injectors/StageCPS.groovy',
            '/org/boozallen/plugins/jte/init/primitives/injectors/StepWrapperCPS.groovy',
    ] as Set<String>

    @Override
    boolean isAllowed(String groovySourceFileUrl) {
        for (String suffix : ALLOWED_SUFFIXES) {
            if (groovySourceFileUrl.endsWith(suffix)) {
                return true
            }
        }
        return false
    }

}
