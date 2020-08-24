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
package org.boozallen.plugins.jte.init.governance.libs

import org.boozallen.plugins.jte.util.TemplateLogger
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner
import spock.lang.Specification
import spock.lang.Unroll

class LibraryProviderSpec extends Specification{

    class LibraryProviderImpl extends LibraryProvider{
        @Override Boolean hasLibrary(FlowExecutionOwner owner, String library){ return false }
        @Override ArrayList loadLibrary(FlowExecutionOwner owner, Binding binding, String s, Map c){
            return []
        }
    }

    @Unroll
    def "when config value is '#actual' and expected type/value is #expected then result is #result"(){
        setup:
        LibraryProviderImpl libSource = new LibraryProviderImpl()

        expect:
        libSource.validateType(Mock(TemplateLogger), actual, expected) == result

        where:
        actual      |     expected      | result
        true        |      boolean      | true
        false       |      boolean      | true
        true        |      Boolean      | true
        false       |      Boolean      | true
        "nope"      |      boolean      | false
        "hey"       |      String       | true
        "${4}"      |      String       | true
        4           |      String       | false
        4           |      Integer      | true
        4           |      int          | true
        4.2         |      Integer      | false
        4.2         |      int          | false
        1           |      Double       | false
        1.0         |      Double       | true
        1           |      Number       | true
        1.0         |      Number       | true
        "hey"       |     ~/.*he.*/     | true
        "heyyy"     |     ~/^hey.*/     | true
        "hi"        |     ~/^hey.*/     | false
        "hi"        |    ["hi", "hey"]  | true
        "opt3"      |  ["opt1", "opt2"] | false
    }

}
