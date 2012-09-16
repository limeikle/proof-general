Proof General Eclipse Plugin: ed.inf.proofgeneral
=================================================

This is the main plugin for Proof General Eclipse.

To get a working system you must also install a plugin
for each theorem prover you want to use.

For more information on this plugin, visit 

   http://proofgeneral.inf.ed.ac.uk/wiki/Main/ProofGeneralEclipse

And see the files in the docs/ directory.

This is an open source project; the source code is
available through the project website.

== Running Proof General within Eclipse (for developers) ==

If you have the source code as Eclipse packages ed.inf.proofgeneral
and ed.inf.proofgeneral.{isabelle,coq,...etc...}, then 
open the plugin product configuration by double-clicking on 
one of the test products (e.g. pg-isabelle.product). 

In the 'Testing' panel, click on 

  'Launch the product'
or
  'Launch the product in debug mode'
  
  (The debug mode will run possibly buggy/incomplete code
   and let you develop/fix code incrementally.  Neat!)

This way of starting Proof General uses only the plugins that
are necessary, rather than all of the plugins that your platform
has installed. 

=================================================

For further information please see the docs/ directory.
For information on packaging, see docs/devel-docs/howto/make-release.txt
