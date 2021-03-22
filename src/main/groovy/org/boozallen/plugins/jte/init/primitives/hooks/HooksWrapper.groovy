package org.boozallen.plugins.jte.init.primitives.hooks

import java.lang.annotation.Annotation

class HooksWrapper implements Serializable{
    static invoke(Class<? extends Annotation> annotation){
        HookInjector.getHooksClass().invoke(annotation)
    }
}
