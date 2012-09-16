theory SimpleTests imports Main
begin

ML_setup {* Output.print_mode := "thm_deps" :: (!Output.print_mode) *} 

lemma foo : "P ∧ Q ==> Q" by auto

lemma bar : "A & B --> B" by (rule foo, auto)
end
