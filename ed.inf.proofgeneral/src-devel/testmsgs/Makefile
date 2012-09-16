XMLLINT=xmllint
RNGSCHEMA=../../../dtd/pgip.rng

# validate an XML file usin xmllint:  make lint.test" validates test.xml
check:
	for f in *.xml; do $(XMLLINT) --noout --relaxng $(RNGSCHEMA) $$f; done
