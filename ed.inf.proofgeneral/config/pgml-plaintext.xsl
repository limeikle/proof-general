<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<!--
	This file is used to convert PGIP output in PGML into plain unstructured text.
	The output text is under a single node <plaintext>.
-->

<xsl:template match="/">
     <plaintext>
          <xsl:apply-templates />
     </plaintext>
</xsl:template>

<xsl:template match="br">
	<!-- <xsl:value-of select="\\n"/> -->	
</xsl:template>

<xsl:template match="normalresponse">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="errorresponse">
    <xsl:apply-templates/>
</xsl:template>


<!--
      Proof state display
-->
<xsl:preserve-space elements="statedisplay pgmltext">
    <xsl:apply-templates/>
</xsl:preserve-space>

<xsl:template match="pgmltext">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="statedisplay">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="sym">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="atom">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="opengoal">
    <xsl:apply-templates/>
</xsl:template>

</xsl:stylesheet>
