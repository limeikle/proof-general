(**** <normalresponse> messages ***)
theory NormalResponse imports Main begin

(* ordinary messages appear in the Prover Output view. *)

ML {* Output.writeln "This is an ordinary message"; *}

ML {* Output.writeln "This is an ordinary message, but...\nwith two lines"; *}

ML {* Output.writeln "Here are three ordinary messages..."; 
      Output.writeln "that are produced by the same command...";
      Output.writeln "so they should be displayed together."; *}

ML {* Output.priority "This is an important message that should catch the user's attention\nIt has two lines"; *}

(* proof state messages appear in the Prover Output view OR the Current State view if active. *)

lemma foo: "P-->P" by auto

(* tracing messages appear in the tracing console *)

ML {* Output.tracing "This is a one-line tracing message"; *}

ML {* Output.tracing "This is a two-line tracing message\nThis is the second line of it."; *}

end
