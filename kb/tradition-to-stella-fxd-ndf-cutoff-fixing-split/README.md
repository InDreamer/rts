# Tradition -> Stella FXD.NDF cutoff-fixing split pack

This pack was reconstructed from photographed VS Code screenshots supplied on 2026-05-06. It captures draft canonical rules for the Tradition-to-Stella FXD.NDF fixing subtree:

`/scb:SCBML/scb:payload/scb:FPMLPayload/conf:trade/scbextn:fxSingleLeg/conf:nonDeliverableSettlement/conf:fixing`

## Scope

- source_system: Tradition
- target_system: Stella
- rule_domain: cutoff-fixing
- product_scope: FXD.NDF
- status: draft

## Contents

- `lookups/lk_fxd_ndf_cutoff_by_pair_and_locode.yaml`: shared cutoff lookup against `TraditionStella Cutoff`
- `helpers/hlp_fxd_ndf_fixing_quoted_currency_pair.yaml`: reusable quoted-currency-pair normalization helper
- `rules/`: fixing block emission, quoted currency pair, fixing date, primary rate source, secondary rate source, and fixing time rules
- `evidence/evidence-index.yaml`: pack-level evidence and diagnostic traces
- `review/review-index.yaml`: reconstruction notes, ambiguity, and review status
- `reports/`: extraction, closure, and review checklist notes

## Source Inventory

- Excel AI bundle: `Tradition_SCBML_mapping-ai-bundle.json`
- Sheets: `FXD`, `STAMP`, `TraditionStella Cutoff`
- Java: `FXDNdfFields.java`, `StaticMappingLookupCore.java`, `StaticMappingLookupUtils.java`
- XSLT: `fx-singleLeg-templates.xsl`
- Tests and samples: `fxdNdfFpML.xml`, `FXDNdfFieldsTest.java`, `StaticMappingLookupCoreTest.java`, `StaticMappingLookupUtilsTest.java`

## Modeling Choices

1. One shared cutoff lookup models cutoff description, fixing time, fixing business center, and hedge primary source/page because they use the same key pattern and reverse-pair fallback.
2. One quoted-currency-pair helper models the inversion logic because the same branch decision drives three sibling target values.
3. Block rules are used where Stella renders a stable block as a unit.
4. Evidence and review are pack-level. Main object files stay thin and query-friendly.
5. This pack remains draft quality. It is reconstructed from photos and does not imply review signoff.

## Intentional Exclusions

- FXD deliverable-leg exchange-rate rules outside the NDF fixing subtree
- `nonDeliverableSettlement/settlementCurrency`
- portfolio, party identification, trader, counterparty, and strategy slices
- FXO option-specific cutoff columns such as `PRIMARY_SOUCE_FOR_OPT` and `PRIMARY_SOUCE_PAGE_FOR_OPT`
