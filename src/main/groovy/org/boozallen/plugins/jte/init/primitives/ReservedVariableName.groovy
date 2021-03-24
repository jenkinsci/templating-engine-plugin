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

import hudson.ExtensionList
import hudson.ExtensionPoint
import jenkins.model.Jenkins

/**
 * Extension point to register particular variable names for protection
 * to prevent TemplatePrimitive's from overriding them
 */
@SuppressWarnings(['EmptyMethodInAbstractClass'])
abstract class ReservedVariableName implements ExtensionPoint{

    static ReservedVariableName byName(String name){
        return all().find{ reservedVar -> reservedVar.getName() == name }
    }

    static ExtensionList<ReservedVariableName> all() {
        return Jenkins.get().getExtensionList(ReservedVariableName)
    }

    abstract String getName()
    abstract String getDescription()

}
