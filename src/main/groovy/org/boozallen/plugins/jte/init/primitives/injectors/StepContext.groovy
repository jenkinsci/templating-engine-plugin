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

/**
 * Step metadata
 */
class StepContext implements Serializable{

    private static final long serialVersionUID = 1L

    /**
     * The name of the library contributing this step
     */
    String library

    /**
     * The name of this step
     * When aliased, will be set to the step alias
     */
    String name

    /**
     * Whether or not this step is the result of a StepAlias annotation
     */
    boolean isAlias

}
