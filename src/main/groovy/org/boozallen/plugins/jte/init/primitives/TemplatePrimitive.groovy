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
package org.boozallen.plugins.jte.init.primitives

import com.cloudbees.groovy.cps.NonCPS

/**
 * Objects whose class extends TemplatePrimitive will be protected in the {@link TemplateBinding} from
 * being inadvertently overridden
 */
abstract class TemplatePrimitive implements Serializable{

    private static final long serialVersionUID = 1L

    Class<? extends TemplatePrimitiveInjector> injector
    String name

    @NonCPS
    Object getValue(){ return this }

    /**
     * Invoked if an object with this class were to be overridden in the {@link TemplateBinding} during initialization
     */
    abstract void throwPreLockException(String preface)

    /**
     * Invoked if an object with this class were to be overridden in the {@link TemplateBinding} after initialization
     */
    abstract void throwPostLockException(String preface)

    /**
     * Returns the injector that creates the primitive
     * <p>
     * implementing classes must mark this method @NonCPS lest a CpsCallableInvocation be thrown
     * during initialization
     * @return
     */
    abstract Class<? extends TemplatePrimitiveInjector> getInjector()

    /**
     * Returns the variable name for this primitive in the binding
     * <p>
     * implementing classes must mark this method @NonCPS lest a CpsCallableInvocation be thrown
     * during initialization
     * @return
     */
    abstract String getName()

    /**
     * Returns the user-facing description of what this primitive is
     * <p>
     *     examples:
     *     - Library Step 'build' from the 'maven' library
     *     - Stage 'continuous_integration'
     * @return the user-facing description of the primitive
     */
    abstract String getDescription()

}
