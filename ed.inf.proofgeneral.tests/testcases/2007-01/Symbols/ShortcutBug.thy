theory ShortcutBug imports Main
begin
(* Parser reports "unable to reconcile" on this example. 
   The source was typed by entering the symbol \ <inter> *)

(* Problem is that ∩ is very wrongly converted to invalid shortcut /\ 
   when sending to Isabelle!!  We should *not* convert 
   text to short cuts, but rather to the symbols that they stand
   for!  Shortcuts are just cute things for input, not to be relied on!
   
   STATUS: fixed 15.1.07 by stopping SM sending short-cut chars.
   
   This caused a nasty and confusing problem parsing Primrec.thy.
   Nasty because "P /\ Q" actually does Parse in Isabelle, but as a
   "/" followed by a quoted space!!!  So we get "P / Q" instead...
*)

lemma "P ∩ Q ⊂ Q"
oops
end
