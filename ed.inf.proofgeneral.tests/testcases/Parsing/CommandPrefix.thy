
theory CommandPrefix imports Main
begin

(* This tests the code for attempted incremental parses
   of commands: previously it would send 3, 6, 9 etc
   lines until a whole command was found.  This strategy
   is flawed for languages that have the property that
   a prefix of a command may itself be a command: we can
   end up parsing a partial command.
*)

(* Since we first try to parse the whole document now,
   to test this case and trigger "gathering" strategy,
   there must be a parse error/edit at the next line which gets
   removed.  
   TEST:  1. Edit start of datatype (e.g. change name).
             This changes parse offset to be start of datatype.
          2. Click on next command: this causes some parsing
             on-demand using the linea-gathering strategy
          3. Check that the *whole* datatype is processed,
             not just part of it. 
   Run this test with the preference for "gathering parser" enabled.
   See findObject() in Parer.java
*)

datatype PrefixExamples =
   Case1 
 | Case2
 | Case3
 | Case4
 | Case5
 | Case6
 | Case7
 | Case8
 | Case9
 | Case10
 | Case11 
 | Case12
 | Case13
 | Case14
 | Case15
 | Case16
 | Case17
 | Case18
 | Case19
 | Case21 
 | Case22
 | Case23
 | Case24
 | Case25
 | Case26
 | Case27
 | Case28
 | Case29
 | Case30
 | Case31 
 | Case32
 | Case33
 | Case34
 | Case35
 | Case36
 | Case37
 | Case38
 | Case39
 | Case40
 | Case41 
 | Case42
 | Case43
 | Case44
 | Case45
 | Case46
 | Case47
 | Case48
 | Case49
 | Case51 
 | Case52
 | Case53
 | Case54
 | Case55
 | Case56
 | Case57
 | Case58
 | Case59
 | Case60
  
lemma
  
end
