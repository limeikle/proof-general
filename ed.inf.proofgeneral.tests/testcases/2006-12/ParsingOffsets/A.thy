theory A imports Main begin

(* test parsing by moving cursor to 
   middle of this comment.  Then use goto
   button.  
   1) All of the comment should be queued
   for processing, not just up to position
   (notice UI glitch). 
   2) Undo, edit comment, repeat.
   The behaviour should be the same: we
   should *not* get a parse error
   from a partly parsed comment!
*)

end
