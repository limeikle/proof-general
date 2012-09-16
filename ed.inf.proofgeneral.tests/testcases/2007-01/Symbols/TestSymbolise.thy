(*
  da: 14.1.07: At present Unicode symbols are stored on disk.
  This is *bad*.  These characters are not understood by
  Isabelle directly so it means that files can only be
  processed in Eclipse!
  
  Files should be stored in ASCII.
  Test: open this file using Open With \<rightarrow> Text Editor.  
  Shouldn't see any symbols. 

  da: SOLVED 15.1.07.  Previous code to desymbolise on save was
  being subverted. Now fixed.
*)

theory TestSymbolise imports Main
begin

term "\<forall> x. P(x)"  (* should be on disk as \ <forall> *)
term "\<exists> x. P(x)"  (* should be on disk as \ <exists> *)
term "P ==> P"    (* should be on disk as \ <Longrightarrow> *) 
term "P \<inter> Q"      (* should be on disk as \ <inter> *)
end

