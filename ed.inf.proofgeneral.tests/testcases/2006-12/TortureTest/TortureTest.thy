(* This example is intended to test each kind of document
   element produced by PGIP markup in Isabelle/Isar.
   This first element is an ordinary <comment>.
   It will be followed by and invisible <whitespace> element
   which contains two carriage returns before the next element.

   The last character position of this comment is 470
   - theory line starts at 473
   - text at 528
   - section at 563
   - consts at 623
   - lemma at 668
   - second lemma at 808
*)

header {* This is a text of many different syntax elements in Isar *}

theory TortureTest imports Main begin  (*opentheory*)

section {* This is a <doccomment> that should be named. *}

text {* This is a plain <doccomment> *}

subsection {* Another <doccomment> named. Next some theory steps *}  

consts P :: "int => bool" (* theory item *)

lemma "∀ x .P(x) --> (∃ x .P(x))" (* open goal, followed by openblock *)
  apply safe (* proofstep *)
  apply auto (* proofstep *)
  done (* closeblock, closegoal *)
  
lemma foo: "Q-->Q" (* opengoal, openblock *)
oops (* closeblock, giveupgoal *)

lemma bar: "R-->R" (* opengoal, openblock *)
sorry (* closeblock, closegoal *)

print_commands    (* spuriouscmd: it displays some output, but is
                     ignored for undo calculations. *)

undos_proof 1 (* badcmd, potentially loses sync (here gives error) *)
  
end (* closetheory *)


(* Here is the datatype from Isabelle:

  * Generic markup on sequential, non-overlapping pieces of proof text *
  datatype pgipdoc = 
    Openblock     of { metavarid: string option, name: string option, objtype: string option }
  | Closeblock    of { }
  | Opentheory    of { thyname: string, parentnames: string list , text: string}
  | Theoryitem    of { name: string option, objtype: string option, text: string }
  | Closetheory   of { text: string }
  | Opengoal	  of { thmname: string option, text: string }
  | Proofstep     of { text: string }
  | Closegoal     of { text: string } 
  | Giveupgoal    of { text: string }
  | Postponegoal  of { text: string }
  | Comment       of { text: string }
  | Doccomment    of { text: string }
  | Whitespace    of { text: string }
  | Spuriouscmd   of { text: string }
  | Badcmd        of { text: string }
  | Unparseable	  of { text: string } 
  | Metainfo 	  of { name: string option, text: string }
  * Last three for PGIP literate markup only: *
  | Litcomment	  of { format: string option, content: XML.content }
  | Showcode	  of { show: bool }				 
  | Setformat	  of { format: string }
 *)