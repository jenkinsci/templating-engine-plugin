.. _Application Environments: 

------------------------
Application Environments
------------------------

The Application Environment primitive exists to simplify referencing deployment
targets from pipeline templates.  

********************************
Configuration File Specification
********************************

The ``application_environments`` key is used to begin declaring application environments. 

Within the application environments block, environments can be defined through subkeys.
These subkeys will be used to reference the environment from your pipeline template. 

By default, application environments accept an optional ``short_name`` and ``long_name``
configuration.  These values will be set to the value of the environment name if not overridden. 

An example specification defining a ``dev`` and ``prod`` environment would be: 

.. code:: 

    application_environments{
        dev{
            long_name = "Development" 
        }
        prod{
            long_name = "Production" 
        }
    }

Then within a pipeline template, assuming there is a pipeline step called ``deploy_to`` that 
accepts an application environment as an argument, you could reference these objects via

.. code:: 

    build()
    deploy_to dev
    test()
    deploy_to prod 

*******************
Determining Context
*******************

A created ApplicationEnvironment can determine the previous and next ApplicationEnvironment that's 
been defined through the ``previous`` and ``next`` properties.  

The first environment's ``previous`` property and the last environment's ``next`` property will be ``null``. 

.. note:: 

    These properties are automatically configured.  If you try to set the ``previous`` and ``next`` properties
    in the environment's definition an exception will be thrown. 

For example, defining: 

.. code:: 

    application_environments{
        dev{
            long_name = "Development"
        }
        test{
            long_name = "Test" 
        }
        prod{
            long_name = "Production" 
        }
    }

This will create ``dev``, ``test``, and ``prod`` objects to be leveraged in your libraries and templates. 

Then, to validate their context you could create a template such as: 

.. code:: 

    // validate dev environment's context 
    assert dev.previous == null
    assert dev.next == test 

    // validate test environment's context
    assert test.previous == dev 
    assert test.next == prod 

    // validate prod environment's context 
    assert prod.previous == test 
    assert prod.next == null 
    

*************************
Additional Configurations 
*************************

The Application Environment primitive can also accept an arbitrary number of additional
configuration fields.

This often comes in handy when libraries provide steps that accept an application environment
as an input parameter. Configurations for the library across all environments would be defined 
in the library spec, while environment specific configurations would be defined on the environment. 

Suppose there was an ec2 library that accepts a ``ssh_credential_id`` configuration that specifies 
a Jenkins credential ID containing the ssh key to access both instances and an ``ip`` configuration 
supplying the IP for the environment to scp an artifact to. 

The configuration file for such a situation could be: 

.. code:: 

    application_environments{
        dev{
            long_name = "Development" 
            ec2{
                ip = "1.2.3.4" 
            }
        }
        prod{
            ec2{
                ip = "1.2.3.5" 
            }
        }
    }

    libraries{
        ec2{
            ssh_credential_id = "ssh_credential" 
        }
    }

Refer to :ref:`Library Development<Getting Started>` for more information on how to build 
libraries that can leverage this pattern. 