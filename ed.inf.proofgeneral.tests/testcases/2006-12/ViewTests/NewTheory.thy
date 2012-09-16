(* Some tests of view initialisation.

Some expected behaviours:

 - If we process this file from startup with a view other than
 "Proof Output" active, switching to the Proof Output (formerly
 called "Latest Output") should display the *latest* output,
 not the first Welcome message message
  
  
*)

theory NewTheory imports Main
begin

lemma foo:"P-->P"
(* Process to here *)
done (* This next line gives error.  HOWEVER, after processing 
        this, we do not want the focus to switch to the 
        prover output window (Which it currently 21.12.06 does after 
        "Could not send command:" is printed).  
        This annoyance means that user has to click back onto editor
        tab to activate it again, and breaks keyboard-only working.
        RESOLVED as of 29.12.06. *)

end
