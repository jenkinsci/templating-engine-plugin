package org.boozallen.plugins.jte.init.primitives

import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.cps.GlobalVariable

import javax.annotation.Nonnull

abstract class TemplatePrimitiveGV extends GlobalVariable{

    @Override
    Object getValue(@Nonnull CpsScript script) throws Exception {
        return this
    }

}
