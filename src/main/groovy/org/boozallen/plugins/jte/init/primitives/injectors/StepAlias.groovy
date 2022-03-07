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

import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import java.lang.annotation.Retention

/**
 * For use in steps to register step aliases
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface StepAlias{

    /**
     * List of static step aliases
     * @return an array of step aliases
     */
    String[] value() default []

    /**
     * A closure whose result is a String or list of strings that are aliases for the step
     * @return A closure to execute to determine step aliases, if any.
     */
    Class dynamic() default {}

    /**
     * Whether or not to create a step for the original step name
     * @return
     */
    boolean keepOriginal() default false

}
