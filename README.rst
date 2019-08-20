Jenkins Templating Engine
=========================

|Jenkins Plugin| |GitHub Release| |Jenkins Plugin Installs| |Gitter| 

.. |Jenkins Plugin| image:: https://img.shields.io/jenkins/plugin/v/templating-engine.svg
   :target: https://plugins.jenkins.io/templating-engine

.. |GitHub Release| image:: https://img.shields.io/github/v/release/jenkinsci/templating-engine-plugin.svg?label=release
   :target: https://github.com/jenkinsci/templating-engine-plugin/releases/latest
       
.. |Jenkins Plugin Installs| image:: https://img.shields.io/jenkins/plugin/i/templating-engine.svg?color=blue
   :target: https://plugins.jenkins.io/templating-engine

.. |Gitter| image:: https://badges.gitter.im/jenkinsci/templating-engine-plugin.svg
   :target: https://gitter.im/jenkinsci/templating-engine-plugin


`Full Documentation Can Be Found Here <https://jenkinsci.github.io/templating-engine-plugin>`_

`Jenkins.io: Introducing the Jenkins Templating Engine! <https://jenkins.io/blog/2019/05/09/templating-engine/>`_


The Jenkins Templating Engine (JTE) is a plugin developed by Booz Allen Hamilton enabling
pipeline templating and governance. JTE  allows you to consolidate 
pipelines into shareable workflows that define the business logic of 
your software delivery processes while allowing for optimal pipeline code reuse by
pulling out tool specific implementations into library modules. 

How Does It Work? 
*****************

JTE allows you to separate the business logic of your pipeline (what should happen, and when) 
from the technical implementation by creating pipeline templates and separating the implementation
of the pipeline actions defined in the template out into pipeline libraries.  

The idea is that regardless of which specific tools are being used there are common steps that 
often take place, such as unit testing, static code analysis, packaging an artifact, and deploying
that artifact to an application environment. 

An example workflow might be: 

.. image:: docs/images/home/sample_template.png
   :scale: 50%
   :align: center

This workflow could be implemented by creating a template, as follows: 

.. code:: 

    unit_test()
    static_code_analysis()
    build()
    deploy_to dev 

This template could be reused by multiple teams using different tools by creating :ref:`libraries<Library Development>` which
implement the pipeline **steps** ``unit_test``, ``static_code_analysis``, ``build``, and ``deploy_to``.  Once these libraries
are created, a configuration file is created to specify which libraries to load. 

Why Templating?
***************

Pipelines are typically defined on a per application basis via a ``Jenkinsfile`` in the 
source code repository.  Often, common code can be pulled out into Shared Libraries to reduce 
code duplication but a few problems remain.  Organization's application portfolios often have
a diverse technical stack, each requiring their own pipeline-as-code integrations.  It becomes
increasingly complex to manage these different implementations across an organization while 
standardizing on the software delivery processes required. Additionally, for organizations with
strict governance requirements around types of security testing and approvals, having a Jenkinsfile
within the source code repository can be problematic as it potentially allows developers to bypass
these mandatory steps. 

.. image:: docs/images/home/value.png
   :scale: 50%
   :align: center

Organizational Governance
^^^^^^^^^^^^^^^^^^^^^^^^^
While it is possible to just use JTE to develop cleaner pipelines by writing a templated Jenkinsfile
inside your application's repo alongside the configuration file, the real power of JTE comes from creating
shareable workflows and using them across teams.  

To enable shareable workflows and to consolidate common configurations between teams, the templates and a parent
configuration file can be defined in a centralized repository. This centralized repository would act as a 
:ref:`governance tier<Governance Tier>` where configurations are specified that applications must inherit.

With governance tiers, organizations can create enterprise scale pipelines that codify the required software
delivery processes and common pipeline configurations that applications will inherit for their pipeline.  

Optimize Code Reuse
^^^^^^^^^^^^^^^^^^^
By separating the implementation of pipeline tool integrations into common libraries, you're able to
share pipeline code effectively across multiple teams.  There is no reason each team writing a pipeline
should spend time developing different versions of the same pipeline code. 

At Booz Allen, we have written pipeline libraries focused on creating a DevSecOps pipeline and automated 
the deployment of various testing tools into a Kubernetes environment.  We call it the Solutions Delivery 
Platform and it's open sourced through the Booz Allen Public License.  Through SDP, we have decreased the
time it takes to develop a mature DevSecOps pipeline from months to days and we're continuously improving
the libraries we've built while incorporating new tool integrations as they are required across our client
engagements. 

Simplify Pipeline Maintainability
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
Let's face it: typically pipelines are developed by a core team of DevOps engineers that are working with
multiple development teams simultaneously.  Much of the code is the same and can be pulled into a traditional
Jenkins Shared Library, but having a Jenkinsfile in every repository is a **pain**. 

It makes updating the flow of a pipeline challenging as the Jenkinsfile must be migrated for every repository.
It can be difficult to add new tool integrations and makes it easier to inadvertently break something because 
each pipeline may be a little different. 

With JTE, everything becomes much simpler.  Making a change to the flow of the pipeline is just a matter of 
updating a pipeline template defined in one location.  Individual tool integrations are organized into 
libraries that contribute isolated steps. 
