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
