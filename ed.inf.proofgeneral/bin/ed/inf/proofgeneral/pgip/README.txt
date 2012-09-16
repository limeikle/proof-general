This is work in progress, to make a clean abstraction of PGIP in Java.

There should be no Eclipse dependencies here, ideally, so we can use
this package elsewhere (eventually its own jar).

See pgip.rnc for schema definition and pgip-isabelle-input,output for representation
used in Isabelle.

To begin with, we're only interested in messages sent to/from prover.  Later we should
add representation for the display commands used by the broker, and even the broker
commands themselves to support a Java broker implementation.

Code in PGIPSyntax should be moved here, gradually.

[da, Jan 07]
 
 ----------
 
 The latest version of pgip.rnc can be fetched/refreshed by:

  cd ed.inf.proofgeneral/src/ed/inf/proofgeneral
  cvs -d :pserver:anon@cvs.inf.ed.ac.uk:/disk/cvs/proofgen export -kv -r HEAD -d pgip Kit/dtd/pgip.rnc

 