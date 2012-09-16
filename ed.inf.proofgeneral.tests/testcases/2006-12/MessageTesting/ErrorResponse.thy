(**** <errorresponse> messages ****)
theory ErrorResponse imports Main begin

(* infos, warnings, errors generate markers in Eclipse.  
   They are also displayed in the Prover Output view, and
   with a nice style sheet could be given error icons as well
   as highlighting. *)

ML {* Output.info "This is a one-line info message"; *}

(* multiple lines are tricky: PGML says to obey white space in parsing *)
ML {* Output.info "This is a two-line info message...\nwhose second line is here."; *}
	
ML {* Output.warning "This is a one-line warning message"; *}

ML {* Output.warning "This is a two-line warning message...\nwhose second warning line is here."; *}

(* A recoverable error: successfully processes and displays in the interface. *)
ML {* ProofGeneralPgip.nonfatal_error "This is a non-fatal error"; *}

(* Error messages with positions: 
   For URLs outside the workspace, we attach here and make a note of original URL in Problems View *)
ML {* ProofGeneralPgip.error_with_pos PgipTypes.Warning 
      (Position.line_name 10 (quote "/home/da/cvs/isabelle/HOL/HOL.thy")) 
      "This warning message was generated in ErrorResponse.thy but appears somewhere completely different!"; *}

(* For URLs in the workspace, we attach to the given location *)
ML {* ProofGeneralPgip.error_with_pos PgipTypes.Warning 
      (Position.line_name 25 
	  	(quote (Path.implode (Path.append (Path.dir (the (ProofGeneralPgip.get_currently_open_file ())))
      				             (Path.basic "NormalResponse.thy")))))
      "This warning message was generated in ErrorResponse.thy but appears somewhere completely different!"; *}

(* debug messages appear in the debug console and a log... somewhere [TODO] *)
ML {* Output.debugging := true; 
      Output.debug (fn ()=>"This is a one-line debug message"); *}
            
(* log messages are sent to a log... somewhere [TODO] *)            
ML {* ProofGeneralPgip.log_msg "This is a log message"; *}


(*** FATAL ERRORS ***)

(* The ordinary error message function Output.error_msg in Isabelle 
   produces a fatal error.  I think this should be hidden from accessible
   use; the resonable expectation is that this function produces an error
   message but not actually a fatal error by itself (that should
   be reserved for the exception-raising error function, which is
   caught by the top level execution).
   This improvement was implemented in Isabelle CVS 3.1.07 by setting a 
   hook in the Isar code to be the only place that uses error
   output by construction.  But was reverted by Makarius a couple
   of days later as he objected and says that Isabelle developers all
   understand that Output.error_msg itself entails the error.
   (Also when the same thing is done for Emacs it breaks some 
   compatibility, in e.g. ML {* 1 1 *}, for some reason not investigated)
*)

(* Currently in Isabelle CVS 8.1.07: next line loses sync.
   Output.error_msg should not be used in user code. 
   ProofGeneralPgip.nonfatal_error is non standard.
*)
ML {* Output.error_msg "This is an error message"; *}

(* NB: a true error in Isabelle generates an <errorresponse> markup
   with a fatal tag which is taken to mean that a scripting
   command fails.
*)
ML {* error "This is an error message"; *}

(* Produces an ML-level error, so two error responses *)
ML {* 1 1 *}
 
(* Another test: try entering a command via "Proof General → Enter Command"
   which produces an error as above.  This should result in error output
   but no marker.
*)
 
end

(* Note that the second 
   message (i.e. val it=():unit) wipes the first one in every case above.  
   This is part of the old message model
   (only show the most recent response), although what
   the Eclipse code does is actually keep a history of the
   responses so we can page back through them.
   A recent resolution in Isabelle (for Emacs PG) has been to rework ML_setup
   [or some similar commands] to not show the val it=():unit.
   
   The current Eclipse behaviour is not too bad, except that we're probably interested
   in seeing output from previous proof state, not just
   the last 20 lines of tracing information from a command
   that produced 400 lines of tracing.   So we should display all of the items up to the next
   <ready/> as one output together.

   3.1.07: this has now been fixed.  Output is accumulated between <ready/>
   messages.
*)

end
