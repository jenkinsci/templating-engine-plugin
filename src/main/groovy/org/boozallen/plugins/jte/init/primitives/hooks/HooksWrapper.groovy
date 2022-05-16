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
package org.boozallen.plugins.jte.init.primitives.hooks

import java.lang.annotation.Annotation

/**
 * This class is used from {@see PipelineTemplateCompiler} to
 * invoke Lifecycle hooks in template
 */
class HooksWrapper implements Serializable{

    private static final long serialVersionUID = 1L
    static void invoke(Class<? extends Annotation> annotation, Boolean exceptionThrown = false){
        HookContext context = new HookContext(exceptionThrown: exceptionThrown)
        HookInjector.getHooksClass().invoke(annotation, context)
    }

}
