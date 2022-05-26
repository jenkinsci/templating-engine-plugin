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

/**
 * encapsulates the runtime context to inform lifecycle hook annotated library step methods
 */
class HookContext implements Serializable{

    private static final long serialVersionUID = 1L

    /**
     * the library contributing the step that triggered the lifecycle hook
     * <p>
     * {@code null} prior to and post pipeline template execution
     */
    String library

    /**
     * the name of the step that triggered the lifecycle hook
     * <p>
     * {@code null} prior to and post pipeline template execution
     */
    String step

    /**
     * the name of the method within the step that was invoked.
     * helpful for triggering hooks after multi-method steps.
     * <p>
     * {@code null} prior to and post pipeline template execution
     */
    String methodName

    /**
     * indicates whether an uncaught exception has been thrown.
     */
    Boolean exceptionThrown = false

}
