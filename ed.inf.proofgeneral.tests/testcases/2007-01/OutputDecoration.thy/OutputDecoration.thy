theory OutputDecoration imports Main
begin

ML {* set show_types *}
lemma od1: "(∀ x . P x) --> (∃ x . P x)"

apply auto
done

end
